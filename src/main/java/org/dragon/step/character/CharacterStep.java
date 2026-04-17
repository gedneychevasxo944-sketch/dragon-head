package org.dragon.step.character;

import org.dragon.agent.react.ReActContext;
import org.dragon.step.ExecutionContext;
import org.dragon.step.Step;
import org.dragon.step.StepResult;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * CharacterStep - Character 执行步骤基类
 *
 * <p>ReAct 循环内所有步骤的基类，提供公共能力：
 * <ul>
 *   <li>迭代次数管理（currentIteration）</li>
 *   <li>执行结果记录到 ReActContext</li>
 *   <li>统一的异常处理和日志</li>
 *   <li>辅助方法：getLatestThought()、getLatestObservation()</li>
 * </ul>
 *
 * <p>所有继承类必须实现 {@link #doExecute(ReActContext)} 方法。
 *
 * @author yijunw
 */
@Slf4j
public abstract class CharacterStep implements Step {

    @Getter
    private final String name;

    protected CharacterStep(String name) {
        this.name = name;
    }

    @Override
    public final StepResult execute(ExecutionContext ctx) {
        if (!(ctx instanceof ReActContext)) {
            return StepResult.failure(getName(), "CharacterStep requires ReActContext");
        }
        ReActContext reactCtx = (ReActContext) ctx;

        long startTime = System.currentTimeMillis();
        try {
            log.info("[CharacterStep] [{}] Executing step: {}", reactCtx.getCurrentIteration(), getName());

            // 执行前拦截
            beforeExecute(reactCtx);

            // 执行
            StepResult result = doExecute(reactCtx);
            result.setStepName(getName());
            result.setDurationMs(System.currentTimeMillis() - startTime);

            // 执行后拦截
            afterExecute(reactCtx, result);

            // 记录结果
            reactCtx.recordStepResult(getName(), result);

            log.info("[CharacterStep] [{}] Step {} completed: success={}",
                    reactCtx.getCurrentIteration(), getName(), result.isSuccess());

            return result;

        } catch (Exception e) {
            log.error("[CharacterStep] [{}] Step {} failed: {}",
                    reactCtx.getCurrentIteration(), getName(), e.getMessage(), e);
            return StepResult.builder()
                    .stepName(getName())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 具体 Step 的执行逻辑
     */
    protected abstract StepResult doExecute(ReActContext ctx) throws Exception;

    /**
     * 获取当前 Thought
     */
    protected String getLatestThought(ReActContext ctx) {
        var thoughts = ctx.getThoughts();
        if (thoughts == null || thoughts.isEmpty()) {
            return null;
        }
        return thoughts.get(thoughts.size() - 1);
    }

    /**
     * 获取当前 Action 结果
     */
    protected String getLatestObservation(ReActContext ctx) {
        var observations = ctx.getObservations();
        if (observations == null || observations.isEmpty()) {
            return null;
        }
        return observations.get(observations.size() - 1);
    }
}