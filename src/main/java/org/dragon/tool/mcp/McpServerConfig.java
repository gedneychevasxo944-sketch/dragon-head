package org.dragon.tool.mcp;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * MCP Server 连接配置。
 *
 * <p>描述一个外部 MCP Server 的连接信息，供 {@link McpHttpClient} 建立连接
 * 和 {@link McpToolLoader} 在启动时加载工具。
 *
 * <p><b>当前仅支持 HTTP 传输方式</b>（对应 MCP 规范的 Streamable HTTP Transport）。
 * 选择 HTTP 的原因：云端部署无需管理子进程，目标 MCP Server 暴露 HTTP 端点即可，运维成本最低。
 *
 * <p><b>HTTP 连接方式</b>：所有请求发往同一个端点（{@link #url}），
 * 使用 JSON-RPC 2.0 over HTTP POST，通过 {@code method} 字段区分操作：
 * <ul>
 *   <li>{@code initialize}  — 握手，获取 server capabilities</li>
 *   <li>{@code tools/list}  — 拉取工具列表</li>
 *   <li>{@code tools/call}  — 调用具体工具</li>
 * </ul>
 */
public class McpServerConfig {

    /**
     * MCP Server 唯一标识名称。
     * 用于构造工具名前缀：工具名 = {@code mcp__{name}__{toolName}}。
     * 应与 TS 项目中的 server name 保持一致（对应 {@code buildMcpToolName(serverName, toolName)}）。
     *
     * <p><b>不可修改</b>：name 一经创建即不允许变更。
     * 原因：所有由该 Server 同步产生的 {@code ToolVersionDO.name}（如 {@code mcp__github__search_code}）
     * 均以此 name 为前缀构造，修改 name 会导致历史工具与新工具名映射断裂，
     * 进而造成 ToolRegistry 缓存脏数据和 LLM tool_call 无法路由。
     * 如需更换名称，应创建新的 MCP Server 配置，并手动迁移绑定关系。
     * Service 层在更新 MCP Server 配置时须校验此字段是否发生变更，若有则拒绝并返回错误。
     */
    private String name;

    /**
     * MCP Server 的 HTTP 端点 URL。
     * 例如：{@code https://mcp.github.com/v1}。
     * 所有 JSON-RPC 请求均 POST 到此 URL。
     */
    private String url;

    /**
     * Bearer Token（可选）。
     * 如果 MCP Server 需要认证，将此值作为 {@code Authorization: Bearer {authToken}} 请求头发送。
     */
    private String authToken;

    /**
     * 额外的自定义请求头（可选）。
     * 用于传递 API Key、Tenant ID 等服务特定认证信息。
     */
    private Map<String, String> headers;

    /**
     * 是否启用此 MCP Server。
     * false 时 {@link McpToolLoader} 跳过此 server，不尝试连接。
     */
    private boolean enabled;

    /**
     * 连接超时时间（毫秒）。
     * 默认 10000ms（10 秒），包括 initialize 握手超时。
     */
    private int connectTimeoutMs;

    /**
     * 工具调用超时时间（毫秒）。
     * 默认 30000ms（30 秒），tools/call 请求超时。
     */
    private int callTimeoutMs;

    public McpServerConfig() {
        this.enabled = true;
        this.connectTimeoutMs = 10_000;
        this.callTimeoutMs = 30_000;
        this.headers = Collections.emptyMap();
    }

    private McpServerConfig(Builder builder) {
        this.name = builder.name;
        this.url = builder.url;
        this.authToken = builder.authToken;
        this.headers = builder.headers != null ? builder.headers : Collections.emptyMap();
        this.enabled = builder.enabled;
        this.connectTimeoutMs = builder.connectTimeoutMs;
        this.callTimeoutMs = builder.callTimeoutMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    // ── Getters & Setters ─────────────────────────────────────────────

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getCallTimeoutMs() {
        return callTimeoutMs;
    }

    public void setCallTimeoutMs(int callTimeoutMs) {
        this.callTimeoutMs = callTimeoutMs;
    }

    @Override
    public String toString() {
        return "McpServerConfig{name='" + name + "', url='" + url + "', enabled=" + enabled + "}";
    }

    // ── Builder ───────────────────────────────────────────────────────

    public static final class Builder {
        private String name;
        private String url;
        private String authToken;
        private Map<String, String> headers;
        private boolean enabled = true;
        private int connectTimeoutMs = 10_000;
        private int callTimeoutMs = 30_000;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder authToken(String authToken) {
            this.authToken = authToken;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder connectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
            return this;
        }

        public Builder callTimeoutMs(int callTimeoutMs) {
            this.callTimeoutMs = callTimeoutMs;
            return this;
        }

        public McpServerConfig build() {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(url, "url must not be null");
            return new McpServerConfig(this);
        }
    }
}
