package org.dragon.memory.storage.repo;

import org.dragon.memory.core.SessionSnapshot;

import java.util.List;
import java.util.Optional;

/**
 * 会话记忆仓库接口
 * 负责管理会话短期记忆的增删改查和检查点维护
 *
 * @author binarytom
 * @version 1.0
 */
public interface SessionMemoryRepository {
    SessionSnapshot create(String sessionId, String workspaceId, String characterId);

    SessionSnapshot update(String sessionId, SessionSnapshot snapshot);

    Optional<SessionSnapshot> get(String sessionId);

    void appendEvent(String sessionId, String event);

    List<String> listEvents(String sessionId);

    void checkpoint(String sessionId, SessionSnapshot snapshot);

    void clear(String sessionId);
}
