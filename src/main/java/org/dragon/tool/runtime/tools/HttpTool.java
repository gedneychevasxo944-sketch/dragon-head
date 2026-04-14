package org.dragon.tool.runtime.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.dragon.tool.runtime.AbstractTool;
import org.dragon.tool.runtime.ToolProgress;
import org.dragon.tool.runtime.ToolResult;
import org.dragon.tool.runtime.ToolResultBlockParam;
import org.dragon.tool.runtime.ToolDefinition;
import org.dragon.tool.runtime.ToolUseContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * HTTP 类型工具。
 *
 * <p>封装对第三方 HTTP API 的调用，执行配置来自 {@link ToolDefinition#getExecutionConfig()}：
 * <pre>
 * {
 *   "url":          "https://api.example.com/action",
 *   "method":       "POST",
 *   "headers":      {"X-Api-Key": "..."},
 *   "bodyTemplate": "{\"query\": \"${query}\"}"
 * }
 * </pre>
 *
 * <p>实例由 {@link org.dragon.tool.runtime.factory.HttpToolFactory} 创建，
 * 绑定特定版本的 executionConfig，可安全跨调用复用（无调用级状态）。
 */
@Slf4j
public class HttpTool extends AbstractTool<JsonNode, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int DEFAULT_TIMEOUT_MS = 30_000;

    private final HttpClient httpClient;
    /** 绑定到此实例的版本配置（由 Factory 注入，不可变） */
    private final JsonNode executionConfig;
    private final String toolId;

    /**
     * @param runtime    工具运行时快照（提供 name / description / executionConfig）
     * @param httpClient 共享的 JDK HttpClient（由 Factory 传入）
     */
    public HttpTool(ToolDefinition runtime, HttpClient httpClient) {
        super(runtime.getName(), runtime.getDescription(), JsonNode.class);
        this.executionConfig = runtime.getExecutionConfig();
        this.toolId = runtime.getToolId();
        this.httpClient = httpClient;
    }

    @Override
    protected CompletableFuture<ToolResult<String>> doCall(JsonNode input,
                                                           ToolUseContext context,
                                                           Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            if (executionConfig == null) {
                return ToolResult.fail("HttpTool: executionConfig is null for tool '" + toolId + "'");
            }

            String url = executionConfig.path("url").asText(null);
            String method = executionConfig.path("method").asText("POST").toUpperCase();

            if (url == null || url.isEmpty()) {
                return ToolResult.fail("HttpTool: missing 'url' in executionConfig for tool '" + toolId + "'");
            }

            try {
                String requestBody = buildRequestBody(input);

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofMillis(DEFAULT_TIMEOUT_MS))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json");

                // 追加自定义请求头
                if (executionConfig.has("headers") && executionConfig.path("headers").isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> headers = executionConfig.path("headers").fields();
                    while (headers.hasNext()) {
                        Map.Entry<String, JsonNode> header = headers.next();
                        requestBuilder.header(header.getKey(), header.getValue().asText());
                    }
                }

                switch (method) {
                    case "GET"    -> requestBuilder.GET();
                    case "PUT"    -> requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(requestBody));
                    case "DELETE" -> requestBuilder.DELETE();
                    default       -> requestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody));
                }

                HttpResponse<String> response = httpClient.send(
                        requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

                int statusCode = response.statusCode();
                String responseBody = response.body();

                if (statusCode >= 200 && statusCode < 300) {
                    log.info("[HttpTool] 调用成功: tool={}, url={}, status={}", getName(), url, statusCode);
                    return ToolResult.ok(responseBody);
                } else {
                    return ToolResult.fail("HttpTool: HTTP " + statusCode + " from '" + url + "': " + responseBody);
                }
            } catch (Exception e) {
                return ToolResult.fail("HttpTool: request failed for tool '" + toolId + "': " + e.getMessage());
            }
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(String output, String toolUseId) {
        return ToolResultBlockParam.ofText(toolUseId, output != null ? output : "");
    }

    @Override
    public boolean isReadOnly(JsonNode input) {
        String method = executionConfig != null
                ? executionConfig.path("method").asText("POST").toUpperCase()
                : "POST";
        return "GET".equals(method);
    }

    // ── 内部辅助 ────────────────────────────────────────────────────

    /**
     * 根据 bodyTemplate 或直接序列化 rawParams 构建请求体。
     */
    private String buildRequestBody(JsonNode rawParams) throws Exception {
        if (executionConfig.has("bodyTemplate")) {
            String template = executionConfig.path("bodyTemplate").asText();
            if (rawParams != null && rawParams.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = rawParams.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String placeholder = "${" + field.getKey() + "}";
                    String value = field.getValue().isTextual()
                            ? field.getValue().asText()
                            : field.getValue().toString();
                    template = template.replace(placeholder, value);
                }
            }
            return template;
        }
        return rawParams != null ? MAPPER.writeValueAsString(rawParams) : "{}";
    }
}

