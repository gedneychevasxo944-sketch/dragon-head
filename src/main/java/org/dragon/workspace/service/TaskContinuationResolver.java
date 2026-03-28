package org.dragon.workspace.service;

import java.util.List;
import java.util.Optional;

import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务续跑解析器
 * 判断用户消息应该继续执行已有任务还是开启新任务
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskContinuationResolver {

    private final StoreFactory storeFactory;

    private TaskStore getTaskStore() {
        return storeFactory.get(TaskStore.class);
    }

    /**
     * 续跑决策结果
     */
    public enum ContinuationDecision {
        /**
         * 继续已有任务
         */
        CONTINUE_EXISTING_TASK,
        /**
         * 开启新任务
         */
        START_NEW_TASK,
        /**
         * 需要更多信息来判断
         */
        NEEDS_MORE_INFO
    }

    /**
     * 续跑解析结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ContinuationResult {
        private ContinuationDecision decision;
        private String taskId;
        private String targetTaskId;    // 实际要恢复执行的任务 ID（可能是子任务）
        private String targetTaskType;  // PARENT / CHILD
        private String reason;
    }

    /**
     * 解析消息，判断应该继续任务还是开启新任务
     *
     * @param workspaceId Workspace ID
     * @param message 归一化的用户消息
     * @return 续跑解析结果
     */
    public ContinuationResult resolve(String workspaceId, NormalizedMessage message) {
        // 1. 优先级：quoteMessageId 命中旧任务
        if (message.getQuoteMessageId() != null && !message.getQuoteMessageId().isEmpty()) {
            Optional<Task> referencedTask = findTaskByQuotedMessageId(workspaceId, message.getQuoteMessageId());
            if (referencedTask.isPresent()) {
                Task task = referencedTask.get();
                log.info("[TaskContinuationResolver] Message quotes task {}, continuing",
                        task.getId());
                return buildContinuationResult(task, "Message quotes existing task");
            }
        }

        // 2. 优先级：threadId 命中旧任务
        if (message.getThreadId() != null && !message.getThreadId().isEmpty()) {
            Optional<Task> waitingTask = findWaitingTaskByThreadId(workspaceId, message.getThreadId());
            if (waitingTask.isPresent()) {
                Task task = waitingTask.get();
                log.info("[TaskContinuationResolver] Found waiting task {} for threadId {}",
                        task.getId(), message.getThreadId());
                return buildContinuationResult(task, "Matching threadId with waiting task");
            }
        }

        // 3. 优先级：chatId + WAITING_USER_INPUT
        if (message.getChatId() != null) {
            Optional<Task> waitingTask = findWaitingUserInputTaskByChatId(workspaceId, message.getChatId());
            if (waitingTask.isPresent()) {
                Task task = waitingTask.get();
                log.info("[TaskContinuationResolver] Found WAITING_USER_INPUT task {} for chatId {}",
                        task.getId(), message.getChatId());
                return buildContinuationResult(task, "Matching chatId with waiting user input task");
            }
        }

        // 4. 优先级：chatId + 最近挂起任务
        if (message.getChatId() != null) {
            Optional<Task> waitingTask = findLatestWaitingTaskByChatId(workspaceId, message.getChatId());
            if (waitingTask.isPresent()) {
                Task task = waitingTask.get();
                log.info("[TaskContinuationResolver] Found latest waiting task {} for chatId {}",
                        task.getId(), message.getChatId());
                return buildContinuationResult(task, "Matching chatId with latest waiting task");
            }
        }

        // 5. 默认开启新任务
        log.info("[TaskContinuationResolver] No matching continuation, starting new task");
        return ContinuationResult.builder()
                .decision(ContinuationDecision.START_NEW_TASK)
                .reason("No matching continuation criteria")
                .build();
    }

    /**
     * 构建续跑结果，包含目标任务类型判定
     */
    private ContinuationResult buildContinuationResult(Task task, String reason) {
        // 判断任务是父任务还是子任务
        String targetTaskId = task.getId();
        String targetTaskType = "PARENT";

        // 如果有 parentTaskId，说明这是子任务
        if (task.getParentTaskId() != null && !task.getParentTaskId().isEmpty()) {
            targetTaskType = "CHILD";
        }

        return ContinuationResult.builder()
                .decision(ContinuationDecision.CONTINUE_EXISTING_TASK)
                .taskId(task.getId())
                .targetTaskId(targetTaskId)
                .targetTaskType(targetTaskType)
                .reason(reason)
                .build();
    }

    /**
     * 通过被引用消息 ID 查找任务
     */
    private Optional<Task> findTaskByQuotedMessageId(String workspaceId, String quoteMessageId) {
        List<Task> tasks = getTaskStore().findByWorkspaceId(workspaceId);
        return tasks.stream()
                .filter(task -> quoteMessageId.equals(task.getSourceMessageId()))
                .findFirst();
    }

    /**
     * 通过线程 ID 查找等待中的任务
     */
    private Optional<Task> findWaitingTaskByThreadId(String workspaceId, String threadId) {
        List<Task> tasks = getTaskStore().findByWorkspaceId(workspaceId);
        return tasks.stream()
                .filter(task -> threadId.equals(task.getSourceChatId())) // 暂用 sourceChatId 存储 threadId
                .filter(task -> task.getStatus() == TaskStatus.SUSPENDED
                        || task.getStatus() == TaskStatus.WAITING_USER_INPUT
                        || task.getStatus() == TaskStatus.WAITING_DEPENDENCY)
                .findFirst();
    }

    /**
     * 通过 chatId 查找 WAITING_USER_INPUT 状态的任务
     */
    private Optional<Task> findWaitingUserInputTaskByChatId(String workspaceId, String chatId) {
        List<Task> tasks = getTaskStore().findByWorkspaceId(workspaceId);
        return tasks.stream()
                .filter(task -> chatId.equals(task.getSourceChatId()))
                .filter(task -> task.getStatus() == TaskStatus.WAITING_USER_INPUT)
                .findFirst();
    }

    /**
     * 通过 chatId 查找最近挂起的任务
     */
    private Optional<Task> findLatestWaitingTaskByChatId(String workspaceId, String chatId) {
        List<Task> tasks = getTaskStore().findByWorkspaceId(workspaceId);
        return tasks.stream()
                .filter(task -> chatId.equals(task.getSourceChatId()))
                .filter(task -> task.getStatus() == TaskStatus.SUSPENDED
                        || task.getStatus() == TaskStatus.WAITING_USER_INPUT
                        || task.getStatus() == TaskStatus.WAITING_DEPENDENCY)
                .reduce((first, second) -> second); // 获取最后一个（最新的）
    }
}
