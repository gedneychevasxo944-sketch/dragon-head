package org.dragon.workspace.task.listener;

import java.util.List;

import org.dragon.store.StoreFactory;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.workspace.WorkspaceTaskExecutor;
import org.dragon.workspace.cooperation.task.notify.WorkspaceTaskNotifier;
import org.dragon.workspace.step.DependencyStep;
import org.dragon.workspace.task.event.TaskChildCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务调度监听器
 * 监听子任务完成事件，触发等待中的可执行任务
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskSchedulerListener {

    private final DependencyStep dependencyStep;
    private final WorkspaceTaskExecutor workspaceTaskExecutor;
    private final WorkspaceTaskNotifier taskNotifier;
    private final StoreFactory storeFactory;

    private TaskStore getTaskStore() {
        return storeFactory.get(TaskStore.class);
    }

    /**
     * 处理子任务完成事件
     * 查找并执行同一父任务下其他可执行的子任务（非递归）
     */
    @Async
    @EventListener
    public void onChildTaskCompleted(TaskChildCompletedEvent event) {
        Task completedChild = event.getCompletedChildTask();
        Task parentTask = event.getParentTask();
        log.info("[TaskSchedulerListener] Handling completion of child {} for parent {}",
                completedChild.getId(), parentTask.getId());

        // 通知依赖解决，触发等待此依赖的任务重新调度
        dependencyStep.notifyDependencyResolved(completedChild.getId());

        // 查找并执行可执行的兄弟任务
        List<Task> runnableTasks = findRunnableChildTasks(parentTask.getId());
        for (Task runnableTask : runnableTasks) {
            try {
                workspaceTaskExecutor.resumeAndExecute(parentTask.getWorkspaceId(), runnableTask, null);
            } catch (Exception e) {
                log.error("[TaskSchedulerListener] Error executing runnable child task {}: {}",
                        runnableTask.getId(), e.getMessage());
                runnableTask.setStatus(TaskStatus.FAILED);
                runnableTask.setErrorMessage(e.getMessage());
                getTaskStore().update(runnableTask);
                taskNotifier.notifyFailed(runnableTask, e.getMessage());
            }
        }
    }

    /**
     * 查找父任务下所有可执行的子任务
     */
    private List<Task> findRunnableChildTasks(String parentTaskId) {
        return getTaskStore().findRunnableChildTasks(parentTaskId);
    }
}
