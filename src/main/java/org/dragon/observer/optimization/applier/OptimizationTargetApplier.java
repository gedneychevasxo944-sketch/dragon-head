package org.dragon.observer.optimization.applier;

import org.dragon.observer.optimization.plan.OptimizationAction;
import org.dragon.observer.optimization.plan.OptimizationAction.TargetType;

/**
 * OptimizationTargetApplier 优化目标应用器策略接口
 * 负责将优化动作应用到特定类型的目标
 *
 * @author wyj
 * @version 1.0
 */
public interface OptimizationTargetApplier {

    /**
     * 获取支持的目标类型
     *
     * @return 目标类型
     */
    OptimizationAction.TargetType getTargetType();

    /**
     * 应用优化修改
     *
     * @param action 优化动作
     * @return 应用结果
     */
    ApplyResult apply(OptimizationAction action);

    /**
     * 捕获目标快照
     *
     * @param targetId 目标 ID
     * @return JSON 格式的快照
     */
    String captureSnapshot(String targetId);

    /**
     * 从快照恢复
     *
     * @param targetId 目标 ID
     * @param snapshot 快照 JSON
     */
    void restoreFromSnapshot(String targetId, String snapshot);

    /**
     * 应用结果
     */
    class ApplyResult {
        private final boolean success;
        private final String message;
        private final String error;

        private ApplyResult(boolean success, String message, String error) {
            this.success = success;
            this.message = message;
            this.error = error;
        }

        public static ApplyResult success(String message) {
            return new ApplyResult(true, message, null);
        }

        public static ApplyResult failure(String error) {
            return new ApplyResult(false, null, error);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getError() { return error; }
    }
}