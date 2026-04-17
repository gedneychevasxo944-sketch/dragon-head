package org.dragon.step.character;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.dragon.agent.llm.LLMRequest;
import org.dragon.agent.llm.LLMResponse;
import org.dragon.agent.llm.caller.LLMCaller;
import org.dragon.agent.model.ModelInstance;
import org.dragon.agent.model.ModelRegistry;
import org.dragon.agent.llm.caller.LLMCallerSelector;
import org.dragon.agent.react.ReActContext;
import org.dragon.step.OutputSchema;
import org.dragon.step.StepResult;
import org.dragon.tools.ToolRegistry;

import lombok.extern.slf4j.Slf4j;

/**
 * ThinkStep - 调用 LLM 生成思考
 *
 * <p>ReAct 循环的核心步骤。根据 BuildPromptStep 组装的 Prompt，
 * 调用 LLM 获取思考结果（Thought）。
 *
 * <p>Thought 通常包含：
 * <ul>
 *   <li>对当前问题的分析</li>
 *   <li>决定要执行的 Action（如调用某个工具）</li>
 * </ul>
 *
 * <p>支持流式输出（streaming），会将中间输出写入 Task 用于实时展示。
 *
 * @author yijunw
 */
@Slf4j
public class ThinkStep extends CharacterStep {

    private final LLMCallerSelector callerSelector;
    private final ModelRegistry modelRegistry;
    private final ToolRegistry toolRegistry;

    public ThinkStep(LLMCallerSelector callerSelector, ModelRegistry modelRegistry, ToolRegistry toolRegistry) {
        super("think");
        this.callerSelector = callerSelector;
        this.modelRegistry = modelRegistry;
        this.toolRegistry = toolRegistry;
    }

    public ThinkStep() {
        super("think");
        this.callerSelector = null;
        this.modelRegistry = null;
        this.toolRegistry = null;
    }

    @Override
    protected StepResult doExecute(ReActContext ctx) throws Exception {
        String modelId = resolveModelId(ctx);
        LLMCaller caller = resolveCaller(ctx, modelId);

        // 优先使用 BuildPromptStep 构建的 prompt，回退到 userInput
        String prompt = BuildPromptStep.getBuiltPrompt(ctx);
        if (prompt == null) {
            prompt = buildPrompt(ctx);
        }

        LLMRequest request = LLMRequest.builder()
                .modelId(modelId)
                .messages(Collections.singletonList(
                        LLMRequest.LLMMessage.builder()
                                .role(LLMRequest.LLMMessage.Role.USER)
                                .content(prompt)
                                .build()
                ))
                .systemPrompt(ctx.getSystemPrompt())
                .tools(toolRegistry != null ? toolRegistry.toDefinitions() : null)
                .build();

        long startTime = System.currentTimeMillis();
        LLMResponse response;

        if (ctx.isStreamingEnabled()) {
            response = streamCall(caller, request, ctx);
        } else {
            response = syncCall(caller, request);
        }

        String thought = response.getContent();
        // 如果 content 为空但有 functionCall，转为 JSON 格式
        if ((thought == null || thought.isEmpty()) && response.getFunctionCall() != null) {
            LLMResponse.FunctionCall fc = response.getFunctionCall();
            thought = String.format(
                    "{\"action\": \"TOOL\", \"tool\": \"%s\", \"params\": %s}",
                    fc.getName(),
                    fc.getArguments()
            );
        }

        ctx.addThought(thought);

        return StepResult.builder()
                .stepName(getName())
                .output(thought)
                .success(true)
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
    }

    private String resolveModelId(ReActContext ctx) {
        String modelId = ctx.getCurrentModelId();
        if (ctx.hasModelSwitch()) {
            modelId = ctx.getNextModelId();
        }
        return modelId != null ? modelId : ctx.getDefaultModelId();
    }

    private LLMCaller resolveCaller(ReActContext ctx, String modelId) {
        if (modelId == null || callerSelector == null) {
            return callerSelector != null ? callerSelector.getDefault() : null;
        }
        ModelInstance model = modelRegistry.get(modelId).orElse(null);
        if (model == null) {
            return callerSelector.getDefault();
        }
        return callerSelector.select(model);
    }

    private String buildPrompt(ReActContext ctx) {
        // 使用 context 中已组装好的 userInput 作为 prompt
        return ctx.getUserInput();
    }

    // ==================== OutputSchema stub（后续优化用） ====================

    /**
     * 通过 OutputSchema 约束 LLM 输出格式（后续优化点）
     *
     * <p>当前为 stub，正式使用时：
     * <ul>
     *   <li>将 schema 注入到 LLMRequest 的 responseFormat 参数</li>
     *   <li>支持 forStepRouting()、forMemberSelection() 等不同场景</li>
     * </ul>
     */
    protected LLMRequest buildSchemaConstrainedRequest(LLMRequest request, OutputSchema schema) {
        // TODO: 实现 OutputSchema 约束
        // 后续可在 request 中设置 responseFormat = schema.getJsonSchema()
        // 示例：request.setResponseFormat(schema.getJsonSchema());
        return request;
    }

    private LLMResponse syncCall(LLMCaller caller, LLMRequest request) {
        return caller.call(request);
    }

    private LLMResponse streamCall(LLMCaller caller, LLMRequest request, ReActContext ctx) {
        StringBuilder fullContent = new StringBuilder();
        AtomicInteger chunkCount = new AtomicInteger(0);

        caller.streamCall(request).forEach(response -> {
            String chunk = response.getContent();
            if (chunk != null) {
                fullContent.append(chunk);
                chunkCount.incrementAndGet();

                // 写入 Task 用于流式输出
                if (ctx.getTask() != null) {
                    String current = ctx.getTask().getCurrentStreamingContent();
                    ctx.getTask().setCurrentStreamingContent((current != null ? current : "") + chunk);
                }
            }
        });

        return LLMResponse.builder()
                .content(fullContent.toString())
                .finishReason("stop")
                .build();
    }
}