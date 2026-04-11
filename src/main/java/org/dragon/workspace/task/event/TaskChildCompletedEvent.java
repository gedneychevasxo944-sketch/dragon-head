package org.dragon.workspace.task.event;

import org.dragon.task.Task;
import org.springframework.context.ApplicationEvent;

import lombok.Getter;

/**
 * 子任务完成事件
 * 当子任务执行完成时发布，用于触发后续任务调度
 *
 * @author wyj
 * @version 1.0
 */
@Getter
public class TaskChildCompletedEvent extends ApplicationEvent {

    private final Task completedChildTask;
    private final Task parentTask;

    public TaskChildCompletedEvent(Object source, Task completedChildTask, Task parentTask) {
        super(source);
        this.completedChildTask = completedChildTask;
        this.parentTask = parentTask;
    }
}