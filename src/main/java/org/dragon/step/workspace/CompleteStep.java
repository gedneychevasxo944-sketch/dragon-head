package org.dragon.step.workspace;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.store.StoreFactory;
import org.dragon.step.StepResult;
import org.dragon.step.Step;
import org.dragon.step.ExecutionContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * CompleteStep - 任务完成处理
 *
 * <p>从 TaskDependencyService.checkAndCompleteParentTask 迁移而来。
 * 负责：依赖通知、父任务状态检查。
 *
 * @author yijunw
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompleteStep implements Step {

    private final StoreFactory storeFactory;

    @Override
    public String getName() {
        return "complete";
    }

    @Override
    public StepResult execute(ExecutionContext ctx) {
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

        // 检查并更新父任务状态
        if (task.getChildTaskIds() != null && !task.getChildTaskIds().isEmpty()) {
            Optional<Task> parentTask = storeFactory.get(TaskStore.class).findById(task.getParentTaskId());
            if (parentTask.isPresent()) {
                checkAndCompleteParentTask(parentTask.get());
            }
        }

        return StepResult.builder()
                .stepName(getName())
                .input(task.getId())
                .output(Map.of("completed", true))
                .success(true)
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
    }

    /**
     * 检查并更新父任务状态
     * <p>
     * 规则：
     * - 所有子任务完成 → 父任务完成
     * - 任一子任务失败 → 父任务失败
     * - 无运行中但有等待中 → 父任务保持运行
     */
    public void checkAndCompleteParentTask(Task parentTask) {
        TaskStore taskStore = storeFactory.get(TaskStore.class);
        List<String> childTaskIds = parentTask.getChildTaskIds();
        if (childTaskIds == null || childTaskIds.isEmpty()) {
            return;
        }

        List<Task> childTasks = childTaskIds.stream()
                .map(taskStore::findById)
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
            taskStore.update(parentTask);
            log.info("[CompleteStep] Parent task {} completed", parentTask.getId());
        } else if (anyFailed) {
            parentTask.setStatus(TaskStatus.FAILED);
            taskStore.update(parentTask);
            log.warn("[CompleteStep] Parent task {} has failed child tasks", parentTask.getId());
        } else if (!anyRunning && anyWaiting) {
            parentTask.setStatus(TaskStatus.RUNNING);
            taskStore.update(parentTask);
        }
    }
}
