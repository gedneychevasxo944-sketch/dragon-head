package org.dragon.workspace.step;

import java.util.Optional;

import org.dragon.channel.entity.NormalizedMessage;
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
 * ResumeStep - 判断继续执行旧任务还是启动新任务
 *
 * <p>从 TaskResumeService 迁移而来，判断消息应该继续已有任务还是开启新任务。
 *
 * @author yijunw
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeStep implements Step {

    private final StoreFactory storeFactory;

    @Override
    public String getName() {
        return "resume";
    }

    @Override
    public StepResult execute(TaskContext ctx) {
        long startTime = System.currentTimeMillis();
        NormalizedMessage message = getMessageFromContext(ctx);

        if (message == null) {
            return StepResult.builder()
                    .stepName(getName())
                    .success(true)
                    .output("START_NEW_TASK")
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        ContinuationResult result = resolve(message, ctx.getWorkspaceId());

        // 设置结果到上下文
        ctx.setConfigValue("continuationDecision", result.decision.name());
        ctx.setConfigValue("targetTaskId", result.targetTaskId);
        ctx.setConfigValue("targetTaskType", result.targetTaskType);

        return StepResult.builder()
                .stepName(getName())
                .input(message)
                .output(result)
                .success(true)
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
    }

    private NormalizedMessage getMessageFromContext(TaskContext ctx) {
        Object input = ctx.getTask() != null ? ctx.getTask().getInput() : null;
        if (input instanceof NormalizedMessage) {
            return (NormalizedMessage) input;
        }
        return null;
    }

    private ContinuationResult resolve(NormalizedMessage message, String workspaceId) {
        TaskStore taskStore = storeFactory.get(TaskStore.class);

        // 1. 优先级：quoteMessageId 命中旧任务
        if (message.getQuoteMessageId() != null && !message.getQuoteMessageId().isEmpty()) {
            Optional<Task> referencedTask = taskStore.findByWorkspaceId(workspaceId).stream()
                    .filter(task -> message.getQuoteMessageId().equals(task.getSourceMessageId()))
                    .findFirst();
            if (referencedTask.isPresent()) {
                return buildResult(referencedTask.get(), "Message quotes existing task");
            }
        }

        // 2. 优先级：threadId 命中旧任务
        if (message.getThreadId() != null && !message.getThreadId().isEmpty()) {
            Optional<Task> waitingTask = taskStore.findByWorkspaceId(workspaceId).stream()
                    .filter(task -> message.getThreadId().equals(task.getSourceChatId()))
                    .filter(task -> isWaitingStatus(task.getStatus()))
                    .findFirst();
            if (waitingTask.isPresent()) {
                return buildResult(waitingTask.get(), "Matching threadId with waiting task");
            }
        }

        // 3. 优先级：chatId + WAITING_USER_INPUT
        if (message.getChatId() != null) {
            Optional<Task> waitingTask = taskStore.findByWorkspaceId(workspaceId).stream()
                    .filter(task -> message.getChatId().equals(task.getSourceChatId()))
                    .filter(task -> task.getStatus() == TaskStatus.WAITING_USER_INPUT)
                    .findFirst();
            if (waitingTask.isPresent()) {
                return buildResult(waitingTask.get(), "Matching chatId with waiting user input task");
            }
        }

        // 4. 优先级：chatId + 最近挂起任务
        if (message.getChatId() != null) {
            Optional<Task> waitingTask = taskStore.findByWorkspaceId(workspaceId).stream()
                    .filter(task -> message.getChatId().equals(task.getSourceChatId()))
                    .filter(task -> isWaitingStatus(task.getStatus()))
                    .reduce((first, second) -> second);
            if (waitingTask.isPresent()) {
                return buildResult(waitingTask.get(), "Matching chatId with latest waiting task");
            }
        }

        // 5. 默认开启新任务
        return new ContinuationResult(ContinuationDecision.START_NEW_TASK, null, null, null, "No matching continuation criteria");
    }

    private boolean isWaitingStatus(TaskStatus status) {
        return status == TaskStatus.SUSPENDED
                || status == TaskStatus.WAITING_USER_INPUT
                || status == TaskStatus.WAITING_DEPENDENCY;
    }

    private ContinuationResult buildResult(Task task, String reason) {
        String targetTaskId = task.getId();
        String targetTaskType = task.getParentTaskId() != null ? "CHILD" : "PARENT";
        return new ContinuationResult(ContinuationDecision.CONTINUE_EXISTING_TASK, task.getId(), targetTaskId, targetTaskType, reason);
    }

    /**
     * 续跑决策结果
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ContinuationResult {
        private ContinuationDecision decision;
        private String taskId;
        private String targetTaskId;
        private String targetTaskType;
        private String reason;
    }

    /**
     * 续跑决策枚举
     */
    private enum ContinuationDecision {
        CONTINUE_EXISTING_TASK,
        START_NEW_TASK,
        NEEDS_MORE_INFO
    }
}
