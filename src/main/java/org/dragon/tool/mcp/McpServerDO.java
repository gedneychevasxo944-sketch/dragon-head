package org.dragon.tool.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MCP Server 配置领域对象（对应数据库 mcp_servers 表）。
 *
 * <p>字段分类：
 * <ul>
 *   <li>标识：id（自增 bigint，DO 中用 Long）</li>
 *   <li>唯一标识名：name（<b>创建后不可修改</b>，详见字段注释）</li>
 *   <li>连接配置：url、authToken、headers、connectTimeoutMs、callTimeoutMs</li>
 *   <li>状态：enabled、deletedAt</li>
 *   <li>时间戳：createdAt、updatedAt</li>
 * </ul>
 *
 * <p>运行时连接信息由 {@link McpServerService} 从本 DO 组装成 {@link McpServerConfig}，
 * 注入到 {@link org.dragon.tool.runtime.factory.McpToolFactory}。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpServerDO {

    // ── 标识 ─────────────────────────────────────────────────────────

    /**
     * 自增主键（对应表的 BIGINT AUTO_INCREMENT）。
     * 领域层统一用 Long，序列化到 API 时可转 String 防止精度丢失。
     */
    private Long id;

    // ── 唯一标识名 ────────────────────────────────────────────────────

    /**
     * MCP Server 唯一标识名称。
     *
     * <p><b>不可修改</b>：name 一经创建即不允许变更。
     * 所有由该 Server 同步产生的工具名（{@code ToolVersionDO.name}）均以此 name 为前缀构造
     * （格式：{@code mcp__{name}__{toolName}}）。修改 name 会导致历史工具名映射断裂，
     * 造成 ToolRegistry 缓存脏数据和 LLM tool_call 无法路由。
     * 如需更换名称，应创建新的 MCP Server，并手动迁移绑定关系。
     *
     * <p>命名规范：仅允许字母、数字、中划线、下划线，长度 2-64，建议全小写。
     * 示例：{@code github}、{@code search-api}、{@code internal_docs}。
     */
    private String name;

    // ── 展示信息 ─────────────────────────────────────────────────────

    /**
     * 展示名称（管理页面展示用，用户可自由编辑）。
     * 不参与工具名构造，与 {@link #name} 独立。
     */
    private String displayName;

    /** 描述，说明该 MCP Server 提供的能力 */
    private String description;

    // ── 连接配置 ─────────────────────────────────────────────────────

    /**
     * MCP Server 的 HTTP 端点 URL。
     * 例如：{@code https://mcp.github.com/v1}。
     * 所有 JSON-RPC 请求均 POST 到此 URL。
     */
    private String url;

    /**
     * Bearer Token（可选）。
     * 如果 MCP Server 需要认证，将此值作为 {@code Authorization: Bearer {authToken}} 发送。
     * 存储时建议加密，读取时由 Service 层解密后填充到 {@link McpServerConfig}。
     */
    private String authToken;

    /**
     * 额外的自定义请求头（序列化为 JSON 字符串存储，如 {@code {"X-Api-Key":"xxx"}}）。
     * 用于传递 API Key、Tenant ID 等服务特定认证信息。
     */
    private String headersJson;

    /**
     * 连接超时时间（毫秒）。默认 10000ms（10 秒）。
     */
    @Builder.Default
    private int connectTimeoutMs = 10_000;

    /**
     * 工具调用超时时间（毫秒）。默认 30000ms（30 秒）。
     */
    @Builder.Default
    private int callTimeoutMs = 30_000;

    // ── 状态 ─────────────────────────────────────────────────────────

    /**
     * 是否启用。
     * false 时 McpToolLoader 跳过此 server，不尝试连接和同步工具。
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * 软删除标记。非 null 时表示已删除。
     * 删除后其下所有 MCP 工具状态自动设为 DISABLED（由 McpServerService 负责）。
     */
    private LocalDateTime deletedAt;

    // ── 时间戳 ─────────────────────────────────────────────────────────

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 最后更新时间 */
    private LocalDateTime updatedAt;

    // ── 语义方法 ─────────────────────────────────────────────────────────

    /** 是否已被软删除 */
    public boolean isDeleted() {
        return deletedAt != null;
    }
}

