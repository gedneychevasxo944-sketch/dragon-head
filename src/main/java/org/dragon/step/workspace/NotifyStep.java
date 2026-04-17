package org.dragon.step.workspace;

import org.dragon.task.Task;
import org.dragon.step.StepResult;
import org.dragon.step.Step;
import org.dragon.step.ExecutionContext;
import org.dragon.workspace.cooperation.task.notify.WorkspaceTaskNotifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * NotifyStep - 任务状态变更通知
 *
 * <p>向相关方发送任务状态变更通知。包括：
 * <ul>
 *   <li>RUNNING - 任务开始执行</li>
 *   <li>COMPLETED - 任务完成</li>
 *   <li>FAILED - 任务失败</li>
 *   <li>WAITING_DEPENDENCY - 等待依赖（挂起）</li>
 *   <li>WAITING_USER_INPUT - 等待用户输入</li>
 *   <li>SUSPENDED - 暂停</li>
 * </ul>
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
