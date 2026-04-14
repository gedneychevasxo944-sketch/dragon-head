package org.dragon.tool.runtime.factory;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.dragon.tool.enums.ToolType;
import org.dragon.tool.mcp.McpHttpClient;
import org.dragon.tool.mcp.McpServerConfig;
import org.dragon.tool.runtime.Tool;
import org.dragon.tool.runtime.ToolFactory;
import org.dragon.tool.runtime.ToolDefinition;
import org.dragon.tool.runtime.tools.McpTool;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 类型工具 Factory。
 *
 * <p>持有 {@link McpHttpClient} 和 serverName → {@link McpServerConfig} 映射，
 * 构建绑定了具体 server + toolName 的 {@link McpTool} 实例。
 *
 * <p>实例可安全跨调用复用（{@link #isSingleton()} 返回 {@code true}）。
 */
@Slf4j
public class McpToolFactory implements ToolFactory {

    private final McpHttpClient mcpHttpClient;
    /** serverName（原始名，如 "github"） → McpServerConfig */
    private final Map<String, McpServerConfig> serverConfigMap = new ConcurrentHashMap<>();

    public McpToolFactory(McpHttpClient mcpHttpClient) {
        this.mcpHttpClient = Objects.requireNonNull(mcpHttpClient, "mcpHttpClient must not be null");
    }

    /**
     * 注册或更新 MCP Server 配置（应用启动时由 McpServerManager 调用）。
     *
     * <p>key 为 {@link McpServerConfig#getName()}，即 MCP Server 的唯一标识。
     * {@code name} 字段一经创建不允许变更（参见 {@link McpServerConfig#name} 字段注释），
     * 此处直接覆盖写入，上层 Service 须在调用前完成 name 不可变校验。
     */
    public void registerServerConfig(McpServerConfig config) {
        Objects.requireNonNull(config, "McpServerConfig must not be null");
        serverConfigMap.put(config.getName(), config);
        log.info("[McpToolFactory] 注册 MCP Server: name={}", config.getName());
    }

    @Override
    public ToolType supportedType() {
        return ToolType.MCP;
    }

    /**
     * 构建绑定了 MCP Server + toolName 的 {@link McpTool} 实例。
     *
     * @throws IllegalArgumentException 若 executionConfig 缺少 serverName / mcpToolName，
     *                                  或 serverName 未注册
     */
    @Override
    public Tool<JsonNode, ?> create(ToolDefinition runtime) {
        JsonNode config = runtime.getExecutionConfig();
        if (config == null) {
            throw new IllegalArgumentException(
                    "McpToolFactory: executionConfig is null for tool '" + runtime.getToolId() + "'");
        }

        String serverName = config.path("serverName").asText(null);
        String mcpToolName = config.path("mcpToolName").asText(null);

        if (serverName == null || serverName.isEmpty()) {
            throw new IllegalArgumentException(
                    "McpToolFactory: missing 'serverName' in executionConfig for tool '"
                            + runtime.getToolId() + "'");
        }
        if (mcpToolName == null || mcpToolName.isEmpty()) {
            throw new IllegalArgumentException(
                    "McpToolFactory: missing 'mcpToolName' in executionConfig for tool '"
                            + runtime.getToolId() + "'");
        }

        McpServerConfig serverConfig = serverConfigMap.get(serverName);
        if (serverConfig == null) {
            throw new IllegalArgumentException(
                    "McpToolFactory: no McpServerConfig registered for server '"
                            + serverName + "'. Registered servers: " + serverConfigMap.keySet());
        }

        return new McpTool(runtime, mcpHttpClient, serverConfig, mcpToolName);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}

