package org.dragon.memory.core;

import java.util.List;

/**
 * 会话记忆服务接口
 * 负责管理会话短期记忆，包括会话摘要、事件记录和检查点管理
 *
 * @author binarytom
 * @version 1.0
 */
public interface SessionMemoryService {
    SessionSnapshot start(String sessionId, String workspaceId, String characterId);

    SessionSnapshot update(String sessionId, SessionSnapshot snapshot);

    SessionSnapshot get(String sessionId);

    void checkpoint(String sessionId);

    List<MemoryEntry> extractCandidates(String sessionId);

    List<MemoryEntry> promote(String sessionId);

    void close(String sessionId);

    /**
     * 手动向 session 追加一条记忆条目
     *
     * @param sessionId session ID
     * @param entry     要追加的记忆条目
     */
    void appendEntry(String sessionId, MemoryEntry entry);

    /**
     * 向 session 写入一条事件记录
     *
     * @param sessionId session ID
     * @param event     事件内容
     */
    void appendEvent(String sessionId, String event);
}