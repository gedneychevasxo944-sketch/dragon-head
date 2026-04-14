package org.dragon.tool.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dragon.tool.runtime.ToolChangeEvent;
import org.dragon.tool.runtime.factory.McpToolFactory;
import org.dragon.tool.store.ToolStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * MCP Server 管理服务。
 *
 * <p><b>职责边界</b>：
 * <ul>
 *   <li>MCP Server 配置的 CRUD（含 name 不可修改校验）</li>
 *   <li>启用 / 禁用 MCP Server</li>
 *   <li>将数据库配置同步到内存（{@link McpToolFactory#registerServerConfig}）</li>
 *   <li>触发工具同步（委托给 {@link McpToolLoader}）</li>
 * </ul>
 *
 * <p><b>name 不可修改约束</b>：{@link McpServerDO#getName()} 一经创建不允许变更。
 * 所有由该 Server 同步产生的工具名（{@code mcp__{name}__{toolName}}）均以此 name 为前缀，
 * 修改会导致历史工具名映射断裂。本 Service 在 {@link #update} 中强制校验此约束。
 *
 * <p><b>启动加载</b>：应用启动时调用 {@link #loadAllToMemory()}，
 * 将所有 enabled 的 Server 配置注入 {@link McpToolFactory}，
 * 再由 {@link McpToolLoader} 完成工具同步。
 */
public class McpServerService {

    private static final Logger log = LoggerFactory.getLogger(McpServerService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final McpServerStore mcpServerStore;
    private final McpToolFactory mcpToolFactory;
    private final McpToolLoader mcpToolLoader;
    private final ToolStore toolStore;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * @param mcpServerStore   MCP Server 持久化接口
     * @param mcpToolFactory   MCP 工具 Factory（维护内存中的 serverConfigMap）
     * @param mcpToolLoader    MCP 工具加载器（负责 initialize + tools/list + 注册工具）
     * @param toolStore        工具持久化接口（禁用 server 时批量修改关联工具状态）
     * @param eventPublisher   Spring 事件发布器（触发 ToolRegistry 缓存失效）
     */
    public McpServerService(McpServerStore mcpServerStore,
                            McpToolFactory mcpToolFactory,
                            McpToolLoader mcpToolLoader,
                            ToolStore toolStore,
                            ApplicationEventPublisher eventPublisher) {
        this.mcpServerStore = Objects.requireNonNull(mcpServerStore, "mcpServerStore must not be null");
        this.mcpToolFactory = Objects.requireNonNull(mcpToolFactory, "mcpToolFactory must not be null");
        this.mcpToolLoader = Objects.requireNonNull(mcpToolLoader, "mcpToolLoader must not be null");
        this.toolStore = Objects.requireNonNull(toolStore, "toolStore must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    // ── 应用启动 ──────────────────────────────────────────────────────────

    /**
     * 应用启动时全量加载：将所有 enabled 的 MCP Server 配置注入 McpToolFactory，
     * 再触发工具同步（initialize → tools/list → 注册工具）。
     *
     * <p>该方法应在 Spring 容器完全初始化后调用（如 {@code @PostConstruct} 或
     * {@code ApplicationReadyEvent} 监听器中）。
     */
    public void loadAllToMemory() {
        List<McpServerDO> servers = mcpServerStore.findAllEnabled();
        if (servers.isEmpty()) {
            log.info("[McpServerService] 无启用的 MCP Server，跳过加载。");
            return;
        }

        List<McpServerConfig> configs = servers.stream()
                .map(this::toConfig)
                .toList();

        // 注入运行时 Factory（内存 serverConfigMap）
        configs.forEach(mcpToolFactory::registerServerConfig);

        // 触发工具同步
        mcpToolLoader.loadAll(configs);

        log.info("[McpServerService] 启动加载完成，共加载 {} 个 MCP Server。", servers.size());
    }

    // ── CRUD ──────────────────────────────────────────────────────────────

    /**
     * 创建 MCP Server 配置。
     *
     * <p>创建后不自动触发工具同步，需显式调用 {@link #syncTools(Long)} 或等待下次启动。
     *
     * @param name             唯一标识名（创建后不可修改，仅允许字母/数字/中划线/下划线，长度 2-64）
     * @param displayName      展示名称
     * @param description      描述
     * @param url              HTTP 端点 URL
     * @param authToken        Bearer Token（可为 null）
     * @param headers          自定义请求头（可为 null）
     * @param connectTimeoutMs 连接超时（毫秒），传 null 使用默认值 10000
     * @param callTimeoutMs    调用超时（毫秒），传 null 使用默认值 30000
     * @return 新建记录的 id
     * @throws IllegalArgumentException 如果 name 已存在（含已软删除记录）或格式非法
     */
    public Long create(String name,
                       String displayName,
                       String description,
                       String url,
                       String authToken,
                       Map<String, String> headers,
                       Integer connectTimeoutMs,
                       Integer callTimeoutMs) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(url, "url must not be null");

        validateName(name);

        if (mcpServerStore.existsByName(name)) {
            throw new IllegalArgumentException(
                    "MCP Server name 已存在：'" + name + "'。name 全局唯一（含已删除记录），"
                            + "如需复用请联系管理员清理旧记录。");
        }

        LocalDateTime now = LocalDateTime.now();
        McpServerDO server = McpServerDO.builder()
                .name(name)
                .displayName(displayName)
                .description(description)
                .url(url)
                .authToken(authToken)
                .headersJson(toJson(headers))
                .connectTimeoutMs(connectTimeoutMs != null ? connectTimeoutMs : 10_000)
                .callTimeoutMs(callTimeoutMs != null ? callTimeoutMs : 30_000)
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        mcpServerStore.save(server);

        log.info("[McpServerService] MCP Server 创建成功: id={}, name={}, url={}",
                server.getId(), name, url);

        return server.getId();
    }

    /**
     * 更新 MCP Server 配置（不含 name，name 不可修改）。
     *
     * <p>更新后如果 server 处于 enabled 状态，会同步刷新 McpToolFactory 中的内存配置。
     * 工具列表不会自动重新同步，如有需要请显式调用 {@link #syncTools(Long)}。
     *
     * @param id               MCP Server id
     * @param displayName      展示名称（null 表示不修改）
     * @param description      描述（null 表示不修改）
     * @param url              HTTP 端点 URL（null 表示不修改）
     * @param authToken        Bearer Token（null 表示不修改；传空字符串表示清空）
     * @param headers          自定义请求头（null 表示不修改；传空 Map 表示清空）
     * @param connectTimeoutMs 连接超时（毫秒），null 表示不修改
     * @param callTimeoutMs    调用超时（毫秒），null 表示不修改
     * @throws IllegalArgumentException 如果 server 不存在
     */
    public void update(Long id,
                       String displayName,
                       String description,
                       String url,
                       String authToken,
                       Map<String, String> headers,
                       Integer connectTimeoutMs,
                       Integer callTimeoutMs) {
        Objects.requireNonNull(id, "id must not be null");

        McpServerDO server = requireServer(id);

        // 选择性更新（null 表示不修改）
        if (displayName != null) {
            server.setDisplayName(displayName);
        }
        if (description != null) {
            server.setDescription(description);
        }
        if (url != null) {
            server.setUrl(url);
        }
        if (authToken != null) {
            server.setAuthToken(authToken.isEmpty() ? null : authToken);
        }
        if (headers != null) {
            server.setHeadersJson(toJson(headers));
        }
        if (connectTimeoutMs != null) {
            server.setConnectTimeoutMs(connectTimeoutMs);
        }
        if (callTimeoutMs != null) {
            server.setCallTimeoutMs(callTimeoutMs);
        }
        server.setUpdatedAt(LocalDateTime.now());

        mcpServerStore.update(server);

        // 若 server 处于 enabled 状态，同步刷新内存配置
        if (server.isEnabled()) {
            mcpToolFactory.registerServerConfig(toConfig(server));
        }

        log.info("[McpServerService] MCP Server 更新成功: id={}, name={}", id, server.getName());
    }

    /**
     * 启用 MCP Server。
     *
     * <p>启用后自动将配置注入 McpToolFactory，并触发工具同步。
     *
     * @param id MCP Server id
     * @throws IllegalArgumentException 如果 server 不存在或已软删除
     * @throws IllegalStateException    如果 server 已是 enabled 状态
     */
    public void enable(Long id) {
        Objects.requireNonNull(id, "id must not be null");

        McpServerDO server = requireServer(id);
        if (server.isEnabled()) {
            throw new IllegalStateException(
                    "MCP Server '" + server.getName() + "' 已是启用状态。");
        }

        server.setEnabled(true);
        server.setUpdatedAt(LocalDateTime.now());
        mcpServerStore.update(server);

        // 注入内存配置 + 触发工具同步
        McpServerConfig config = toConfig(server);
        mcpToolFactory.registerServerConfig(config);
        mcpToolLoader.reloadServer(config);

        log.info("[McpServerService] MCP Server 启用: id={}, name={}", id, server.getName());
    }

    /**
     * 禁用 MCP Server。
     *
     * <p>禁用后发布 ToolChangeEvent 触发 ToolRegistry 缓存失效，
     * 该 Server 下的所有 MCP 工具对 Agent 立即不可见。
     * 工具记录本身状态变为 {@code DISABLED}（不删除），以便重新启用时恢复。
     *
     * @param id MCP Server id
     * @throws IllegalArgumentException 如果 server 不存在或已软删除
     * @throws IllegalStateException    如果 server 已是禁用状态
     */
    public void disable(Long id) {
        Objects.requireNonNull(id, "id must not be null");

        McpServerDO server = requireServer(id);
        if (!server.isEnabled()) {
            throw new IllegalStateException(
                    "MCP Server '" + server.getName() + "' 已是禁用状态。");
        }

        server.setEnabled(false);
        server.setUpdatedAt(LocalDateTime.now());
        mcpServerStore.update(server);

        // 将该 Server 下所有 MCP 工具标记为 DISABLED
        disableToolsByServerName(server.getName());

        // 触发全量缓存失效（ToolRegistry 下次查询时重新加载，不含该 Server 的工具）
        eventPublisher.publishEvent(
                ToolChangeEvent.ofAll(this, "disableMcpServer: server=" + server.getName()));

        log.info("[McpServerService] MCP Server 禁用: id={}, name={}", id, server.getName());
    }

    /**
     * 软删除 MCP Server。
     *
     * <p>删除后该 Server 下的所有 MCP 工具状态变为 DISABLED（不硬删除工具），
     * 触发 ToolRegistry 缓存全量失效。
     *
     * @param id MCP Server id
     * @throws IllegalArgumentException 如果 server 不存在或已软删除
     */
    public void delete(Long id) {
        Objects.requireNonNull(id, "id must not be null");

        McpServerDO server = requireServer(id);

        server.setDeletedAt(LocalDateTime.now());
        server.setEnabled(false);
        server.setUpdatedAt(LocalDateTime.now());
        mcpServerStore.update(server);

        // 将该 Server 下所有 MCP 工具标记为 DISABLED
        disableToolsByServerName(server.getName());

        // 触发全量缓存失效
        eventPublisher.publishEvent(
                ToolChangeEvent.ofAll(this, "deleteMcpServer: server=" + server.getName()));

        log.info("[McpServerService] MCP Server 软删除: id={}, name={}", id, server.getName());
    }

    // ── 工具同步 ──────────────────────────────────────────────────────────

    /**
     * 手动触发指定 MCP Server 的工具同步（initialize → tools/list → 注册/更新工具）。
     *
     * <p>同步是实时阻塞式操作（当前线程等待 MCP Server 响应）。
     * 同步结果（新增/更新/禁用的工具数量）记录在普通日志中，不触发 ToolActionLog。
     *
     * @param id MCP Server id
     * @throws IllegalArgumentException 如果 server 不存在、已软删除或未启用
     */
    public void syncTools(Long id) {
        Objects.requireNonNull(id, "id must not be null");

        McpServerDO server = requireServer(id);
        if (!server.isEnabled()) {
            throw new IllegalArgumentException(
                    "MCP Server '" + server.getName() + "' 未启用，无法同步工具。请先启用该 Server。");
        }

        McpServerConfig config = toConfig(server);

        // 确保内存配置是最新的
        mcpToolFactory.registerServerConfig(config);

        // 触发工具同步（阻塞等待完成）
        mcpToolLoader.reloadServer(config);

        log.info("[McpServerService] MCP Server 工具同步完成: id={}, name={}", id, server.getName());
    }

    // ── 查询 ──────────────────────────────────────────────────────────────

    /**
     * 按 id 查询 MCP Server（含已软删除）。
     *
     * @param id MCP Server id
     * @return Optional 包装的 MCP Server 配置
     */
    public Optional<McpServerDO> findById(Long id) {
        return mcpServerStore.findById(id);
    }

    /**
     * 按 name 查询 MCP Server（含已软删除）。
     *
     * @param name MCP Server 唯一标识名
     * @return Optional 包装的 MCP Server 配置
     */
    public Optional<McpServerDO> findByName(String name) {
        return mcpServerStore.findByName(name);
    }

    /**
     * 查询所有未删除的 MCP Server 配置，按创建时间升序。
     *
     * @return 所有未删除的 MCP Server 列表
     */
    public List<McpServerDO> findAll() {
        return mcpServerStore.findAll();
    }

    // ── 内部辅助 ──────────────────────────────────────────────────────────

    /**
     * 按 id 加载 server，断言存在且未软删除。
     */
    private McpServerDO requireServer(Long id) {
        McpServerDO server = mcpServerStore.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "MCP Server 不存在: id=" + id));
        if (server.isDeleted()) {
            throw new IllegalArgumentException(
                    "MCP Server 已被删除: id=" + id + ", name=" + server.getName());
        }
        return server;
    }

    /**
     * 将持久化 DO 转换为运行时 {@link McpServerConfig}。
     */
    private McpServerConfig toConfig(McpServerDO server) {
        Map<String, String> headers = parseHeaders(server.getHeadersJson());
        return McpServerConfig.builder()
                .name(server.getName())
                .url(server.getUrl())
                .authToken(server.getAuthToken())
                .headers(headers)
                .enabled(server.isEnabled())
                .connectTimeoutMs(server.getConnectTimeoutMs())
                .callTimeoutMs(server.getCallTimeoutMs())
                .build();
    }

    /**
     * 将该 MCP Server 下所有关联 MCP 工具的状态设为 DISABLED。
     *
     * <p>通过工具名前缀 {@code mcp__{normalizedName}__} 匹配该 Server 的工具。
     * 当前实现依赖 {@link ToolStore#disableByNamePrefix}；
     * 若 ToolStore 暂未提供该方法，可由调用方先按前缀查工具再逐个 update。
     */
    private void disableToolsByServerName(String serverName) {
        String normalizedName = serverName.replaceAll("[^a-zA-Z0-9]", "_");
        String prefix = "mcp__" + normalizedName + "__";
        // ToolStore 的 disableByNamePrefix 由实现层按 name LIKE 'prefix%' 批量更新
        toolStore.disableByNamePrefix(prefix);
        log.info("[McpServerService] 已禁用 MCP Server '{}' 下的所有工具（前缀='{}'）",
                serverName, prefix);
    }

    /**
     * 校验 name 格式（字母/数字/中划线/下划线，长度 2-64）。
     */
    private void validateName(String name) {
        if (!name.matches("^[a-zA-Z0-9_\\-]{2,64}$")) {
            throw new IllegalArgumentException(
                    "MCP Server name 格式非法：'" + name + "'。"
                            + "只允许字母、数字、中划线、下划线，长度 2-64。");
        }
    }

    /**
     * 解析 headersJson 字符串为 Map。
     */
    private Map<String, String> parseHeaders(String headersJson) {
        if (headersJson == null || headersJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return MAPPER.readValue(headersJson, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("[McpServerService] headersJson 解析失败，使用空 Map: {}", headersJson, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 序列化对象为 JSON 字符串；null 输入返回 null。
     */
    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalStateException("McpServerService: JSON 序列化失败", e);
        }
    }
}

