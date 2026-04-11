package org.dragon.workspace.task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 统一管理任务依赖
 * <p>
 * 职责：
 * - 依赖检查（areDependenciesMet）
 * - 依赖解决通知与重调度（notifyDependencyResolved）
 * - 父任务状态检查（checkAndCompleteParentTask）
 * - 可执行子任务查找（findRunnableChildTasks）
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDependencyService {

    private final StoreFactory storeFactory;

    private TaskStore getTaskStore() {
        return storeFactory.get(TaskStore.class);
    }

    /**
     * 检查任务的所有依赖是否都已完成
     *
     * @param task 任务
     * @return 依赖是否全部满足
     */
    public boolean areDependenciesMet(Task task) {
        List<String> dependencyTaskIds = task.getDependencyTaskIds();
        if (dependencyTaskIds == null || dependencyTaskIds.isEmpty()) {
            return true;
        }

        for (String depTaskId : dependencyTaskIds) {
            Optional<Task> depTask = getTaskStore().findById(depTaskId);
            if (depTask.isEmpty() || depTask.get().getStatus() != TaskStatus.COMPLETED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 通知依赖已解决，触发等待此依赖的任务重调度
     *
     * @param dependencyTaskId 已解决的依赖任务 ID
     */
    public void notifyDependencyResolved(String dependencyTaskId) {
        List<Task> waitingTasks = getTaskStore().findWaitingTasksByDependencyTaskId(dependencyTaskId);
        for (Task task : waitingTasks) {
            if (areDependenciesMet(task)) {
                log.info("[TaskDependencyService] All dependencies resolved for task {}, re-scheduling", task.getId());
                task.setStatus(TaskStatus.PENDING);
                task.setUpdatedAt(LocalDateTime.now());
                getTaskStore().update(task);
            }
        }
        log.info("[TaskDependencyService] Dependency {} resolved, checked {} waiting tasks", dependencyTaskId, waitingTasks.size());
    }

    /**
     * 查找父任务下所有可执行的子任务
     *
     * @param parentTaskId 父任务 ID
     * @return 可执行的子任务列表
     */
    public List<Task> findRunnableChildTasks(String parentTaskId) {
        return getTaskStore().findRunnableChildTasks(parentTaskId);
    }

    /**
     * 检查并更新父任务状态
     * <p>
     * 规则：
     * - 所有子任务完成 → 父任务完成
     * - 任一子任务失败 → 父任务失败
     * - 无运行中但有等待中 → 父任务保持运行
     *
     * @param parentTask 父任务
     */
    public void checkAndCompleteParentTask(Task parentTask) {
        List<String> childTaskIds = parentTask.getChildTaskIds();
        if (childTaskIds == null || childTaskIds.isEmpty()) {
            return;
        }

        List<Task> childTasks = childTaskIds.stream()
                .map(getTaskStore()::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        boolean allCompleted = childTasks.stream()
                .allMatch(st -> st.getStatus() == TaskStatus.COMPLETED);
        boolean anyFailed = childTasks.stream()
                .anyMatch(st -> st.getStatus() == TaskStatus.FAILED);
        boolean anyRunning = childTasks.stream()
                .anyMatch(st -> st.getStatus() == TaskStatus.RUNNING);
        boolean anyWaiting = childTasks.stream()
                .anyMatch(st -> st.getStatus() == TaskStatus.WAITING_DEPENDENCY
                        || st.getStatus() == TaskStatus.WAITING_USER_INPUT
                        || st.getStatus() == TaskStatus.SUSPENDED);

        if (allCompleted) {
            parentTask.setStatus(TaskStatus.COMPLETED);
            parentTask.setCompletedAt(LocalDateTime.now());
            parentTask.setResult("All child tasks completed successfully");
            getTaskStore().update(parentTask);
            log.info("[TaskDependencyService] Parent task {} completed", parentTask.getId());
        } else if (anyFailed) {
            parentTask.setStatus(TaskStatus.FAILED);
            getTaskStore().update(parentTask);
            log.warn("[TaskDependencyService] Parent task {} has failed child tasks", parentTask.getId());
        } else if (!anyRunning && anyWaiting) {
            parentTask.setStatus(TaskStatus.RUNNING);
            getTaskStore().update(parentTask);
        }
    }
}
