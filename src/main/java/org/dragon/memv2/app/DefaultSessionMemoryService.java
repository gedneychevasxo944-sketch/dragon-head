package org.dragon.memv2.app;

import org.dragon.memv2.core.MemoryEntry;
import org.dragon.memv2.core.SessionSnapshot;
import org.dragon.memv2.core.SessionMemoryService;
import org.dragon.memv2.core.MemoryExtractionService;
import org.dragon.memv2.storage.repo.SessionMemoryRepository;
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
    private final MemoryExtractionService memoryExtractionService;

    public DefaultSessionMemoryService(SessionMemoryRepository sessionMemoryRepository,
                                       MemoryExtractionService memoryExtractionService) {
        this.sessionMemoryRepository = sessionMemoryRepository;
        this.memoryExtractionService = memoryExtractionService;
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
        SessionSnapshot snapshot = get(sessionId);
        if (snapshot == null) {
            return List.of();
        }

        List<String> events = sessionMemoryRepository.listEvents(sessionId);
        return memoryExtractionService.extract(snapshot, events);
    }

    @Override
    public List<MemoryEntry> promote(String sessionId) {
        SessionSnapshot snapshot = get(sessionId);
        if (snapshot == null) {
            return List.of();
        }

        List<String> events = sessionMemoryRepository.listEvents(sessionId);
        return memoryExtractionService.promote(sessionId, snapshot, events);
    }

    @Override
    public void close(String sessionId) {
        checkpoint(sessionId);
        // 清理会话资源
        sessionMemoryRepository.clear(sessionId);
    }
}
