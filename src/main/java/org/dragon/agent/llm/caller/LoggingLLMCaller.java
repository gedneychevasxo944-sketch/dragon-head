package org.dragon.agent.llm.caller;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.dragon.agent.llm.LLMRequest;
import org.dragon.agent.llm.LLMResponse;
import org.dragon.observer.actionlog.ObserverActionLogService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * LLM 调用日志装饰器
 * 包装 LLMCaller，在调用时自动上报调用信息到 Observer
 *
 * @author wyj
 * @version 1.0
 */
@Component
@Primary
public class LoggingLLMCaller implements LLMCaller {

    private final LLMCaller delegate;
    private final ObserverActionLogService actionLogService;

    public LoggingLLMCaller(LLMCaller delegate, ObserverActionLogService actionLogService) {
        this.delegate = delegate;
        this.actionLogService = actionLogService;
    }

    @Override
    public LLMResponse call(LLMRequest request) {
        long startTime = System.currentTimeMillis();
        String input = buildInputSummary(request);
        String modelId = request != null ? request.getModelId() : "unknown";

        // 上报调用开始
        actionLogService.logLLMCallStart("SYSTEM", "llm", modelId, input);

        try {
            LLMResponse response = delegate.call(request);
            long latency = System.currentTimeMillis() - startTime;

            // 上报调用完成
            actionLogService.logLLMCall("SYSTEM", "llm", modelId,
                    input, response.getContent(),
                    getInputTokens(response), getOutputTokens(response),
                    latency, isSuccess(response));

            return response;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;

            // 上报调用失败
            actionLogService.logLLMCall("SYSTEM", "llm", modelId,
                    input, e.getMessage(), 0, 0, latency, false);

            throw e;
        }
    }

    @Override
    public CompletableFuture<LLMResponse> callAsync(LLMRequest request) {
        long startTime = System.currentTimeMillis();
        String input = buildInputSummary(request);
        String modelId = request != null ? request.getModelId() : "unknown";

        actionLogService.logLLMCallStart("SYSTEM", "llm", modelId, input);

        return delegate.callAsync(request).whenComplete((response, error) -> {
            long latency = System.currentTimeMillis() - startTime;
            if (error != null) {
                actionLogService.logLLMCall("SYSTEM", "llm", modelId,
                        input, error.getMessage(), 0, 0, latency, false);
            } else {
                actionLogService.logLLMCall("SYSTEM", "llm", modelId,
                        input, response.getContent(),
                        getInputTokens(response), getOutputTokens(response),
                        latency, isSuccess(response));
            }
        });
    }

    @Override
    public LLMResponse call(String modelId, LLMRequest request) {
        long startTime = System.currentTimeMillis();
        String input = buildInputSummary(request);

        actionLogService.logLLMCallStart("SYSTEM", "llm", modelId, input);

        try {
            LLMResponse response = delegate.call(modelId, request);
            long latency = System.currentTimeMillis() - startTime;

            actionLogService.logLLMCall("SYSTEM", "llm", modelId,
                    input, response.getContent(),
                    getInputTokens(response), getOutputTokens(response),
                    latency, isSuccess(response));

            return response;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;

            actionLogService.logLLMCall("SYSTEM", "llm", modelId,
                    input, e.getMessage(), 0, 0, latency, false);

            throw e;
        }
    }

    @Override
    public CompletableFuture<LLMResponse> callAsync(String modelId, LLMRequest request) {
        long startTime = System.currentTimeMillis();
        String input = buildInputSummary(request);

        actionLogService.logLLMCallStart("SYSTEM", "llm", modelId, input);

        return delegate.callAsync(modelId, request).whenComplete((response, error) -> {
            long latency = System.currentTimeMillis() - startTime;
            if (error != null) {
                actionLogService.logLLMCall("SYSTEM", "llm", modelId,
                        input, error.getMessage(), 0, 0, latency, false);
            } else {
                actionLogService.logLLMCall("SYSTEM", "llm", modelId,
                        input, response.getContent(),
                        getInputTokens(response), getOutputTokens(response),
                        latency, isSuccess(response));
            }
        });
    }

    @Override
    public Stream<LLMResponse> streamCall(LLMRequest request) {
        // 流式调用单独处理，暂不自动上报
        return delegate.streamCall(request);
    }

    @Override
    public Stream<LLMResponse> streamCall(String modelId, LLMRequest request) {
        // 流式调用单独处理，暂不自动上报
        return delegate.streamCall(modelId, request);
    }

    private String buildInputSummary(LLMRequest request) {
        if (request == null || request.getMessages() == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (var message : request.getMessages()) {
            sb.append(message.getRole()).append(": ").append(message.getContent()).append("\n");
        }
        return sb.toString();
    }

    private int getInputTokens(LLMResponse response) {
        if (response == null || response.getUsage() == null) {
            return 0;
        }
        return response.getUsage().getPromptTokens();
    }

    private int getOutputTokens(LLMResponse response) {
        if (response == null || response.getUsage() == null) {
            return 0;
        }
        return response.getUsage().getCompletionTokens();
    }

    private boolean isSuccess(LLMResponse response) {
        if (response == null) {
            return false;
        }
        // 根据 finishReason 判断是否成功
        String finishReason = response.getFinishReason();
        return "stop".equalsIgnoreCase(finishReason) || "length".equalsIgnoreCase(finishReason);
    }
}
