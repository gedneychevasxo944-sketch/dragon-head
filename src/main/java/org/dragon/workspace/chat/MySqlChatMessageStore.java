package org.dragon.workspace.chat;

import io.ebean.Database;
import org.dragon.datasource.entity.ChatMessageEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MySqlChatMessageStore 聊天消息MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlChatMessageStore implements ChatMessageStore {

    private final Database mysqlDb;

    public MySqlChatMessageStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(ChatMessage message) {
        ChatMessageEntity entity = ChatMessageEntity.fromChatMessage(message);
        mysqlDb.save(entity);
    }

    @Override
    public ChatMessage findById(String messageId) {
        ChatMessageEntity entity = mysqlDb.find(ChatMessageEntity.class, messageId);
        return entity != null ? entity.toChatMessage() : null;
    }

    @Override
    public List<ChatMessage> findByWorkspaceId(String workspaceId, int limit) {
        return mysqlDb.find(ChatMessageEntity.class)
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
        return mysqlDb.find(ChatMessageEntity.class)
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
        return mysqlDb.find(ChatMessageEntity.class)
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
        return mysqlDb.find(ChatMessageEntity.class)
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
        return mysqlDb.find(ChatMessageEntity.class)
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
        ChatMessageEntity entity = mysqlDb.find(ChatMessageEntity.class, messageId);
        if (entity != null) {
            entity.setRead(true);
            mysqlDb.update(entity);
        }
    }

    @Override
    public void delete(String messageId) {
        mysqlDb.delete(ChatMessageEntity.class, messageId);
    }

    @Override
    public void deleteByWorkspaceId(String workspaceId) {
        mysqlDb.find(ChatMessageEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .delete();
    }

    @Override
    public List<ChatMessage> findByTaskId(String taskId) {
        return mysqlDb.find(ChatMessageEntity.class)
                .where()
                .eq("taskId", taskId)
                .orderBy()
                .asc("timestamp")
                .findList()
                .stream()
                .map(ChatMessageEntity::toChatMessage)
                .collect(Collectors.toList());
    }
}