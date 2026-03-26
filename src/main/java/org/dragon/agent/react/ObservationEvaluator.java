package org.dragon.agent.react;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 观察结果评估器
 * 负责评估 Action 执行后的观察结果是否可用
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class ObservationEvaluator {

    /**
     * 评估观察结果是否可用
     */
    public ObservationAssessment assess(String observation) {
        if (observation == null || observation.isEmpty()) {
            return ObservationAssessment.builder()
                    .available(false)
                    .isError(false)
                    .reason("观察结果为空")
                    .build();
        }

        String lowerObs = observation.toLowerCase();
        boolean isError = lowerObs.contains("error")
                || lowerObs.contains("exception")
                || lowerObs.contains("failed")
                || lowerObs.contains("失败")
                || lowerObs.contains("tool not allowed")
                || lowerObs.contains("tool not found");

        if (isError) {
            return ObservationAssessment.builder()
                    .available(false)
                    .isError(true)
                    .reason("观察结果包含错误信息")
                    .build();
        }

        return ObservationAssessment.builder()
                .available(true)
                .isError(false)
                .reason("观察结果正常")
                .build();
    }

    /**
     * 判断是否应该结束执行
     */
    public boolean shouldFinish(ReActContext context, ObservationAssessment assessment) {
        if (context.getActions().isEmpty()) {
            return false;
        }

        Action lastAction = context.getActions().get(context.getActions().size() - 1);
        Action.ActionType type = lastAction.getType();

        // STATUS_CHANGE 类型应该结束当前轮次（由执行层处理状态变更）
        if (type == Action.ActionType.STATUS_CHANGE) {
            return true;
        }

        // RESPOND/FINISH 类型需要检查观察结果是否可用
        if (type == Action.ActionType.RESPOND || type == Action.ActionType.FINISH) {
            return assessment != null && assessment.isAvailable();
        }

        // TOOL/MEMORY 类型不应该结束
        return false;
    }

    /**
     * 观察结果可用性评估
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ObservationAssessment {
        private boolean available;
        private boolean isError;
        private String reason;
    }
}
