package org.dragon.workspace.task;

import org.dragon.task.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TaskBridge 接口（保留用于兼容性）
 *
 * <p>已被 Step 执行框架取代，此处仅保留用于兼容现有代码。
 *
 * @author wyj
 * @deprecated Use Step + TaskContext instead
 */
@Deprecated
public interface TaskBridge {

    /**
     * 执行任务
     */
    Task execute(Task task, TaskBridgeContext context);

    /**
     * 暂停任务
     */
    Task suspend(Task task, TaskBridge.SuspendContext context);

    /**
     * 恢复任务
     */
    Task resume(Task task, TaskBridge.ResumeContext context);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class SuspendContext {
        private String reason;
        private String suspendedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ResumeContext {
        private Object newInput;
        private String reason;
    }
}
