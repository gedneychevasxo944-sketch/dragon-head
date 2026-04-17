package org.dragon.step.character;

import java.util.Map;

import org.dragon.agent.react.ObservationEvaluator;
import org.dragon.agent.react.ReActContext;
import org.dragon.step.StepResult;

import lombok.extern.slf4j.Slf4j;

/**
 * EvaluationStep - 评估执行结果，决定是否结束循环
 *
 * <p>ReAct 循环的最后一步。评估 ActStep 的执行结果（Observation），
 * 判断任务是否已经完成，决定是否继续下一次迭代。
 *
 * <p>评估维度：
 * <ul>
 *   <li>available - 结果是否有效</li>
 *   <li>isError - 是否出错</li>
 * </ul>
 *
 * <p>如果 shouldFinish = true，设置 context.complete() 结束循环。
 *
 * @author yijunw
 */
@Slf4j
public class EvaluationStep extends CharacterStep {

    private final ObservationEvaluator observationEvaluator;

    public EvaluationStep(ObservationEvaluator observationEvaluator) {
        super("evalutaion");
        this.observationEvaluator = observationEvaluator;
    }

    public EvaluationStep() {
        super("observe");
        this.observationEvaluator = null;
    }

    @Override
    protected StepResult doExecute(ReActContext ctx) throws Exception {
        String result = getLatestObservation(ctx);
        if (result == null) {
            result = "";
        }

        ctx.addObservation(result);

        if (observationEvaluator == null) {
            // 没有 evaluator，默认不结束
            return StepResult.builder()
                    .stepName(getName())
                    .output(result)
                    .success(true)
                    .metadata(Map.of("shouldFinish", false))
                    .build();
        }

        ObservationEvaluator.ObservationAssessment assessment = observationEvaluator.assess(result);
        boolean shouldFinish = observationEvaluator.shouldFinish(ctx, assessment);

        if (shouldFinish) {
            ctx.complete(result);
        }

        log.info("[EvaluationStep] [{}] Assessment: available={}, isError={}, shouldFinish={}",
                ctx.getCurrentIteration(),
                assessment.isAvailable(),
                assessment.isError(),
                shouldFinish);

        return StepResult.builder()
                .stepName(getName())
                .output(result)
                .success(true)
                .metadata(Map.of(
                        "assessment", assessment,
                        "shouldFinish", shouldFinish,
                        "isError", assessment.isError()
                ))
                .build();
    }
}