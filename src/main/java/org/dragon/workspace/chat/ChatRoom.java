package org.dragon.workspace.chat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ChatRoom 聊天室服务
 * 提供工作空间内的消息传递和协作会话管理
 * 会记录所有 Character 之间的沟通信息
 * 实现 ChatRoomObserver 接口供 Observer 使用
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoom implements ChatRoomObserver {

    private final WorkspaceRegistry workspaceRegistry;
    private final StoreFactory storeFactory;

    private ChatMessageStore getMessageStore() {
        return storeFactory.get(ChatMessageStore.class);
    }

    private ChatSessionStore getSessionStore() {
        return storeFactory.get(ChatSessionStore.class);
    }

    // ==================== 消息功能 ====================

    /**
     * 发送消息
     *
     * @param message 消息
     * @return 含 ID 的消息
     */
    public ChatMessage sendMessage(ChatMessage message) {
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
        log.info("[ChatRoom] Sent message {} in workspace {}",
                message.getId(), message.getWorkspaceId());

        return message;
    }

    /**
     * 获取消息
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<ChatMessage> getMessages(String workspaceId, String characterId, int limit) {
        return getMessageStore().findByWorkspaceIdAndReceiverId(workspaceId, characterId, limit);
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

    // ==================== 会话功能 ====================

    /**
     * 创建协作会话
     *
     * @param workspaceId 工作空间 ID
     * @param participantIds 参与者 ID 列表
     * @param taskId 关联任务 ID
     * @return 创建的会话
     */
    public ChatSession createSession(String workspaceId,
            List<String> participantIds, String taskId) {
        // 验证工作空间存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workspace not found: " + workspaceId));

        ChatSession session = ChatSession.builder()
                .id(UUID.randomUUID().toString())
                .workspaceId(workspaceId)
                .taskId(taskId)
                .participantIds(participantIds)
                .context(java.util.Collections.emptyMap())
                .participantStates(new java.util.HashMap<>())
                .taskStates(new java.util.HashMap<>())
                .blockedParticipants(new ArrayList<>())
                .decisions(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status(ChatSession.Status.ACTIVE)
                .build();

        getSessionStore().save(session);
        log.info("[ChatRoom] Created session {} in workspace {}",
                session.getId(), workspaceId);

        return session;
    }

    /**
     * 添加参与者到会话
     *
     * @param sessionId 会话 ID
     * @param characterId Character ID
     */
    public void addToSession(String sessionId, String characterId) {
        ChatSession session = getSessionStore().findById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        if (!session.getParticipantIds().contains(characterId)) {
            session.getParticipantIds().add(characterId);
            session.setUpdatedAt(LocalDateTime.now());
            getSessionStore().update(session);
        }
    }

    /**
     * 记录决策
     *
     * @param sessionId 会话 ID
     * @param decision 决策记录
     */
    public void recordDecision(String sessionId, ChatSession.DecisionRecord decision) {
        ChatSession session = getSessionStore().findById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        if (decision.getId() == null || decision.getId().isEmpty()) {
            decision.setId(UUID.randomUUID().toString());
        }
        if (decision.getTimestamp() == null) {
            decision.setTimestamp(LocalDateTime.now());
        }

        session.getDecisions().add(decision);
        session.setUpdatedAt(LocalDateTime.now());
        getSessionStore().update(session);
    }

    /**
     * 获取会话
     *
     * @param sessionId 会话 ID
     * @return 会话
     */
    public ChatSession getSession(String sessionId) {
        return getSessionStore().findById(sessionId);
    }

    /**
     * 获取会话中的所有消息（用于 Observer）
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    public List<ChatMessage> getSessionMessages(String sessionId) {
        return getMessageStore().findBySessionId(sessionId);
    }

    /**
     * 完成会话
     *
     * @param sessionId 会话 ID
     */
    public void completeSession(String sessionId) {
        ChatSession session = getSessionStore().findById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        session.setStatus(ChatSession.Status.COMPLETED);
        session.setUpdatedAt(LocalDateTime.now());
        getSessionStore().update(session);
    }

    /**
     * 获取任务关联的会话
     *
     * @param taskId 任务 ID
     * @return 会话
     */
    public ChatSession getSessionByTaskId(String taskId) {
        return getSessionStore().findByTaskId(taskId);
    }

    // ==================== 任务协作方法 ====================

    /**
     * 开始任务协作会话
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @param participantIds 参与者 ID 列表
     * @return 创建的协作会话
     */
    public ChatSession startTaskCollaboration(String workspaceId, String taskId, List<String> participantIds) {
        log.info("[ChatRoom] Starting task collaboration for task {} in workspace {}", taskId, workspaceId);

        // 检查是否已存在该任务的协作会话
        ChatSession existingSession = getSessionStore().findByTaskId(taskId);
        if (existingSession != null) {
            log.info("[ChatRoom] Task {} already has collaboration session {}", taskId, existingSession.getId());
            return existingSession;
        }

        return createSession(workspaceId, participantIds, taskId);
    }

    /**
     * 发送任务消息
     *
     * @param message 消息（需包含 taskId）
     * @return 含 ID 的消息
     */
    public ChatMessage sendTaskMessage(ChatMessage message) {
        if (message.getTaskId() == null) {
            throw new IllegalArgumentException("taskId is required for task messages");
        }

        // 确保消息类型为 TASK
        if (message.getMessageType() == ChatMessage.MessageType.TEXT) {
            message.setMessageType(ChatMessage.MessageType.TASK);
        }

        return sendMessage(message);
    }

    /**
     * 发送任务状态更新消息
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @param senderId 发送者 ID
     * @param status 新状态
     * @param content 更新内容
     * @return 消息
     */
    public ChatMessage sendTaskUpdateMessage(String workspaceId, String taskId,
            String senderId, String status, String content) {
        ChatMessage message = ChatMessage.builder()
                .workspaceId(workspaceId)
                .senderId(senderId)
                .taskId(taskId)
                .content(content)
                .messageType(ChatMessage.MessageType.TASK)
                .taskPurpose(ChatMessage.TaskMessagePurpose.TASK_UPDATE)
                .messageSubtype(status)
                .build();

        return sendTaskMessage(message);
    }

    /**
     * 发送任务分配消息
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @param parentTaskId 父任务 ID
     * @param senderId 发送者 ID
     * @param assigneeId 被分配者 ID
     * @param content 分配说明
     * @return 消息
     */
    public ChatMessage sendTaskAssignmentMessage(String workspaceId, String taskId,
            String parentTaskId, String senderId, String assigneeId, String content) {
        ChatMessage message = ChatMessage.builder()
                .workspaceId(workspaceId)
                .senderId(senderId)
                .receiverId(assigneeId)
                .taskId(taskId)
                .parentTaskId(parentTaskId)
                .content(content)
                .messageType(ChatMessage.MessageType.TASK)
                .taskPurpose(ChatMessage.TaskMessagePurpose.TASK_ASSIGNMENT)
                .build();

        return sendTaskMessage(message);
    }

    /**
     * 发送任务结果消息
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @param senderId 发送者 ID
     * @param resultStatus 结果状态（COMPLETED/FAILED）
     * @param resultContent 结果内容
     * @return 消息
     */
    public ChatMessage sendTaskResultMessage(String workspaceId, String taskId,
            String senderId, String resultStatus, String resultContent) {
        ChatMessage message = ChatMessage.builder()
                .workspaceId(workspaceId)
                .senderId(senderId)
                .taskId(taskId)
                .content(resultContent)
                .messageType(ChatMessage.MessageType.TASK)
                .taskPurpose(ChatMessage.TaskMessagePurpose.TASK_RESULT)
                .messageSubtype(resultStatus)
                .build();

        return sendTaskMessage(message);
    }

    /**
     * 发送任务阻塞消息
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @param senderId 发送者 ID
     * @param reason 阻塞原因
     * @return 消息
     */
    public ChatMessage sendTaskBlockedMessage(String workspaceId, String taskId,
            String senderId, String reason) {
        ChatMessage message = ChatMessage.builder()
                .workspaceId(workspaceId)
                .senderId(senderId)
                .taskId(taskId)
                .content(reason)
                .messageType(ChatMessage.MessageType.TASK)
                .taskPurpose(ChatMessage.TaskMessagePurpose.TASK_BLOCKED)
                .build();

        return sendTaskMessage(message);
    }

    /**
     * 发送任务完成通知
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @param senderId 发送者 ID
     * @param summary 完成摘要
     * @return 消息
     */
    public ChatMessage sendTaskCompleteMessage(String workspaceId, String taskId,
            String senderId, String summary) {
        ChatMessage message = ChatMessage.builder()
                .workspaceId(workspaceId)
                .senderId(senderId)
                .taskId(taskId)
                .content(summary)
                .messageType(ChatMessage.MessageType.TASK)
                .taskPurpose(ChatMessage.TaskMessagePurpose.TASK_COMPLETE)
                .messageSubtype("COMPLETED")
                .build();

        return sendTaskMessage(message);
    }

    /**
     * 获取任务协作历史
     *
     * @param taskId 任务 ID
     * @return 消息列表
     */
    public List<ChatMessage> getTaskCollaborationHistory(String taskId) {
        ChatSession session = getSessionStore().findByTaskId(taskId);
        if (session == null) {
            return new ArrayList<>();
        }
        return getMessageStore().findBySessionId(session.getId());
    }

    /**
     * 获取任务的所有消息（不限于会话）
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @return 任务相关的消息列表
     */
    public List<ChatMessage> getTaskMessages(String workspaceId, String taskId) {
        return getMessageStore().findByTaskId(taskId);
    }

    /**
     * 通知任务协作完成
     *
     * @param taskId 任务 ID
     */
    public void notifyTaskCollaborationComplete(String taskId) {
        ChatSession session = getSessionStore().findByTaskId(taskId);
        if (session != null) {
            completeSession(session.getId());
            log.info("[ChatRoom] Task collaboration {} completed", taskId);
        }
    }

    // ==================== 参与者状态管理 ====================

    /**
     * 标记参与者等待状态
     *
     * @param sessionId 会话 ID
     * @param characterId Character ID
     * @param reason 等待原因
     */
    public void markParticipantWaiting(String sessionId, String characterId, String reason) {
        ChatSession session = getSessionStore().findById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        if (session.getParticipantStates() == null) {
            session.setParticipantStates(new java.util.HashMap<>());
        }
        session.getParticipantStates().put(characterId, "WAITING:" + reason);
        if (session.getBlockedParticipants() == null) {
            session.setBlockedParticipants(new ArrayList<>());
        }
        if (!session.getBlockedParticipants().contains(characterId)) {
            session.getBlockedParticipants().add(characterId);
        }
        session.setUpdatedAt(LocalDateTime.now());
        getSessionStore().update(session);
        log.info("[ChatRoom] Participant {} marked as waiting in session {}: {}", characterId, sessionId, reason);
    }

    /**
     * 标记参与者就绪状态
     *
     * @param sessionId 会话 ID
     * @param characterId Character ID
     */
    public void markParticipantReady(String sessionId, String characterId) {
        ChatSession session = getSessionStore().findById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        if (session.getParticipantStates() == null) {
            session.setParticipantStates(new java.util.HashMap<>());
        }
        session.getParticipantStates().put(characterId, "READY");
        if (session.getBlockedParticipants() != null) {
            session.getBlockedParticipants().remove(characterId);
        }
        session.setUpdatedAt(LocalDateTime.now());
        getSessionStore().update(session);
        log.info("[ChatRoom] Participant {} marked as ready in session {}", characterId, sessionId);
    }

    /**
     * 获取会话消息列表
     *
     * @param sessionId 会话 ID
     * @param limit 限制数量
     * @return 消息列表
     */
    public List<ChatMessage> listSessionMessages(String sessionId, int limit) {
        List<ChatMessage> messages = getMessageStore().findBySessionId(sessionId);
        if (messages.size() > limit) {
            return messages.subList(messages.size() - limit, messages.size());
        }
        return messages;
    }

    /**
     * 构建会话摘要
     *
     * @param sessionId 会话 ID
     * @return 会话摘要信息
     */
    public java.util.Map<String, Object> buildSessionSummary(String sessionId) {
        ChatSession session = getSessionStore().findById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        java.util.Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("sessionId", sessionId);
        summary.put("workspaceId", session.getWorkspaceId());
        summary.put("taskId", session.getTaskId());
        summary.put("status", session.getStatus());
        summary.put("participantCount", session.getParticipantIds() != null ? session.getParticipantIds().size() : 0);
        summary.put("participantStates", session.getParticipantStates());
        summary.put("taskStates", session.getTaskStates());
        summary.put("blockedCount", session.getBlockedParticipants() != null ? session.getBlockedParticipants().size() : 0);
        summary.put("lastSummary", session.getLastSummary());
        summary.put("updatedAt", session.getUpdatedAt());
        return summary;
    }
}
