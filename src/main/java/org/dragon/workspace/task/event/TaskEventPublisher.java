package org.dragon.workspace.task.event;

import org.dragon.task.Task;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务事件发布器
 * 通过 ApplicationEventPublisher 发布任务相关事件
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 发布子任务完成事件
     *
     * @param completedChildTask 已完成的子任务
     * @param parentTask 父任务
     */
    public void publishChildCompleted(Task completedChildTask, Task parentTask) {
        TaskChildCompletedEvent event = new TaskChildCompletedEvent(this, completedChildTask, parentTask);
        log.info("[TaskEventPublisher] Publishing TaskChildCompletedEvent for child {} of parent {}",
                completedChildTask.getId(), parentTask.getId());
        eventPublisher.publishEvent(event);
    }
}