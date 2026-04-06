package org.dragon.agent.react;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.dragon.agent.llm.LLMRequest;
import org.dragon.agent.llm.LLMResponse;
import org.dragon.agent.llm.caller.LLMCaller;
import org.dragon.character.Character;
import org.dragon.config.PromptKeys;
import org.dragon.config.service.ConfigApplication;
import org.dragon.task.Task;
import org.dragon.character.builtin.BuiltInCharacterFactory;
import org.dragon.workspace.service.task.arrangement.dto.PromptWriterInput;
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

    private final LLMCaller llmCaller;
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

    public ReActExecutor(LLMCaller llmCaller,
                         ConfigApplication configApplication,
                         ObjectProvider<BuiltInCharacterFactory> builtInCharacterFactoryProvider,
                         ObjectProvider<CharacterCaller> characterCallerProvider,
                         ThoughtPromptAssembler thoughtPromptAssembler,
                         ActionParser actionParser,
                         ActionExecutor actionExecutor,
                         ObservationEvaluator observationEvaluator,
                         ToolRegistry toolRegistry,
                        SkillRegistry skillRegistry) {
        this.llmCaller = llmCaller;
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
        log.info("[ReAct] Starting ReAct loop for execution: {}", context.getExecutionId());

        // ReAct 循环：Thought -> Action -> Observation
        while (!context.isComplete() && context.incrementIteration() <= context.getMaxIterations()) {
            try {
                log.debug("[ReAct] Iteration {} started", context.getCurrentIteration());

                // Step 1: Thought - 让 LLM 分析并决定下一步行动
                String thought = think(context);

                // Step 2: Action - 根据思考执行动作
                String actionResult = act(context, thought);

                // Step 3: Observation - 观察动作结果，并评估可用性
                ObservationEvaluator.ObservationAssessment assessment = observe(context, actionResult);

                // 检查是否应该结束（结合动作类型与观察结果可用性）
                if (observationEvaluator.shouldFinish(context, assessment)) {
                    context.complete(actionResult);
                    log.info("[ReAct] Execution completed at iteration {}", context.getCurrentIteration());
                    break;
                }

            } catch (Exception e) {
                log.error("[ReAct] Error at iteration: {}", context.getCurrentIteration(), e);
                handleError(context, e);
            }
        }

        // 检查是否达到最大迭代次数
        if (!context.isComplete() && context.getCurrentIteration() >= context.getMaxIterations()) {
            context.complete("达到最大迭代次数");
            log.warn("[ReAct] Max iterations reached: {}", context.getMaxIterations());
        }

        return buildResult(context);
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

        // 根据是否启用流式调用选择不同的方法
        if (context.isStreamingEnabled()) {
            return streamThink(context, request);
        } else {
            return syncThink(context, request);
        }
    }

    /**
     * 同步思考
     */
    private String syncThink(ReActContext context, LLMRequest request) {
        LLMResponse response = llmCaller.call(request);
        String thought = response.getContent();
        context.addThought(thought);
        log.debug("[ReAct] Thought: {}", thought);
        return thought;
    }

    /**
     * 流式思考
     */
    private String streamThink(ReActContext context, LLMRequest request) {
        Stream<LLMResponse> stream = llmCaller.streamCall(request);
        StringBuilder fullContent = new StringBuilder();

        stream.forEach(response -> {
            String chunk = response.getContent();
            if (chunk != null) {
                fullContent.append(chunk);

                // 写入 Task
                Task task = context.getTask();
                if (task != null) {
                    String current = task.getCurrentStreamingContent();
                    task.setCurrentStreamingContent((current != null ? current : "") + chunk);
                }
            }
        });

        String thought = fullContent.toString();
        context.addThought(thought);
        log.debug("[ReAct] Streamed Thought: {}", thought);

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
        // 解析动作
        Action action = actionParser.parse(thought);
        if (action == null) {
            log.warn("[ReAct] Failed to parse action from thought");
            return "无法解析动作";
        }

        context.addAction(action);
        log.debug("[ReAct] Action: {} - {}", action.getType(), action.getToolName());

        // 执行动作
        String modelId = resolveModelId(context, action);
        return actionExecutor.execute(context, action, modelId);
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
        log.debug("[ReAct] Observation: {}", result);
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
