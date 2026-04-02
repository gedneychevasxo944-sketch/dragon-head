package org.dragon.memory.memv2.app;

import org.dragon.memory.memv2.core.MemoryEntry;
import org.dragon.memory.memv2.core.SessionSnapshot;
import org.dragon.memory.memv2.core.SessionMemoryService;
import org.dragon.memory.memv2.storage.repo.SessionMemoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 会话记忆服务实现类
 * 负责管理会话短期记忆，包括会话摘要、事件记录和检查点管理
 *
 * @author wyj
 * @version 1.0
 */
@Service
public class DefaultSessionMemoryService implements SessionMemoryService {
    private final SessionMemoryRepository sessionMemoryRepository;

    public DefaultSessionMemoryService(SessionMemoryRepository sessionMemoryRepository) {
        this.sessionMemoryRepository = sessionMemoryRepository;
    }

    @Override
    public SessionSnapshot start(String sessionId, String workspaceId, String characterId) {
        return sessionMemoryRepository.create(sessionId, workspaceId, characterId);
    }

    @Override
    public SessionSnapshot update(String sessionId, SessionSnapshot snapshot) {
        return sessionMemoryRepository.update(sessionId, snapshot);
    }

    @Override
    public SessionSnapshot get(String sessionId) {
        return sessionMemoryRepository.get(sessionId).orElse(null);
    }

    @Override
    public void checkpoint(String sessionId) {
        SessionSnapshot snapshot = get(sessionId);
        if (snapshot != null) {
            sessionMemoryRepository.checkpoint(sessionId, snapshot);
        }
    }

    @Override
    public List<MemoryEntry> extractCandidates(String sessionId) {
        // 简化实现：实际应用中需要根据会话内容提取记忆候选
        return List.of();
    }

    @Override
    public List<MemoryEntry> promote(String sessionId) {
        // 简化实现：实际应用中需要将提取的候选记忆提升到长期记忆
        return List.of();
    }

    @Override
    public void close(String sessionId) {
        checkpoint(sessionId);
        // 简化实现：实际应用中可能需要清理会话资源
    }
}
