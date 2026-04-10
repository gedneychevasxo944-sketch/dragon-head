package org.dragon.workspace.cooperation.chat;

import io.ebean.Database;
import org.dragon.datasource.entity.ChatSessionEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MySqlChatSessionStore 聊天会话MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlChatSessionStore implements ChatSessionStore {

    private final Database mysqlDb;

    public MySqlChatSessionStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(ChatSession session) {
        ChatSessionEntity entity = ChatSessionEntity.fromChatSession(session);
        mysqlDb.save(entity);
    }

    @Override
    public ChatSession findById(String sessionId) {
        ChatSessionEntity entity = mysqlDb.find(ChatSessionEntity.class, sessionId);
        return entity != null ? entity.toChatSession() : null;
    }

    @Override
    public List<ChatSession> findByWorkspaceId(String workspaceId) {
        return mysqlDb.find(ChatSessionEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .orderBy()
                .desc("createdAt")
                .findList()
                .stream()
                .map(ChatSessionEntity::toChatSession)
                .collect(Collectors.toList());
    }

    @Override
    public ChatSession findByTaskId(String taskId) {
        ChatSessionEntity entity = mysqlDb.find(ChatSessionEntity.class)
                .where()
                .eq("taskId", taskId)
                .findOne();
        return entity != null ? entity.toChatSession() : null;
    }

    @Override
    public List<ChatSession> findActiveByWorkspaceId(String workspaceId) {
        return mysqlDb.find(ChatSessionEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .eq("status", ChatSession.Status.ACTIVE.name())
                .orderBy()
                .desc("updatedAt")
                .findList()
                .stream()
                .map(ChatSessionEntity::toChatSession)
                .collect(Collectors.toList());
    }

    @Override
    public void update(ChatSession session) {
        ChatSessionEntity entity = ChatSessionEntity.fromChatSession(session);
        mysqlDb.update(entity);
    }

    @Override
    public void delete(String sessionId) {
        mysqlDb.delete(ChatSessionEntity.class, sessionId);
    }
}
