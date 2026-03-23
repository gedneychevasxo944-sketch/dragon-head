package org.dragon.workspace.chat;

import io.ebean.DB;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MySqlChatMessageStore 聊天消息MySQL存储实现
 */
@Component
public class MySqlChatMessageStore implements ChatMessageStore {

    @Override
    public void save(ChatMessage message) {
        ChatMessageEntity entity = ChatMessageEntity.fromChatMessage(message);
        DB.save(entity);
    }

    @Override
    public ChatMessage findById(String messageId) {
        ChatMessageEntity entity = DB.find(ChatMessageEntity.class, messageId);
        return entity != null ? entity.toChatMessage() : null;
    }

    @Override
    public List<ChatMessage> findByWorkspaceId(String workspaceId, int limit) {
        return DB.find(ChatMessageEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .orderBy()
                .desc("timestamp")
                .setMaxRows(limit)
                .findList()
                .stream()
                .map(ChatMessageEntity::toChatMessage)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessage> findByWorkspaceIdAndTimeRange(
            String workspaceId, LocalDateTime startTime, LocalDateTime endTime) {
        return DB.find(ChatMessageEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .ge("timestamp", startTime)
                .le("timestamp", endTime)
                .orderBy()
                .asc("timestamp")
                .findList()
                .stream()
                .map(ChatMessageEntity::toChatMessage)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessage> findByWorkspaceIdAndReceiverId(
            String workspaceId, String receiverId, int limit) {
        return DB.find(ChatMessageEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .eq("receiverId", receiverId)
                .orderBy()
                .desc("timestamp")
                .setMaxRows(limit)
                .findList()
                .stream()
                .map(ChatMessageEntity::toChatMessage)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessage> findByCharacterId(String characterId, int limit) {
        return DB.find(ChatMessageEntity.class)
                .where()
                .or()
                .eq("senderId", characterId)
                .eq("receiverId", characterId)
                .endOr()
                .orderBy()
                .desc("timestamp")
                .setMaxRows(limit)
                .findList()
                .stream()
                .map(ChatMessageEntity::toChatMessage)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessage> findBySessionId(String sessionId) {
        return DB.find(ChatMessageEntity.class)
                .where()
                .eq("sessionId", sessionId)
                .orderBy()
                .asc("timestamp")
                .findList()
                .stream()
                .map(ChatMessageEntity::toChatMessage)
                .collect(Collectors.toList());
    }

    @Override
    public void markAsRead(String messageId) {
        ChatMessageEntity entity = DB.find(ChatMessageEntity.class, messageId);
        if (entity != null) {
            entity.setRead(true);
            DB.update(entity);
        }
    }

    @Override
    public void delete(String messageId) {
        DB.delete(ChatMessageEntity.class, messageId);
    }

    @Override
    public void deleteByWorkspaceId(String workspaceId) {
        DB.find(ChatMessageEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .delete();
    }
}