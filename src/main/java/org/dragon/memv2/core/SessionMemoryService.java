package org.dragon.memv2.core;

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
}
