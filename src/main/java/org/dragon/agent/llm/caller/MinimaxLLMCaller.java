package org.dragon.agent.llm.caller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.dragon.agent.llm.LLMRequest;
import org.dragon.agent.llm.LLMResponse;
import org.dragon.config.context.InheritanceContext;
import org.dragon.config.service.ConfigApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Minimax LLM 调用器实现
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component("minimaxLLMCaller")
public class MinimaxLLMCaller implements LLMCaller {

    private final HttpClient httpClient;
    private final Gson gson;
    private final String apiKey;
    private final String baseUrl;
    private final String defaultModel;
    private final String groupId;

    @Autowired
    public MinimaxLLMCaller(ConfigApplication configApplication) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.gson = new Gson();
        InheritanceContext ctx = InheritanceContext.forGlobal();
        this.apiKey = configApplication.getStringValue("llm.minimax.apiKey", ctx, "sk-cp-oOukgyncEXgiR7-dYG0QR3nC_CP3Li9iUgHa4otrj_HSIgTf0tEKClsx869mqDRW-DttngbtQ3Q8kSzMg3Opou6OVPnUeg1Y0iAcAByGMABmHFfgRciDe24");
        this.baseUrl = configApplication.getStringValue("llm.minimax.baseUrl", ctx, "https://api.minimax.chat");
        this.defaultModel = configApplication.getStringValue("llm.minimax.model", ctx, "MiniMax-M2.7");
        this.groupId = configApplication.getStringValue("llm.minimax.groupId", ctx, "");
    }

    @Override
    public LLMResponse call(LLMRequest request) {
        return call(defaultModel, request);
    }

    @Override
    public CompletableFuture<LLMResponse> callAsync(LLMRequest request) {
        return callAsync(defaultModel, request);
    }

    @Override
    public LLMResponse call(String modelId, LLMRequest request) {
        try {
            String url = buildUrl(modelId);
            String body = buildRequestBody(modelId, request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            return parseResponse(response.body());

        } catch (Exception e) {
            log.error("[Minimax] 调用失败: {}", e.getMessage(), e);
            return buildErrorResponse(e.getMessage());
        }
    }

    @Override
    public CompletableFuture<LLMResponse> callAsync(String modelId, LLMRequest request) {
        return CompletableFuture.supplyAsync(() -> call(modelId, request));
    }

    @Override
    public Stream<LLMResponse> streamCall(LLMRequest request) {
        return streamCall(defaultModel, request);
    }

    @Override
    public Stream<LLMResponse> streamCall(String modelId, LLMRequest request) {
        // 简化实现：返回单个非流式响应
        // 完整流式实现需要 SSE 处理
        return Stream.of(call(modelId, request));
    }

    private String buildUrl(String modelId) {
        if (groupId != null && !groupId.isEmpty()) {
            return baseUrl + "/v1/text/chatcompletion_v2?GroupId=" + groupId;
        }
        return baseUrl + "/v1/text/chatcompletion_v2";
    }

    private String buildRequestBody(String modelId, LLMRequest request) {
        JsonObject body = new JsonObject();
        body.addProperty("model", modelId != null ? modelId : defaultModel);
        body.addProperty("stream", request.isStream());

        // 构建消息
        JsonArray messages = new JsonArray();

        // 系统提示词
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", request.getSystemPrompt());
            messages.add(systemMsg);
        }

        // 消息列表
        if (request.getMessages() != null) {
            for (LLMRequest.LLMMessage msg : request.getMessages()) {
                JsonObject msgObj = new JsonObject();
                msgObj.addProperty("role", msg.getRole().name().toLowerCase());
                msgObj.addProperty("content", msg.getContent());
                if (msg.getName() != null) {
                    msgObj.addProperty("name", msg.getName());
                }
                messages.add(msgObj);
            }
        }

        body.add("messages", messages);

        // 工具列表（用于 tool_call 模式）
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            JsonArray toolsArray = new JsonArray();
            for (Map<String, Object> tool : request.getTools()) {
                JsonObject toolObj = new JsonObject();
                toolObj.addProperty("type", "function");
                JsonObject functionObj = new JsonObject();
                functionObj.addProperty("name", String.valueOf(tool.get("name")));
                functionObj.addProperty("description", String.valueOf(tool.get("description")));
                functionObj.add("parameters", gson.toJsonTree(tool.get("input_schema")));
                toolObj.add("function", functionObj);
                toolsArray.add(toolObj);
            }
            body.add("tools", toolsArray);
        }

        // 可选参数
        if (request.getTemperature() != null) {
            body.addProperty("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            body.addProperty("max_tokens", request.getMaxTokens());
        }

        // extraParams
        if (request.getExtraParams() != null) {
            for (Map.Entry<String, Object> entry : request.getExtraParams().entrySet()) {
                body.addProperty(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        return body.toString();
    }

    private LLMResponse parseResponse(String responseBody) {
        try {
            log.info("[Minimax] parseResponse: responseBody={}", responseBody);
            JsonElement jsonElement = JsonParser.parseString(responseBody);
            if (!jsonElement.isJsonObject()) {
                log.error("[Minimax] 响应不是 JSON 对象: {}", responseBody);
                return buildErrorResponse("响应格式错误: " + responseBody);
            }

            JsonObject json = jsonElement.getAsJsonObject();

            // 检查错误
            if (json.has("base_resp")) {
                JsonObject baseResp = json.getAsJsonObject("base_resp");
                int statusCode = baseResp.get("status_code").getAsInt();
                if (statusCode != 0) {
                    String errorMsg = baseResp.get("status_msg").getAsString();
                    log.error("[Minimax] API 错误: {}", errorMsg);
                    return buildErrorResponse(errorMsg);
                }
            }

            JsonObject choice = json.getAsJsonArray("choices").get(0).getAsJsonObject();
            JsonObject message = choice.getAsJsonObject("message");

            String finishReason = choice.has("finish_reason")
                    ? choice.get("finish_reason").getAsString()
                    : "stop";

            String content = null;
            LLMResponse.FunctionCall functionCall = null;

            // 处理 tool_calls 模式
            if (message.has("tool_calls")) {
                JsonArray toolCalls = message.getAsJsonArray("tool_calls");
                if (toolCalls != null && toolCalls.size() > 0) {
                    JsonObject toolCall = toolCalls.get(0).getAsJsonObject();
                    String funcName = toolCall.getAsJsonObject("function").get("name").getAsString();
                    String arguments = toolCall.getAsJsonObject("function").get("arguments").getAsString();
                    functionCall = LLMResponse.FunctionCall.builder()
                            .name(funcName)
                            .arguments(arguments)
                            .build();
                    log.info("[Minimax] tool_call detected: name={}, arguments={}", funcName, arguments);
                }
            } else if (message.has("content")) {
                // 处理普通文本模式
                content = message.get("content").isJsonNull() ? null : message.get("content").getAsString();
            }

            log.info("[Minimax] parseResponse: finishReason={}, content={}, functionCall={}", finishReason, content, functionCall);

            // 用量统计
            LLMResponse.Usage usage = null;
            if (json.has("usage")) {
                JsonObject usageJson = json.getAsJsonObject("usage");
                usage = LLMResponse.Usage.builder()
                        .promptTokens(usageJson.has("prompt_tokens")
                                ? usageJson.get("prompt_tokens").getAsInt() : 0)
                        .completionTokens(usageJson.has("completion_tokens")
                                ? usageJson.get("completion_tokens").getAsInt() : 0)
                        .totalTokens(usageJson.has("total_tokens")
                                ? usageJson.get("total_tokens").getAsInt() : 0)
                        .build();
            }

            return LLMResponse.builder()
                    .content(content)
                    .functionCall(functionCall)
                    .finishReason(finishReason)
                    .usage(usage)
                    .lastChunk(true)
                    .build();

        } catch (Exception e) {
            log.error("[Minimax] 解析响应失败: {}", e.getMessage(), e);
            return buildErrorResponse("解析响应失败: " + e.getMessage());
        }
    }

    private LLMResponse buildErrorResponse(String errorMessage) {
        return LLMResponse.builder()
                .content("")
                .finishReason("error")
                .build();
    }
}
