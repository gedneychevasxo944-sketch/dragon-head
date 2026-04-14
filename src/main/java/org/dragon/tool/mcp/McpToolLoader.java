package org.dragon.tool.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dragon.tool.domain.ToolDO;
import org.dragon.tool.domain.ToolVersionDO;
import org.dragon.tool.enums.ToolStatus;
import org.dragon.tool.enums.ToolType;
import org.dragon.tool.enums.ToolVersionStatus;
import org.dragon.tool.enums.ToolVisibility;
import org.dragon.tool.runtime.ToolChangeEvent;
import org.dragon.tool.runtime.adapter.ParameterSchema;
import org.dragon.tool.store.ToolStore;
import org.dragon.tool.store.ToolVersionStore;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP 工具加载器。
 *
 * <p>负责在应用启动时（或手动刷新时）连接所有配置的 MCP Server，
 * 拉取工具列表，并将工具注册到 {@link ToolRegistry} 和 {@link ToolVersionStore}。
 *
 * <p>对应 TS 项目中 {@code src/services/mcp/client.ts} 的
 * {@code prefetchAllMcpResources()} + {@code getMcpToolsCommandsAndResources()} 流程：
 * <ul>
 *   <li>并发连接多个 MCP Server（类似 TS 的 {@code pMap} 并发批处理）</li>
 *   <li>每个 server：initialize 握手 → tools/list 拉取工具</li>
 *   <li>工具名格式：{@code mcp__{serverName}__{toolName}}（与 TS {@code buildMcpToolName} 一致）</li>
 *   <li>单个 server 失败不影响其他 server（catch 后记录错误，继续处理）</li>
 * </ul>
 *
 * <p><b>工具名命名规则</b>（与 TS 项目 {@code mcpStringUtils.ts} 一致）：
 * <pre>
 * 工具名 = "mcp__" + normalize(serverName) + "__" + normalize(toolName)
 * normalize：将非字母数字字符替换为下划线，避免 LLM 工具名冲突
 * </pre>
 *
 * <p><b>MCP 工具默认 visibility=PUBLIC</b>：
 * MCP Server 提供的工具默认对所有 Character 可见。如需限制，可在注册后修改 ToolDO.visibility=PRIVATE
 * 并通过绑定机制控制。
 */
public class McpToolLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final McpHttpClient mcpHttpClient;
    private final ToolStore toolStore;
    private final ToolVersionStore toolVersionStore;
    private final ApplicationEventPublisher eventPublisher;

    /** 并发连接 MCP Server 的线程池，默认 5 并发 */
    private final ExecutorService executor;

    public McpToolLoader(McpHttpClient mcpHttpClient,
                         ToolStore toolStore,
                         ToolVersionStore toolVersionStore,
                         ApplicationEventPublisher eventPublisher) {
        this.mcpHttpClient = mcpHttpClient;
        this.toolStore = toolStore;
        this.toolVersionStore = toolVersionStore;
        this.eventPublisher = eventPublisher;
        this.executor = Executors.newFixedThreadPool(5);
    }

    // ── 核心方法 ─────────────────────────────────────────────────

    /**
     * 并发加载所有 MCP Server 的工具，注册到 ToolRegistry 和 ToolVersionStore。
     *
     * <p>对每个 enabled=true 的 server 并发执行：initialize + tools/list → 批量 register。
     * 单个 server 失败时记录错误日志，不中断其他 server 的加载（容错设计）。
     *
     * @param serverConfigs MCP Server 配置列表
     */
    public void loadAll(List<McpServerConfig> serverConfigs) {
        if (serverConfigs == null || serverConfigs.isEmpty()) {
            return;
        }

        // 先清理已有的 MCP 工具（重新加载时避免重复注册）
        for (McpServerConfig config : serverConfigs) {
            if (config.isEnabled()) {
                // 发布全量失效事件，确保 ToolRegistry 缓存清空对应 server 的工具
                eventPublisher.publishEvent(
                        ToolChangeEvent.ofAll(this, "mcpReload: server=" + config.getName()));
            }
        }

        // 并发加载各 server 的工具
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (McpServerConfig config : serverConfigs) {
            if (!config.isEnabled()) {
                continue;
            }
            CompletableFuture<Void> future = CompletableFuture
                    .runAsync(() -> loadServer(config), executor)
                    .exceptionally(ex -> {
                        System.err.println("[McpToolLoader] Failed to load server '"
                                + config.getName() + "': " + ex.getMessage());
                        return null; // 单个 server 失败不影响整体
                    });
            futures.add(future);
        }

        // 等待所有 server 加载完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * 重新加载指定 MCP Server 的工具（用于 server 重连或工具刷新）。
     *
     * @param config MCP Server 配置
     */
    public void reloadServer(McpServerConfig config) {
        eventPublisher.publishEvent(
                ToolChangeEvent.ofAll(this, "mcpReloadServer: server=" + config.getName()));
        loadServer(config);
    }

    // ── 内部方法 ─────────────────────────────────────────────────

    /**
     * 加载单个 MCP Server 的工具：initialize → tools/list → register。
     */
    private void loadServer(McpServerConfig config) {
        try {
            // ① initialize 握手（验证连通性，获取 capabilities）
            JsonNode capabilities = mcpHttpClient.initialize(config);

            // 检查 server 是否支持 tools（capabilities.tools 存在）
            if (capabilities.path("tools").isMissingNode()) {
                System.out.println("[McpToolLoader] Server '" + config.getName()
                        + "' does not support tools capability, skipping.");
                return;
            }

            // ② tools/list 拉取工具定义
            List<McpToolDefinition> toolDefs = mcpHttpClient.listTools(config);
            if (toolDefs.isEmpty()) {
                System.out.println("[McpToolLoader] Server '" + config.getName()
                        + "' returned no tools.");
                return;
            }

            // ③ 批量注册工具到 ToolRegistry + ToolVersionStore
            int registered = 0;
            for (McpToolDefinition toolDef : toolDefs) {
                try {
                    registerMcpTool(config, toolDef);
                    registered++;
                } catch (Exception e) {
                    System.err.println("[McpToolLoader] Failed to register tool '"
                            + toolDef.getName() + "' from server '" + config.getName()
                            + "': " + e.getMessage());
                }
            }
            System.out.println("[McpToolLoader] Loaded " + registered + " tools from server '"
                    + config.getName() + "'");

        } catch (McpHttpClient.McpConnectionException e) {
            // 连接失败，记录错误，不抛出（容错）
            System.err.println("[McpToolLoader] Connection failed for server '"
                    + config.getName() + "': " + e.getMessage());
        }
    }

    /**
     * 将单个 MCP 工具定义注册到 ToolRegistry 和 ToolVersionStore。
     *
     * <p>工具名格式：{@code mcp__{serverName}__{toolName}}（与 TS buildMcpToolName 一致）。
     * MCP inputSchema 映射到 {@link UnifiedToolDeclaration} 的 parameters 字段。
     */
    private void registerMcpTool(McpServerConfig serverConfig, McpToolDefinition toolDef) {
        String normalizedServerName = normalizeName(serverConfig.getName());
        String normalizedToolName = normalizeName(toolDef.getName());
        String fullToolName = "mcp__" + normalizedServerName + "__" + normalizedToolName;
        String toolId = "mcp_" + normalizedServerName + "_" + normalizedToolName;

        LocalDateTime now = LocalDateTime.now();

        // 将 MCP inputSchema 转换为展开的声明字段
        McpDeclaration decl = buildDeclaration(toolDef);

        // 构造 executionConfig（McpToolExecutor 需要的运行时信息）
        ObjectNode executionConfig = MAPPER.createObjectNode();
        executionConfig.put("serverName", serverConfig.getName()); // 原始 serverName，用于查找 McpServerConfig
        executionConfig.put("mcpToolName", toolDef.getName());      // 原始 toolName，用于 tools/call

        // 构造版本（version=1，MCP 工具直接 PUBLISHED）
        ToolVersionDO version = new ToolVersionDO();
        version.setToolId(toolId);
        version.setVersion(1);
        version.setName(fullToolName);
        version.setDescription(decl.description);
        version.setParameters(decl.parametersJson);
        version.setRequiredParams(decl.requiredParamsJson);
        version.setExecutionConfig(executionConfig);
        version.setToolType(ToolType.MCP);
        version.setStatus(ToolVersionStatus.PUBLISHED);
        version.setCreatedAt(now);
        version.setPublishedAt(now);

        // 构造工具元信息
        ToolDO tool = ToolDO.builder()
                .id(toolId)
                .name(fullToolName)
                .toolType(ToolType.MCP)
                .visibility(ToolVisibility.PUBLIC)  // MCP 工具默认公开可见（所有 Character 可用）
                .status(ToolStatus.ACTIVE)
                .createdAt(now)
                .build();

        toolStore.save(tool);
        toolVersionStore.save(version);
        // 发布全量缓存失效，确保新注册的 MCP 工具在下次查询时立即生效
        eventPublisher.publishEvent(ToolChangeEvent.ofAll(this, "registerMcpTool: " + fullToolName));
    }

    /**
     * 将 MCP {@link McpToolDefinition} 的 inputSchema 解析为展开的声明字段。
     *
     * <p>MCP inputSchema 结构（JSON Schema 子集）：
     * <pre>
     * {
     *   "type": "object",
     *   "properties": {"param1": {"type": "string", "description": "..."}, ...},
     *   "required": ["param1"]
     * }
     * </pre>
     * parameters 和 requiredParams 字段序列化为 JSON 字符串，存储到 {@link ToolVersionDO}。
     */
    private McpDeclaration buildDeclaration(McpToolDefinition toolDef) {
        String description = toolDef.getDescription() != null ? toolDef.getDescription() : "";
        Map<String, ParameterSchema> paramsMap = new java.util.LinkedHashMap<>();
        List<String> requiredList = new ArrayList<>();

        JsonNode inputSchema = toolDef.getInputSchema();
        if (inputSchema != null && !inputSchema.isMissingNode()) {
            // 解析 properties
            JsonNode properties = inputSchema.path("properties");
            if (properties.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    paramsMap.put(entry.getKey(), buildParameterSchema(entry.getValue()));
                }
            }

            // 解析 required 列表
            JsonNode required = inputSchema.path("required");
            if (required.isArray()) {
                for (JsonNode req : required) {
                    requiredList.add(req.asText());
                }
            }
        }

        return new McpDeclaration(description, toJson(paramsMap), toJson(requiredList));
    }

    /** MCP 声明中间结果（将6展开字段传递给 ToolVersionDO） */
    private record McpDeclaration(String description, String parametersJson, String requiredParamsJson) {}

    private String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize to JSON", e);
        }
    }

    /**
     * 递归将 JSON Schema property 节点转换为 {@link ParameterSchema}。
     */
    private ParameterSchema buildParameterSchema(JsonNode schemaNode) {
        var builder = ParameterSchema.builder()
                .type(schemaNode.path("type").asText("string"));

        if (schemaNode.has("description")) {
            builder.description(schemaNode.path("description").asText());
        }

        // 枚举值
        if (schemaNode.has("enum")) {
            List<String> enumValues = new ArrayList<>();
            for (JsonNode enumVal : schemaNode.path("enum")) {
                enumValues.add(enumVal.asText());
            }
            builder.enumValues(enumValues);
        }

        // array 类型的 items
        if ("array".equals(schemaNode.path("type").asText()) && schemaNode.has("items")) {
            builder.items(buildParameterSchema(schemaNode.path("items")));
        }

        // object 类型的 properties（递归）
        if ("object".equals(schemaNode.path("type").asText()) && schemaNode.has("properties")) {
            Map<String, ParameterSchema> props = new java.util.LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = schemaNode.path("properties").fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                props.put(entry.getKey(), buildParameterSchema(entry.getValue()));
            }
            builder.properties(props);

            // 嵌套 required
            if (schemaNode.has("required")) {
                List<String> requiredList = new ArrayList<>();
                for (JsonNode req : schemaNode.path("required")) {
                    requiredList.add(req.asText());
                }
                builder.required(requiredList);
            }
        }

        return builder.build();
    }

    /**
     * 将名称规范化：将非字母数字字符替换为下划线。
     * 与 TS 项目 {@code normalization.ts} 中的 {@code normalizeNameForMCP} 逻辑一致。
     */
    static String normalizeName(String name) {
        if (name == null) return "";
        return name.replaceAll("[^a-zA-Z0-9]", "_");
    }
}
