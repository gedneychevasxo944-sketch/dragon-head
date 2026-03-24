package org.dragon.workspace.service;

import java.util.List;
import java.util.Optional;

import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务恢复目标解析器
 * 负责从匹配到的任务中解析出正确的可执行任务
 * - 如果命中的是子任务，直接返回
 * - 如果命中的是父任务，挑选可继续的子任务
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskResumeTargetResolver {

    private final TaskStore taskStore;

    /**
     * 从匹配到的任务中解析出实际可执行的任务
     *
     * @param matchedTask 匹配到的任务（可能是父任务或子任务）
     * @return 可执行的任务（如果是父任务则返回等待中的子任务）
     */
    public Task resolveExecutableTask(Task matchedTask) {
        // 如果命中的是子任务（有 parentTaskId），直接返回
        if (matchedTask.getParentTaskId() != null && !matchedTask.getParentTaskId().isEmpty()) {
            return matchedTask;
        }

        // 如果命中的是父任务，查找可继续的子任务
        List<Task> childTasks = taskStore.findByParentTaskId(matchedTask.getId());
        if (childTasks.isEmpty()) {
            log.warn("[TaskResumeTargetResolver] Parent task {} has no child tasks, using parent", matchedTask.getId());
            return matchedTask;
        }

        // 查找处于等待状态的子任务
        Optional<Task> runnableChild = childTasks.stream()
                .filter(this::isTaskRunnable)
                .findFirst();

        if (runnableChild.isPresent()) {
            log.info("[TaskResumeTargetResolver] Resolved parent task {} to runnable child {}",
                    matchedTask.getId(), runnableChild.get().getId());
            return runnableChild.get();
        }

        // 没有可运行的子任务，返回父任务（由编排层处理）
        log.info("[TaskResumeTargetResolver] No runnable child for parent {}, using parent", matchedTask.getId());
        return matchedTask;
    }

    /**
     * 从父任务中解析所有可执行子任务
     *
     * @param parentTask 父任务
     * @return 可执行的子任务列表
     */
    public List<Task> resolveExecutableTasks(Task parentTask) {
        List<Task> childTasks = taskStore.findByParentTaskId(parentTask.getId());
        return childTasks.stream()
                .filter(this::isTaskRunnable)
                .toList();
    }

    /**
     * 判断任务是否处于可执行状态
     */
    public boolean isTaskRunnable(Task task) {
        TaskStatus status = task.getStatus();
        return status == TaskStatus.PENDING
                || status == TaskStatus.WAITING_DEPENDENCY
                || status == TaskStatus.WAITING_USER_INPUT
                || status == TaskStatus.SUSPENDED;
    }
}
