package org.dragon.agent.react;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.dragon.agent.llm.LLMRequest;
import org.dragon.agent.llm.LLMResponse;
import org.dragon.agent.llm.caller.LLMCaller;
import org.dragon.agent.llm.caller.LLMCallerSelector;
import org.dragon.agent.model.ModelInstance;
import org.dragon.agent.model.ModelRegistry;
import org.dragon.character.Character;
import org.dragon.config.PromptKeys;
import org.dragon.config.service.ConfigApplication;
import org.dragon.task.Task;
import org.dragon.character.builtin.BuiltInCharacterFactory;
import org.dragon.workspace.task.dto.PromptWriterInput;
import org.dragon.agent.llm.util.CharacterCaller;
import org.dragon.skill.runtime.SkillRegistry;
import org.dragon.tools.ToolRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

/**
 * ReAct 执行器
 * 实现 ReAct (Reasoning + Acting) 循环框架
 *
 * 流程：Thought -> Action -> Observation -> Thought -> ... -> Finish
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class ReActExecutor {

    private final LLMCallerSelector callerSelector;
    private final ModelRegistry modelRegistry;
    private final Gson gson;
    private final ConfigApplication configApplication;
    private final ObjectProvider<BuiltInCharacterFactory> builtInCharacterFactoryProvider;
    private final ObjectProvider<CharacterCaller> characterCallerProvider;
    private final ThoughtPromptAssembler thoughtPromptAssembler;
    private final ActionParser actionParser;
    private final ActionExecutor actionExecutor;
    private final ObservationEvaluator observationEvaluator;
    private final ToolRegistry toolRegistry;
    private final SkillRegistry skillRegistry;

    public ReActExecutor(LLMCallerSelector callerSelector,
                         ModelRegistry modelRegistry,
                         ConfigApplication configApplication,
                         ObjectProvider<BuiltInCharacterFactory> builtInCharacterFactoryProvider,
                         ObjectProvider<CharacterCaller> characterCallerProvider,
                         ThoughtPromptAssembler thoughtPromptAssembler,
                         ActionParser actionParser,
                         ActionExecutor actionExecutor,
                         ObservationEvaluator observationEvaluator,
                         ToolRegistry toolRegistry,
                        SkillRegistry skillRegistry) {
        this.callerSelector = callerSelector;
        this.modelRegistry = modelRegistry;
        this.configApplication = configApplication;
        this.builtInCharacterFactoryProvider = builtInCharacterFactoryProvider;
        this.characterCallerProvider = characterCallerProvider;
        this.gson = new Gson();
        this.thoughtPromptAssembler = thoughtPromptAssembler;
        this.actionParser = actionParser;
        this.actionExecutor = actionExecutor;
        this.observationEvaluator = observationEvaluator;
        this.toolRegistry = toolRegistry;
        this.skillRegistry = skillRegistry;
    }

    /**
     * 执行 ReAct 循环
     *
     * @param context 执行上下文
     * @return 执行结果
     */
    public ReActResult execute(ReActContext context) {
        String executionId = context.getExecutionId();
        String characterId = context.getCharacterId();
        String userInput = context.getUserInput();

        log.info("[ReAct] ================== ReAct 执行开始 ==================");
        log.info("[ReAct] ExecutionId: {}", executionId);
        log.info("[ReAct] CharacterId: {}", characterId);
        log.info("[ReAct] UserInput: {}", userInput);
        log.info("[ReAct] MaxIterations: {}", context.getMaxIterations());
        log.info("[ReAct] SystemPrompt: {}", context.getSystemPrompt());
        log.info("[ReAct] StreamingEnabled: {}", context.isStreamingEnabled());

        // ReAct 循环：Thought -> Action -> Observation
        while (!context.isComplete() && context.incrementIteration() <= context.getMaxIterations()) {
            int iteration = context.getCurrentIteration();
            try {
                log.info("[ReAct] -------- 第 {}/{} 轮迭代开始 --------", iteration, context.getMaxIterations());

                // Step 1: Thought - 让 LLM 分析并决定下一步行动
                log.info("[ReAct] [{}] Step 1: Think - 发送请求到大模型...", iteration);
                String thought = think(context);
                log.info("[ReAct] [{}] Think 完成，思考结果长度: {} chars", iteration, thought != null ? thought.length() : 0);
                log.info("[ReAct] [{}] Think 结果: {}", iteration, thought);

                // Step 2: Action - 根据思考执行动作
                log.info("[ReAct] [{}] Step 2: Act - 解析并执行动作...", iteration);
                String actionResult = act(context, thought);
                log.info("[ReAct] [{}] Act 完成，动作结果: {}", iteration, actionResult);

                // Step 3: Observation - 观察动作结果，并评估可用性
                log.info("[ReAct] [{}] Step 3: Observe - 评估观察结果...", iteration);
                ObservationEvaluator.ObservationAssessment assessment = observe(context, actionResult);
                log.info("[ReAct] [{}] Observation 评估: available={}, isError={}, reason={}, shouldFinish={}",
                        iteration, assessment.isAvailable(), assessment.isError(), assessment.getReason(), observationEvaluator.shouldFinish(context, assessment));

                // 检查是否应该结束（结合动作类型与观察结果可用性）
                if (observationEvaluator.shouldFinish(context, assessment)) {
                    context.complete(actionResult);
                    log.info("[ReAct] [{}] ========== 执行完成 ==========", iteration);
                    log.info("[ReAct] 最终响应: {}", actionResult);
                    break;
                }

                log.info("[ReAct] -------- 第 {} 轮迭代结束 --------", iteration);

            } catch (Exception e) {
                log.error("[ReAct] [{}] 迭代执行出错: {}", iteration, e.getMessage(), e);
                handleError(context, e);
            }
        }

        // 检查是否达到最大迭代次数
        if (!context.isComplete() && context.getCurrentIteration() >= context.getMaxIterations()) {
            context.complete("达到最大迭代次数");
            log.warn("[ReAct] 达到最大迭代次数: {}", context.getMaxIterations());
        }

        ReActResult result = buildResult(context);
        log.info("[ReAct] ========== ReAct 执行结束 ==========");
        log.info("[ReAct] Success: {}, Iterations: {}, FinalResponse: {}",
                result.isSuccess(), result.getIterations(), result.getResponse());

        return result;
    }

    // ==================== ReAct 步骤方法 ====================

    /**
     * Step 1: Thought
     * 让 LLM 分析问题，决定下一步行动
     *
     * @param context 执行上下文
     * @return LLM 的思考结果
     */
    private String think(ReActContext context) {
        String modelId = resolveModelId(context);
        String prompt = buildThoughtPrompt(context);

        log.info("[ReAct] [{}] 构建 LLM Request - modelId: {}", context.getCurrentIteration(), modelId);
        log.info("[ReAct] [{}] SystemPrompt: {}", context.getCurrentIteration(), context.getSystemPrompt());
        log.info("[ReAct] [{}] UserPrompt: {}", context.getCurrentIteration(), prompt);

        LLMRequest request = LLMRequest.builder()
                .modelId(modelId)
                .messages(java.util.Collections.singletonList(
                        LLMRequest.LLMMessage.builder()
                                .role(LLMRequest.LLMMessage.Role.USER)
                                .content(prompt)
                                .build()
                ))
                .systemPrompt(context.getSystemPrompt())
                .tools(toolRegistry.toDefinitions())
                .build();

        log.info("[ReAct] [{}] 工具数量: {}", context.getCurrentIteration(),
                request.getTools() != null ? request.getTools().size() : 0);

        // 根据是否启用流式调用选择不同的方法
        if (context.isStreamingEnabled()) {
            log.info("[ReAct] [{}] 使用流式调用", context.getCurrentIteration());
            return streamThink(context, request);
        } else {
            log.info("[ReAct] [{}] 使用同步调用", context.getCurrentIteration());
            return syncThink(context, request);
        }
    }

    /**
     * 同步思考
     */
    private String syncThink(ReActContext context, LLMRequest request) {
        int iteration = context.getCurrentIteration();
        String modelId = request.getModelId();

        // 解析 Caller
        LLMCaller caller = resolveCaller(context, modelId);
        log.info("[ReAct] [{}] ========== 同步 LLM 调用开始 ==========", iteration);
        log.info("[ReAct] [{}] Caller: {}", iteration, caller.getClass().getSimpleName());
        log.info("[ReAct] [{}] ModelId: {}", iteration, modelId);

        // 打印完整请求
        log.info("[ReAct] [{}] ========== LLM Request ==========", iteration);
        log.info("[ReAct] [{}] modelId: {}", iteration, request.getModelId());
        log.info("[ReAct] [{}] systemPrompt: {}", iteration, request.getSystemPrompt());
        log.info("[ReAct] [{}] messages: {}", iteration, request.getMessages());
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            log.info("[ReAct] [{}] tools (前3个): {}", iteration, request.getTools().stream().limit(3).toList());
        }
        log.info("[ReAct] [{}] temperature: {}", iteration, request.getTemperature());
        log.info("[ReAct] [{}] maxTokens: {}", iteration, request.getMaxTokens());

        // 调用 LLM
        long startTime = System.currentTimeMillis();
        LLMResponse response = caller.call(request);
        long duration = System.currentTimeMillis() - startTime;

        log.info("[ReAct] [{}] ========== LLM Response ==========", iteration);
        log.info("[ReAct] [{}] 耗时: {}ms", iteration, duration);
        log.info("[ReAct] [{}] finishReason: {}", iteration, response.getFinishReason());
        log.info("[ReAct] [{}] content (完整): {}", iteration, response.getContent());
        log.info("[ReAct] [{}] functionCall: {}", iteration, response.getFunctionCall());

        if (response.getUsage() != null) {
            log.info("[ReAct] [{}] usage: promptTokens={}, completionTokens={}, totalTokens={}",
                    iteration,
                    response.getUsage().getPromptTokens(),
                    response.getUsage().getCompletionTokens(),
                    response.getUsage().getTotalTokens());
        }

        String thought = response.getContent();
        // 如果 content 为空但有 functionCall，说明是 tool_calls 模式
        if ((thought == null || thought.isEmpty()) && response.getFunctionCall() != null) {
            LLMResponse.FunctionCall fc = response.getFunctionCall();
            // 转换为 ActionParser 可解析的 JSON 格式
            thought = String.format(
                    "{\"action\": \"TOOL\", \"tool\": \"%s\", \"params\": %s}",
                    fc.getName(),
                    fc.getArguments()
            );
            log.info("[ReAct] [{}] 从 functionCall 构建 thought: {}", iteration, thought);
        }
        if (thought == null || thought.isEmpty()) {
            log.warn("[ReAct] [{}] LLM 返回内容为空!", iteration);
        }

        context.addThought(thought);
        log.info("[ReAct] [{}] ========== 同步 LLM 调用结束 ==========", iteration);

        return thought;
    }

    /**
     * 流式思考
     */
    private String streamThink(ReActContext context, LLMRequest request) {
        int iteration = context.getCurrentIteration();
        String modelId = request.getModelId();

        // 解析 Caller
        LLMCaller caller = resolveCaller(context, modelId);
        log.info("[ReAct] [{}] ========== 流式 LLM 调用开始 ==========", iteration);
        log.info("[ReAct] [{}] Caller: {}", iteration, caller.getClass().getSimpleName());
        log.info("[ReAct] [{}] ModelId: {}", iteration, modelId);

        Stream<LLMResponse> stream = caller.streamCall(request);
        StringBuilder fullContent = new StringBuilder();

        long startTime = System.currentTimeMillis();
        AtomicInteger chunkCount = new AtomicInteger(0);
        int currentIteration = iteration;  // effectively final for lambda

        stream.forEach(response -> {
            String chunk = response.getContent();
            if (chunk != null) {
                int count = chunkCount.incrementAndGet();
                fullContent.append(chunk);

                // 每10个chunk打印一次进度
                if (count % 10 == 0) {
                    log.info("[ReAct] [{}] 流式接收 chunk #{}, 当前累计长度: {}",
                            currentIteration, count, fullContent.length());
                }

                // 写入 Task
                Task task = context.getTask();
                if (task != null) {
                    String current = task.getCurrentStreamingContent();
                    task.setCurrentStreamingContent((current != null ? current : "") + chunk);
                }
            }
        });

        long duration = System.currentTimeMillis() - startTime;

        String thought = fullContent.toString();
        log.info("[ReAct] [{}] ========== 流式 LLM 调用结束 ==========", iteration);
        log.info("[ReAct] [{}] 总 chunks: {}", iteration, chunkCount.get());
        log.info("[ReAct] [{}] 耗时: {}ms", iteration, duration);
        log.info("[ReAct] [{}] 最终内容长度: {} chars", iteration, thought.length());
        log.info("[ReAct] [{}] 最终内容: {}", iteration, thought);

        context.addThought(thought);

        return thought;
    }

    /**
     * Step 2: Action
     * 解析思考结果，执行相应的动作
     *
     * @param context 执行上下文
     * @param thought 思考结果
     * @return 动作执行结果
     */
    private String act(ReActContext context, String thought) {
        int iteration = context.getCurrentIteration();

        // 解析动作
        log.info("[ReAct] [{}] ========== 动作解析开始 ==========", iteration);
        log.info("[ReAct] [{}] 待解析的 Thought: {}", iteration, thought);

        Action action = actionParser.parse(thought);
        if (action == null) {
            log.warn("[ReAct] [{}] 无法从 Thought 解析出动作!", iteration);
            return "无法解析动作";
        }

        context.addAction(action);
        log.info("[ReAct] [{}] 动作解析成功:", iteration);
        log.info("[ReAct] [{}]   ActionType: {}", iteration, action.getType());
        log.info("[ReAct] [{}]   ToolName: {}", iteration, action.getToolName());
        log.info("[ReAct] [{}]   Params: {}", iteration, action.getParameters());
        log.info("[ReAct] [{}] ========== 动作解析结束 ==========", iteration);

        // 执行动作
        String modelId = resolveModelId(context, action);
        log.info("[ReAct] [{}] 执行动作, modelId: {}", iteration, modelId);

        long startTime = System.currentTimeMillis();
        String result = actionExecutor.execute(context, action, modelId);
        long duration = System.currentTimeMillis() - startTime;

        log.info("[ReAct] [{}] 动作执行完成, 耗时: {}ms", iteration, duration);
        log.info("[ReAct] [{}] 执行结果: {}", iteration, result);

        return result;
    }

    /**
     * Step 3: Observation
     * 记录动作执行结果到上下文，并评估结果可用性
     *
     * @param context 执行上下文
     * @param result 动作执行结果
     * @return 观察结果可用性评估
     */
    private ObservationEvaluator.ObservationAssessment observe(ReActContext context, String result) {
        context.addObservation(result);
        log.info("[ReAct] [{}] Observation 记录: {}", context.getCurrentIteration(), result);
        return observationEvaluator.assess(result);
    }

    // ==================== 辅助方法 ====================

    /**
     * 处理错误
     *
     * @param context 执行上下文
     * @param e 异常
     */
    private void handleError(ReActContext context, Exception e) {
        String errorMsg = "Error: " + e.getMessage();
        context.addObservation(errorMsg);

        if (context.getCurrentIteration() >= context.getMaxIterations()) {
            context.complete("执行达到最大迭代次数");
        }
    }

    /**
     * 构建思考阶段的 Prompt
     * 优先使用 PromptWriter 动态装配，回退到 ThoughtPromptAssembler
     *
     * @param context 上下文
     * @return Prompt
     */
    private String buildThoughtPrompt(ReActContext context) {
        // 优先尝试动态装配
        String dynamicPrompt = tryBuildDynamicThoughtPrompt(context);
        if (dynamicPrompt != null) {
            return dynamicPrompt;
        }
        // 回退到 ThoughtPromptAssembler 拼装
        return thoughtPromptAssembler.assemble(context);
    }

    /**
     * 尝试通过 PromptWriter 动态装配 Prompt
     */
    private String tryBuildDynamicThoughtPrompt(ReActContext context) {
        if (configApplication == null) {
            return null;
        }

        BuiltInCharacterFactory builtInCharacterFactory = builtInCharacterFactoryProvider.getIfAvailable();
        CharacterCaller characterCaller = characterCallerProvider.getIfAvailable();
        if (builtInCharacterFactory == null || characterCaller == null) {
            return null;
        }

        try {
            String workspaceId = resolveWorkspaceId(context);
            if (workspaceId == null) {
                return null;
            }

            // 获取 PromptWriter Character
            Character promptWriterChar = builtInCharacterFactory.getOrCreatePromptWriterCharacter(workspaceId);
            if (promptWriterChar == null) {
                return null;
            }

            // 获取模板
            String template = configApplication.getPrompt(workspaceId, context.getCharacterId(), PromptKeys.REACT_EXECUTE);
            if (template == null || template.isEmpty()) {
                return null;
            }

            // 组装 PromptWriter 输入
            PromptWriterInput input = buildReActPromptWriterInput(context, template);
            String inputJson = gson.toJson(input);

            // 调用 PromptWriter Character 生成最终 Prompt
            return characterCaller.call(promptWriterChar, inputJson);

        } catch (Exception e) {
            log.warn("[ReAct] PromptWriter dynamic prompt failed, falling back to assembler: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析工作空间 ID
     */
    private String resolveWorkspaceId(ReActContext context) {
        // 优先从 context.workspaceId 获取
        if (context.getWorkspaceId() != null) {
            return context.getWorkspaceId();
        }
        // 回退到从 Task 获取
        Task task = context.getTask();
        if (task != null && task.getWorkspaceId() != null) {
            return task.getWorkspaceId();
        }
        // 从 Character.workspaceIds 获取（需要在调用前设置）
        return null;
    }

    /**
     * 构建 ReAct 阶段的 PromptWriter 输入
     */
    private PromptWriterInput buildReActPromptWriterInput(ReActContext context, String template) {
        List<PromptWriterInput.MemberInfo> memberInfos = new ArrayList<>();

        Map<String, Object> contextHints = Map.of(
                "timestamp", java.time.LocalDateTime.now().toString(),
                "allowFollowUp", false,
                "executionId", context.getExecutionId()
        );

        PromptWriterInput.TaskInfo taskInfo = PromptWriterInput.TaskInfo.builder()
                .id(context.getTask() != null ? context.getTask().getId() : null)
                .name(null)
                .description(null)
                .input(context.getUserInput())
                .parentTaskId(null)
                .build();

        return PromptWriterInput.builder()
                .workspaceId(resolveWorkspaceId(context))
                .promptType("react_execute")
                .promptTemplate(template)
                .task(taskInfo)
                .members(memberInfos)
                .contextHints(contextHints)
                .build();
    }

    /**
     * 解析模型 ID
     *
     * @param context 上下文
     * @return 模型 ID
     */
    private String resolveModelId(ReActContext context) {
        String modelId = context.getCurrentModelId();

        // 检查是否需要切换模型
        if (context.hasModelSwitch()) {
            modelId = context.getNextModelId();
        }

        return modelId != null ? modelId : context.getDefaultModelId();
    }

    /**
     * 解析模型 ID（考虑动作指定的模型）
     *
     * @param context 上下文
     * @param action 动作
     * @return 模型 ID
     */
    private String resolveModelId(ReActContext context, Action action) {
        if (action.getModelId() != null) {
            return action.getModelId();
        }
        return resolveModelId(context);
    }

    /**
     * 根据模型 ID 解析对应的 LLMCaller
     *
     * @param context 上下文
     * @param modelId 模型 ID
     * @return 对应的 LLMCaller
     */
    private LLMCaller resolveCaller(ReActContext context, String modelId) {
        if (modelId == null) {
            return callerSelector.getDefault();
        }

        ModelInstance model = modelRegistry.get(modelId).orElse(null);
        if (model == null) {
            log.warn("[ReAct] Model not found: {}, using default caller", modelId);
            return callerSelector.getDefault();
        }

        LLMCaller caller = callerSelector.select(model);
        log.info("[ReAct] [{}] 模型解析: modelId={}, provider={}, selectedCaller={}",
                context != null ? context.getCurrentIteration() : 0,
                modelId, model.getProvider(), caller.getClass().getSimpleName());
        return caller;
    }

    /**
     * 构建执行结果
     *
     * @param context 上下文
     * @return 执行结果
     */
    private ReActResult buildResult(ReActContext context) {
        // 提取最后一个动作的类型和状态变更信息
        Action.StatusChange statusChange = null;
        Action.ActionType finalActionType = null;

        if (!context.getActions().isEmpty()) {
            Action lastAction = context.getActions().get(context.getActions().size() - 1);
            finalActionType = lastAction.getType();
            if (lastAction.getType() == Action.ActionType.STATUS_CHANGE) {
                statusChange = lastAction.getStatusChange();
            }
        }

        return ReActResult.builder()
                .executionId(context.getExecutionId())
                .success(context.isComplete())
                .response(context.getFinalResponse())
                .iterations(context.getCurrentIteration())
                .thoughts(context.getThoughts())
                .actions(context.getActions())
                .observations(context.getObservations())
                .errorMessage(context.getErrorMessage())
                .statusChange(statusChange)
                .finalActionType(finalActionType)
                .build();
    }
}
