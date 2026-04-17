package org.dragon.step.character;

import org.dragon.agent.react.Action;
import org.dragon.agent.react.ActionExecutor;
import org.dragon.agent.react.ActionParser;
import org.dragon.agent.react.ReActContext;
import org.dragon.step.StepResult;

import lombok.extern.slf4j.Slf4j;

/**
 * ActStep - 执行 Action
 *
 * <p>从 ThinkStep 的输出解析 Action 并执行。
 *
 * <p>Action 类型包括：
 * <ul>
 *   <li>TOOL - 调用工具（如查天气、发邮件）</li>
 *   <li>CHAT - 生成回复文本</li>
 *   <li>STATUS_CHANGE - 状态变更（如标记完成）</li>
 * </ul>
 *
 * <p>执行结果（Observation）会存入上下文，供 ObserveStep 评估。
 *
 * @author yijunw
 */
@Slf4j
public class ActStep extends CharacterStep {

    private final ActionParser actionParser;
    private final ActionExecutor actionExecutor;

    public ActStep(ActionParser actionParser, ActionExecutor actionExecutor) {
        super("act");
        this.actionParser = actionParser;
        this.actionExecutor = actionExecutor;
    }

    public ActStep() {
        super("act");
        this.actionParser = null;
        this.actionExecutor = null;
    }

    @Override
    protected StepResult doExecute(ReActContext ctx) throws Exception {
        String thought = getLatestThought(ctx);
        if (thought == null || thought.isEmpty()) {
            return StepResult.failure(getName(), "no_thought_available");
        }

        // 解析动作
        Action action = actionParser.parse(thought);
        if (action == null) {
            return StepResult.failure(getName(), "cannot_parse_action");
        }

        ctx.addAction(action);

        // 解析模型
        String modelId = resolveModelId(ctx, action);

        // 执行动作
        long startTime = System.currentTimeMillis();
        String result = actionExecutor.execute(ctx, action, modelId);

        // 记录结果到 observations，供 ObserveStep 使用
        ctx.addObservation(result);

        return StepResult.builder()
                .stepName(getName())
                .output(result)
                .success(true)
                .durationMs(System.currentTimeMillis() - startTime)
                .metadata(java.util.Map.of(
                        "actionType", action.getType().name(),
                        "toolName", action.getToolName() != null ? action.getToolName() : "none"
                ))
                .build();
    }

    private String resolveModelId(ReActContext ctx, Action action) {
        if (action.getModelId() != null) {
            return action.getModelId();
        }
        String modelId = ctx.getCurrentModelId();
        if (ctx.hasModelSwitch()) {
            modelId = ctx.getNextModelId();
        }
        return modelId != null ? modelId : ctx.getDefaultModelId();
    }
}