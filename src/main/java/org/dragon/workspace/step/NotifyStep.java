package org.dragon.workspace.step;

import org.dragon.task.Task;
import org.dragon.workspace.context.StepResult;
import org.dragon.workspace.context.TaskContext;
import org.dragon.workspace.cooperation.task.notify.WorkspaceTaskNotifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * NotifyStep - 任务状态变更通知
 *
 * <p>从 WorkspaceTaskNotifier 迁移而来，负责发送开始/进度/完成/失败等通知。
 *
 * @author yijunw
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyStep implements Step {

    private final WorkspaceTaskNotifier taskNotifier;

    @Override
    public String getName() {
        return "notify";
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

        try {
            switch (task.getStatus()) {
                case RUNNING -> taskNotifier.notifyStarted(task);
                case COMPLETED -> taskNotifier.notifyCompleted(task);
                case FAILED -> taskNotifier.notifyFailed(task, task.getErrorMessage());
                case WAITING_DEPENDENCY -> taskNotifier.notifyWaiting(task, task.getWaitingReason());
                case WAITING_USER_INPUT -> {
                    if (task.getLastQuestion() != null) {
                        taskNotifier.notifyQuestion(task, task.getLastQuestion());
                    }
                }
                case SUSPENDED -> taskNotifier.notifyWaiting(task, task.getErrorMessage());
                default -> log.debug("[NotifyStep] No notification for status: {}", task.getStatus());
            }

            return StepResult.builder()
                    .stepName(getName())
                    .input(task.getId())
                    .output("notified")
                    .success(true)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("[NotifyStep] Notification failed for task {}: {}", task.getId(), e.getMessage(), e);
            return StepResult.builder()
                    .stepName(getName())
                    .input(task.getId())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }
}
