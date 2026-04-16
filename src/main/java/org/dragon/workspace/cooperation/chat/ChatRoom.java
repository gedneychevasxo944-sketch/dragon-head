package org.dragon.workspace.cooperation.chat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.dragon.store.StoreFactory;
import org.dragon.workspace.WorkspaceRegistry;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ChatRoom 聊天室服务（Pub/Sub 模式）
 *
 * <p>Character 之间通过 ChatRoom 发布消息，不关心消费者。
 * 消息按 taskId 路由，消费者自行订阅。
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoom {

    private final WorkspaceRegistry workspaceRegistry;
    private final StoreFactory storeFactory;

    private ChatMessageStore getMessageStore() {
        return storeFactory.get(ChatMessageStore.class);
    }

    // ==================== 发布消息 ====================

    /**
     * 发布消息（甩出去不管）
     *
     * @param message 消息（需含 workspaceId, senderId, taskId）
     * @return 含 ID 的消息
     */
    public ChatMessage publish(ChatMessage message) {
        // 验证工作空间存在
        workspaceRegistry.get(message.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workspace not found: " + message.getWorkspaceId()));

        // 生成 ID
        if (message.getId() == null || message.getId().isEmpty()) {
            message.setId(UUID.randomUUID().toString());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }

        getMessageStore().save(message);
        log.info("[ChatRoom] Published message {} from {} to task {}",
                message.getId(), message.getSenderId(), message.getTaskId());

        return message;
    }

    /**
     * 发布结果消息
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID（消息发到此任务）
     * @param senderId 发送者 ID
     * @param resultContent 结果内容
     * @return 消息
     */
    public ChatMessage publishResult(String workspaceId, String taskId,
            String senderId, String resultContent) {
        return publish(ChatMessage.builder()
                .workspaceId(workspaceId)
                .senderId(senderId)
                .taskId(taskId)
                .content(resultContent)
                .messageType(ChatMessage.MessageType.TASK_RESULT)
                .taskPurpose(ChatMessage.TaskMessagePurpose.TASK_RESULT)
                .build());
    }

    /**
     * 发布需求/请求消息
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 目标任务 ID
     * @param senderId 发送者 ID
     * @param requestContent 请求内容
     * @return 消息
     */
    public ChatMessage publishRequest(String workspaceId, String taskId,
            String senderId, String requestContent) {
        return publish(ChatMessage.builder()
                .workspaceId(workspaceId)
                .senderId(senderId)
                .taskId(taskId)
                .content(requestContent)
                .messageType(ChatMessage.MessageType.TASK_REQUEST)
                .taskPurpose(ChatMessage.TaskMessagePurpose.TASK_DEPENDENCY)
                .build());
    }

    // ==================== 订阅消息 ====================

    /**
     * 获取任务相关的消息
     *
     * @param taskId 任务 ID
     * @return 消息列表
     */
    public List<ChatMessage> getMessages(String taskId) {
        return getMessageStore().findByTaskId(taskId);
    }

    /**
     * 获取任务相关的最新消息
     *
     * @param taskId 任务 ID
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<ChatMessage> getMessages(String taskId, int limit) {
        List<ChatMessage> messages = getMessageStore().findByTaskId(taskId);
        if (messages.size() > limit) {
            return messages.subList(messages.size() - limit, messages.size());
        }
        return messages;
    }

    /**
     * 获取接收者的消息
     *
     * @param workspaceId 工作空间 ID
     * @param receiverId 接收者 ID
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<ChatMessage> getMessages(String workspaceId, String receiverId, int limit) {
        return getMessageStore().findByWorkspaceIdAndReceiverId(workspaceId, receiverId, limit);
    }

    /**
     * 获取工作空间内所有消息（用于 Observer）
     *
     * @param workspaceId 工作空间 ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 消息列表
     */
    public List<ChatMessage> getAllMessages(String workspaceId, LocalDateTime startTime, LocalDateTime endTime) {
        return getMessageStore().findByWorkspaceIdAndTimeRange(workspaceId, startTime, endTime);
    }

    /**
     * 获取特定 Character 发送或接收的消息（用于 Observer）
     *
     * @param characterId Character ID
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<ChatMessage> getMessagesByCharacter(String characterId, int limit) {
        return getMessageStore().findByCharacterId(characterId, limit);
    }
}