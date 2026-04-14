package org.dragon.tool.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP HTTP 客户端。
 *
 * <p>实现 JSON-RPC 2.0 over HTTP POST，用于与外部 MCP Server 通信。
 * 对应 TS 项目中 {@code src/services/mcp/client.ts} 的 HTTP 连接部分。
 *
 * <p><b>协议交互流程</b>（三个核心操作）：
 * <pre>
 * ① initialize（握手）：
 *   POST {url}  {"jsonrpc":"2.0","id":1,"method":"initialize","params":{...}}
 *   → 确认 MCP 版本兼容，获取 server capabilities（尤其是 capabilities.tools 是否存在）
 *
 * ② tools/list（工具发现，启动时调用）：
 *   POST {url}  {"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
 *   → 返回该 server 提供的所有工具定义（name, description, inputSchema）
 *
 * ③ tools/call（工具执行，运行时调用）：
 *   POST {url}  {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"...","arguments":{...}}}
 *   → 返回工具执行结果 content 数组（通常是 text 类型）
 * </pre>
 *
 * <p><b>传输实现</b>：使用 JDK 11 自带的 {@link HttpClient}，
 * 无需引入额外 HTTP 库。
 *
 * <p><b>错误处理</b>：
 * <ul>
 *   <li>网络错误 / 超时 → 抛出 {@link McpConnectionException}</li>
 *   <li>JSON-RPC 错误响应（error 字段存在）→ 抛出 {@link McpCallException}</li>
 *   <li>HTTP 非 2xx → 抛出 {@link McpConnectionException}</li>
 * </ul>
 */
public class McpHttpClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String MCP_PROTOCOL_VERSION = "2024-11-05";
    private static final String CLIENT_NAME = "tool-platform";
    private static final String CLIENT_VERSION = "1.0";
    private static final String CONTENT_TYPE = "application/json";

    /** JSON-RPC 请求 ID 自增器（每个 client 实例独立计数，线程安全） */
    private final AtomicInteger requestIdCounter = new AtomicInteger(1);

    private final HttpClient httpClient;

    public McpHttpClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    // ── 核心操作 ─────────────────────────────────────────────────────

    /**
     * 执行 MCP initialize 握手，验证连通性并获取 server capabilities。
     *
     * <p>对应 MCP 规范的 {@code initialize} 请求。必须在 {@link #listTools} 之前调用。
     *
     * @param config MCP Server 配置
     * @return server capabilities（JsonNode，包含 {@code tools}、{@code resources} 等字段）
     * @throws McpConnectionException 如果连接失败或握手失败
     */
    public JsonNode initialize(McpServerConfig config) {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("protocolVersion", MCP_PROTOCOL_VERSION);

        ObjectNode capabilities = MAPPER.createObjectNode();
        params.set("capabilities", capabilities);

        ObjectNode clientInfo = MAPPER.createObjectNode();
        clientInfo.put("name", CLIENT_NAME);
        clientInfo.put("version", CLIENT_VERSION);
        params.set("clientInfo", clientInfo);

        JsonNode response = sendRequest(config, "initialize", params);
        return response.path("result").path("capabilities");
    }

    /**
     * 拉取 MCP Server 的工具列表。
     *
     * <p>对应 MCP 规范的 {@code tools/list} 请求。
     * 返回的工具定义包含 name、description、inputSchema，
     * 供 {@link McpToolLoader} 转换并注册到 {@link org.dragon.tool.runtime.ToolRegistry}。
     *
     * @param config MCP Server 配置
     * @return 工具定义列表（可能为空列表，不为 null）
     * @throws McpConnectionException 如果请求失败
     */
    public List<McpToolDefinition> listTools(McpServerConfig config) {
        ObjectNode params = MAPPER.createObjectNode();
        JsonNode response = sendRequest(config, "tools/list", params);

        JsonNode toolsNode = response.path("result").path("tools");
        List<McpToolDefinition> tools = new ArrayList<>();

        if (toolsNode.isArray()) {
            for (JsonNode toolNode : toolsNode) {
                String name = toolNode.path("name").asText();
                String description = toolNode.path("description").asText();
                JsonNode inputSchema = toolNode.path("inputSchema");
                if (!name.isEmpty()) {
                    tools.add(new McpToolDefinition(name, description, inputSchema));
                }
            }
        }
        return tools;
    }

    /**
     * 调用 MCP Server 上的指定工具。
     *
     * <p>对应 MCP 规范的 {@code tools/call} 请求。
     * 返回结果 content 数组中的文本内容，多个 text block 之间用换行拼接。
     *
     * @param config      MCP Server 配置
     * @param toolName    工具原始名称（不含 mcp__ 前缀，如 {@code search_code}）
     * @param arguments   工具调用参数（JsonNode 对象）
     * @return 工具执行结果的文本表示
     * @throws McpConnectionException 如果请求失败
     * @throws McpCallException       如果工具执行返回 JSON-RPC error 响应
     */
    public String callTool(McpServerConfig config, String toolName, JsonNode arguments) {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("name", toolName);
        params.set("arguments", arguments != null ? arguments : MAPPER.createObjectNode());

        JsonNode response = sendRequest(config, "tools/call", params);
        JsonNode result = response.path("result");

        // 提取 content 数组中的 text 内容，多个 block 拼接
        JsonNode contentArray = result.path("content");
        StringBuilder sb = new StringBuilder();
        if (contentArray.isArray()) {
            for (JsonNode block : contentArray) {
                String type = block.path("type").asText();
                if ("text".equals(type)) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(block.path("text").asText());
                }
                // 其他类型（image/resource）暂不处理，预留扩展
            }
        }
        return sb.toString();
    }

    // ── 内部辅助方法 ──────────────────────────────────────────────────

    /**
     * 发送 JSON-RPC 2.0 请求到 MCP Server，返回完整响应 JsonNode。
     *
     * @param config MCP Server 配置
     * @param method JSON-RPC method 名称
     * @param params 请求参数
     * @return 完整响应 JsonNode（包含 jsonrpc、id、result 或 error 字段）
     * @throws McpConnectionException 如果网络/HTTP/超时错误
     * @throws McpCallException       如果响应包含 JSON-RPC error 字段
     */
    private JsonNode sendRequest(McpServerConfig config, String method, ObjectNode params) {
        int requestId = requestIdCounter.getAndIncrement();

        // 构造 JSON-RPC 2.0 请求体
        ObjectNode rpcRequest = MAPPER.createObjectNode();
        rpcRequest.put("jsonrpc", "2.0");
        rpcRequest.put("id", requestId);
        rpcRequest.put("method", method);
        rpcRequest.set("params", params);

        String requestBody;
        try {
            requestBody = MAPPER.writeValueAsString(rpcRequest);
        } catch (Exception e) {
            throw new McpConnectionException(config.getName(),
                    "Failed to serialize JSON-RPC request: " + e.getMessage(), e);
        }

        // 构造 HTTP 请求
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(config.getUrl()))
                .timeout(Duration.ofMillis(
                        "tools/call".equals(method) ? config.getCallTimeoutMs() : config.getConnectTimeoutMs()))
                .header("Content-Type", CONTENT_TYPE)
                .header("Accept", CONTENT_TYPE)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        // 添加 Authorization 头
        if (config.getAuthToken() != null && !config.getAuthToken().isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + config.getAuthToken());
        }

        // 添加自定义请求头
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(requestBuilder::header);
        }

        // 发送请求
        HttpResponse<String> httpResponse;
        try {
            httpResponse = httpClient.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (java.net.http.HttpTimeoutException e) {
            throw new McpConnectionException(config.getName(),
                    "Request timed out for method '" + method + "'", e);
        } catch (Exception e) {
            throw new McpConnectionException(config.getName(),
                    "Network error for method '" + method + "': " + e.getMessage(), e);
        }

        // 检查 HTTP 状态码
        int statusCode = httpResponse.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new McpConnectionException(config.getName(),
                    "HTTP error " + statusCode + " for method '" + method
                            + "': " + httpResponse.body());
        }

        // 解析响应体
        JsonNode responseNode;
        try {
            responseNode = MAPPER.readTree(httpResponse.body());
        } catch (Exception e) {
            throw new McpConnectionException(config.getName(),
                    "Failed to parse JSON-RPC response: " + e.getMessage(), e);
        }

        // 检查 JSON-RPC 错误响应
        JsonNode errorNode = responseNode.path("error");
        if (!errorNode.isMissingNode() && !errorNode.isNull()) {
            int code = errorNode.path("code").asInt();
            String message = errorNode.path("message").asText();
            throw new McpCallException(config.getName(), toolNameFromMethod(method), code, message);
        }

        return responseNode;
    }

    private String toolNameFromMethod(String method) {
        return "tools/call".equals(method) ? "tool_call" : method;
    }

    // ── 内部异常类 ────────────────────────────────────────────────────

    /**
     * MCP Server 连接/网络/HTTP 层面的异常。
     */
    public static class McpConnectionException extends RuntimeException {
        private final String serverName;

        public McpConnectionException(String serverName, String message, Throwable cause) {
            super("[MCP:" + serverName + "] " + message, cause);
            this.serverName = serverName;
        }

        public McpConnectionException(String serverName, String message) {
            super("[MCP:" + serverName + "] " + message);
            this.serverName = serverName;
        }

        public String getServerName() {
            return serverName;
        }
    }

    /**
     * MCP tools/call 返回 JSON-RPC error 响应时的异常。
     */
    public static class McpCallException extends RuntimeException {
        private final String serverName;
        private final String toolName;
        private final int errorCode;

        public McpCallException(String serverName, String toolName, int errorCode, String message) {
            super("[MCP:" + serverName + "] Tool '" + toolName + "' returned error "
                    + errorCode + ": " + message);
            this.serverName = serverName;
            this.toolName = toolName;
            this.errorCode = errorCode;
        }

        public String getServerName() {
            return serverName;
        }

        public String getToolName() {
            return toolName;
        }

        public int getErrorCode() {
            return errorCode;
        }
    }
}
