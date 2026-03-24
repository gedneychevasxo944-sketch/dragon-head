package org.dragon.workspace.service.task.execution;

import org.dragon.task.Task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务执行桥接器接口
 * 负责将任务委托给 Character 执行，并处理执行结果
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
     * 通知依赖任务已解决
     */
    void notifyDependencyResolved(String taskId, String dependencyTaskId);

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
