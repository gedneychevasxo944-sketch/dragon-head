package org.dragon.workspace.task;

import org.dragon.task.Task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务执行桥接器接口
 * <p>
 * 职责：纯执行抽象，不包含编排逻辑
 *
 * @author wyj
 * @version 1.0
 */
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

    /**
     * 任务暂停上下文
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class SuspendContext {
        private String reason;
        private String suspendedAt;
    }

    /**
     * 任务恢复上下文
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ResumeContext {
        private Object newInput;
        private String reason;
    }
}
