package org.dragon.workspace.step;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.store.StoreFactory;
import org.dragon.workspace.context.StepResult;
import org.dragon.workspace.context.TaskContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * DependencyStep - 检查任务依赖是否满足
 *
 * <p>从 TaskDependencyService.areDependenciesMet 迁移而来。
 *
 * @author yijunw
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DependencyStep implements Step {

    private final StoreFactory storeFactory;

    @Override
    public String getName() {
        return "dependency";
    }

    @Override
    public StepResult execute(TaskContext ctx) {
        long startTime = System.currentTimeMillis();
        Task task = ctx.getTask();

        if (task == null) {
            return StepResult.builder()
                    .stepName(getName())
                    .success(false)
                    .errorMessage("No task in context")
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        boolean dependenciesMet = areDependenciesMet(task);

        Map<String, Object> output = Map.of(
                "dependenciesMet", dependenciesMet,
                "dependencyTaskIds", task.getDependencyTaskIds() != null ? task.getDependencyTaskIds() : List.of()
        );

        return StepResult.builder()
                .stepName(getName())
                .input(task.getId())
                .output(output)
                .success(true)
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
    }

    /**
     * 检查任务的所有依赖是否都已完成
     */
    public boolean areDependenciesMet(Task task) {
        List<String> dependencyTaskIds = task.getDependencyTaskIds();
        if (dependencyTaskIds == null || dependencyTaskIds.isEmpty()) {
            return true;
        }

        TaskStore taskStore = storeFactory.get(TaskStore.class);
        for (String depTaskId : dependencyTaskIds) {
            Optional<Task> depTask = taskStore.findById(depTaskId);
            if (depTask.isEmpty() || depTask.get().getStatus() != TaskStatus.COMPLETED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 通知依赖已解决，触发等待此依赖的任务重调度
     */
    public void notifyDependencyResolved(String dependencyTaskId) {
        TaskStore taskStore = storeFactory.get(TaskStore.class);
        List<Task> waitingTasks = taskStore.findWaitingTasksByDependencyTaskId(dependencyTaskId);
        for (Task task : waitingTasks) {
            if (areDependenciesMet(task)) {
                log.info("[DependencyStep] All dependencies resolved for task {}, re-scheduling", task.getId());
                task.setStatus(TaskStatus.PENDING);
                task.setUpdatedAt(java.time.LocalDateTime.now());
                taskStore.update(task);
            }
        }
        log.info("[DependencyStep] Dependency {} resolved, checked {} waiting tasks", dependencyTaskId, waitingTasks.size());
    }
}
