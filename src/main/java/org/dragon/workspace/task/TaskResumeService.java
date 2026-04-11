package org.dragon.workspace.task;

import java.util.List;
import java.util.Optional;

import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务续跑服务
 * <p>
 * 职责：
 * - 判断消息应该继续执行已有任务还是开启新任务
 * - 从匹配到的任务中解析出实际可执行的任务
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskResumeService {

    private final StoreFactory storeFactory;

    private TaskStore getTaskStore() {
        return storeFactory.get(TaskStore.class);
    }

    /**
     * 续跑决策结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ContinuationResult {
        private ContinuationDecision decision;
        private String taskId;
        private String targetTaskId;
        private String targetTaskType;
        private String reason;
    }

    /**
     * 续跑决策枚举
     */
    public enum ContinuationDecision {
        CONTINUE_EXISTING_TASK,
        START_NEW_TASK,
        NEEDS_MORE_INFO
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
                log.info("[TaskResumeService] Message quotes task {}, continuing", task.getId());
                return buildContinuationResult(task, "Message quotes existing task");
            }
        }

        // 2. 优先级：threadId 命中旧任务
        if (message.getThreadId() != null && !message.getThreadId().isEmpty()) {
            Optional<Task> waitingTask = findWaitingTaskByThreadId(workspaceId, message.getThreadId());
            if (waitingTask.isPresent()) {
                Task task = waitingTask.get();
                log.info("[TaskResumeService] Found waiting task {} for threadId {}", task.getId(), message.getThreadId());
                return buildContinuationResult(task, "Matching threadId with waiting task");
            }
        }

        // 3. 优先级：chatId + WAITING_USER_INPUT
        if (message.getChatId() != null) {
            Optional<Task> waitingTask = findWaitingUserInputTaskByChatId(workspaceId, message.getChatId());
            if (waitingTask.isPresent()) {
                Task task = waitingTask.get();
                log.info("[TaskResumeService] Found WAITING_USER_INPUT task {} for chatId {}", task.getId(), message.getChatId());
                return buildContinuationResult(task, "Matching chatId with waiting user input task");
            }
        }

        // 4. 优先级：chatId + 最近挂起任务
        if (message.getChatId() != null) {
            Optional<Task> waitingTask = findLatestWaitingTaskByChatId(workspaceId, message.getChatId());
            if (waitingTask.isPresent()) {
                Task task = waitingTask.get();
                log.info("[TaskResumeService] Found latest waiting task {} for chatId {}", task.getId(), message.getChatId());
                return buildContinuationResult(task, "Matching chatId with latest waiting task");
            }
        }

        // 5. 默认开启新任务
        log.info("[TaskResumeService] No matching continuation, starting new task");
        return ContinuationResult.builder()
                .decision(ContinuationDecision.START_NEW_TASK)
                .reason("No matching continuation criteria")
                .build();
    }

    /**
     * 从匹配到的任务中解析出实际可执行的任务
     *
     * @param matchedTask 匹配到的任务（可能是父任务或子任务）
     * @return 可执行的任务（如果是父任务则返回等待中的子任务）
     */
    public Task resolveExecutableTask(Task matchedTask) {
        if (matchedTask.getParentTaskId() != null && !matchedTask.getParentTaskId().isEmpty()) {
            return matchedTask;
        }

        List<Task> childTasks = getTaskStore().findByParentTaskId(matchedTask.getId());
        if (childTasks.isEmpty()) {
            log.warn("[TaskResumeService] Parent task {} has no child tasks, using parent", matchedTask.getId());
            return matchedTask;
        }

        Optional<Task> runnableChild = childTasks.stream()
                .filter(this::isTaskRunnable)
                .findFirst();

        if (runnableChild.isPresent()) {
            log.info("[TaskResumeService] Resolved parent task {} to runnable child {}", matchedTask.getId(), runnableChild.get().getId());
            return runnableChild.get();
        }

        log.info("[TaskResumeService] No runnable child for parent {}, using parent", matchedTask.getId());
        return matchedTask;
    }

    /**
     * 从父任务中解析所有可执行子任务
     *
     * @param parentTask 父任务
     * @return 可执行的子任务列表
     */
    public List<Task> resolveExecutableTasks(Task parentTask) {
        List<Task> childTasks = getTaskStore().findByParentTaskId(parentTask.getId());
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

    private ContinuationResult buildContinuationResult(Task task, String reason) {
        String targetTaskId = task.getId();
        String targetTaskType = "PARENT";
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

    private Optional<Task> findTaskByQuotedMessageId(String workspaceId, String quoteMessageId) {
        return getTaskStore().findByWorkspaceId(workspaceId).stream()
                .filter(task -> quoteMessageId.equals(task.getSourceMessageId()))
                .findFirst();
    }

    private Optional<Task> findWaitingTaskByThreadId(String workspaceId, String threadId) {
        return getTaskStore().findByWorkspaceId(workspaceId).stream()
                .filter(task -> threadId.equals(task.getSourceChatId()))
                .filter(task -> task.getStatus() == TaskStatus.SUSPENDED
                        || task.getStatus() == TaskStatus.WAITING_USER_INPUT
                        || task.getStatus() == TaskStatus.WAITING_DEPENDENCY)
                .findFirst();
    }

    private Optional<Task> findWaitingUserInputTaskByChatId(String workspaceId, String chatId) {
        return getTaskStore().findByWorkspaceId(workspaceId).stream()
                .filter(task -> chatId.equals(task.getSourceChatId()))
                .filter(task -> task.getStatus() == TaskStatus.WAITING_USER_INPUT)
                .findFirst();
    }

    private Optional<Task> findLatestWaitingTaskByChatId(String workspaceId, String chatId) {
        return getTaskStore().findByWorkspaceId(workspaceId).stream()
                .filter(task -> chatId.equals(task.getSourceChatId()))
                .filter(task -> task.getStatus() == TaskStatus.SUSPENDED
                        || task.getStatus() == TaskStatus.WAITING_USER_INPUT
                        || task.getStatus() == TaskStatus.WAITING_DEPENDENCY)
                .reduce((first, second) -> second);
    }
}
