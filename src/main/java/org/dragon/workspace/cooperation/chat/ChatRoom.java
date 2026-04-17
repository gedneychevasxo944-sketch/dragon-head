package org.dragon.workspace.cooperation.chat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.dragon.store.StoreFactory;
import org.dragon.task.Task;
import org.dragon.task.TaskExecutionService;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.task.dto.TaskCreationCommand;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;

/**
 * ChatRoom 聊天室（按 Workspace 实例化）
 *
 * <p>Character 之间通过 ChatRoom 发布消息，不关心消费者。
 * 消息按 taskId 路由，消费者自行订阅。
 * <p>
 * 支持需求/响应机制：
 * <ul>
 *   <li>发布 DEMAND：设置 Task 为 WAITING_DEPENDENCY</li>
 *   <li>收到 CLAIMED：添加到认领者列表</li>
 *   <li>收到 RESULT：检查是否所有认领者都完成，完成则唤醒</li>
 * </ul>
 *
 * <p>ChatRoom 按 Workspace 创建，不是全局单例。
 * 通过 {@link TaskExecutionService} 延迟注入来避免循环依赖。
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
public class ChatRoom {

    private final WorkspaceRegistry workspaceRegistry;
    private final StoreFactory storeFactory;
    private final Supplier<TaskExecutionService> taskExecutorProvider;

    public ChatRoom(WorkspaceRegistry workspaceRegistry, StoreFactory storeFactory,
                    Supplier<TaskExecutionService> taskExecutorProvider) {
        this.workspaceRegistry = workspaceRegistry;
        this.storeFactory = storeFactory;
        this.taskExecutorProvider = taskExecutorProvider;
    }

    private ChatMessageStore getMessageStore() {
        return storeFactory.get(ChatMessageStore.class);
    }

    private TaskStore getTaskStore() {
        return storeFactory.get(TaskStore.class);
    }

    // ==================== 发布消息 ====================

    /**
     * 发布消息，自动处理唤醒逻辑
     *
     * @param message 消息
     * @return 含 ID 的消息
     */
    public ChatMessage publish(ChatMessage message) {
        workspaceRegistry.get(message.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workspace not found: " + message.getWorkspaceId()));

        if (message.getId() == null || message.getId().isEmpty()) {
            message.setId(UUID.randomUUID().toString());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }

        getMessageStore().save(message);
        log.info("[ChatRoom] Published message {} from {} to task {}",
                message.getId(), message.getSenderId(), message.getTaskId());

        if (message.getInResponseTo() != null) {
            routeResponse(message);
        }

        return message;
    }

    /**
     * 路由响应消息
     */
    private void routeResponse(ChatMessage response) {
        String demandTaskId = response.getInResponseTo();
        Task demandTask = getTaskStore().findById(demandTaskId).orElse(null);

        if (demandTask == null) {
            log.warn("[ChatRoom] Demand task {} not found", demandTaskId);
            return;
        }

        if (response.getResponseStatus() == ChatMessage.ResponseStatus.CLAIMED) {
            demandTask.addClaimerId(response.getSenderId());
            getTaskStore().update(demandTask);
            log.info("[ChatRoom] Task {} claimed by {}, total claimers: {}",
                    demandTaskId, response.getSenderId(), demandTask.getClaimerIds().size());

        } else if (response.getResponseStatus() == ChatMessage.ResponseStatus.COMPLETED
                || response.getMessageType() == ChatMessage.MessageType.TASK_RESULT) {
            demandTask.removeClaimerId(response.getSenderId());

            if (demandTask.getClaimerIds() == null || demandTask.getClaimerIds().isEmpty()) {
                wakeUpTask(demandTask, response);
            } else {
                getTaskStore().update(demandTask);
                log.info("[ChatRoom] Task {} still waiting for {} claimers",
                        demandTaskId, demandTask.getClaimerIds().size());
            }
        }
    }

    /**
     * 唤醒任务
     */
    private void wakeUpTask(Task task, ChatMessage response) {
        if (task.getWaitingForCharacterId() != null
                && !task.getWaitingForCharacterId().equals(response.getSenderId())) {
            log.info("[ChatRoom] Task {} waiting for {}, but response from {}",
                    task.getId(), task.getWaitingForCharacterId(), response.getSenderId());
            return;
        }

        task.setStatus(TaskStatus.PENDING);
        task.setWaitingReason(null);
        task.setResult(response.getContent());
        task.setUpdatedAt(LocalDateTime.now());
        getTaskStore().update(task);

        // 延迟获取 TaskExecutionService
        TaskExecutionService taskExecutor = taskExecutorProvider.get();
        if (taskExecutor != null) {
            taskExecutor.submitAndExecute(task.getWorkspaceId(),
                    TaskCreationCommand.builder()
                            .taskName(task.getName())
                            .taskDescription("resumed: " + task.getDescription())
                            .input(task.getResult())
                            .creatorId(task.getCreatorId())
                            .sourceChannel(task.getSourceChannel())
                            .build());

            log.info("[ChatRoom] Woke up task {} after receiving {} from {}",
                    task.getId(), response.getMessageType(), response.getSenderId());
        } else {
            log.warn("[ChatRoom] TaskExecutionService not available, cannot wake up task {}", task.getId());
        }
    }

    // ==================== 快捷发布方法 ====================

    public ChatMessage publishDemand(String workspaceId, String taskId,
            String senderId, String correlationId, String demandContent,
            String assignedCharacterId) {
        return publish(ChatMessage.builder()
                .workspaceId(workspaceId)
                .senderId(senderId)
                .taskId(taskId)
                .correlationId(correlationId)
                .messageType(ChatMessage.MessageType.TASK_REQUEST)
                .taskPurpose(ChatMessage.TaskMessagePurpose.TASK_DEPENDENCY)
                .demandContent(demandContent)
                .assignedCharacterId(assignedCharacterId)
                .build());
    }

    public ChatMessage publishClaimed(String workspaceId, String taskId,
            String senderId, String inResponseTo) {
        return publish(ChatMessage.builder()
                .workspaceId(workspaceId)
                .senderId(senderId)
                .taskId(taskId)
                .inResponseTo(inResponseTo)
                .messageType(ChatMessage.MessageType.RESPONSE)
                .responseStatus(ChatMessage.ResponseStatus.CLAIMED)
                .content("已认领，开始处理")
                .build());
    }

    public ChatMessage publishCompleted(String workspaceId, String taskId,
            String senderId, String inResponseTo, String resultContent) {
        return publish(ChatMessage.builder()
                .workspaceId(workspaceId)
                .senderId(senderId)
                .taskId(taskId)
                .inResponseTo(inResponseTo)
                .messageType(ChatMessage.MessageType.TASK_RESULT)
                .responseStatus(ChatMessage.ResponseStatus.COMPLETED)
                .content(resultContent)
                .build());
    }

    // ==================== 订阅消息 ====================

    public List<ChatMessage> getMessages(String taskId) {
        return getMessageStore().findByTaskId(taskId);
    }

    public List<ChatMessage> getMessages(String taskId, int limit) {
        List<ChatMessage> messages = getMessageStore().findByTaskId(taskId);
        if (messages.size() > limit) {
            return messages.subList(messages.size() - limit, messages.size());
        }
        return messages;
    }

    public List<ChatMessage> getMessages(String workspaceId, String receiverId, int limit) {
        return getMessageStore().findByWorkspaceIdAndReceiverId(workspaceId, receiverId, limit);
    }

    public List<ChatMessage> getAllMessages(String workspaceId, LocalDateTime startTime, LocalDateTime endTime) {
        return getMessageStore().findByWorkspaceIdAndTimeRange(workspaceId, startTime, endTime);
    }

    public List<ChatMessage> getMessagesByCharacter(String characterId, int limit) {
        return getMessageStore().findByCharacterId(characterId, limit);
    }
}