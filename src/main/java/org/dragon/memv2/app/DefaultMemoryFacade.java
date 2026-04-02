package org.dragon.memv2.app;

import org.dragon.memv2.core.MemoryEntry;
import org.dragon.memv2.core.MemoryFacade;
import org.dragon.memv2.core.MemoryQuery;
import org.dragon.memv2.core.MemorySearchResult;
import org.dragon.memv2.core.SessionSnapshot;
import org.dragon.memv2.core.CharacterMemoryService;
import org.dragon.memv2.core.WorkspaceMemoryService;
import org.dragon.memv2.core.SessionMemoryService;
import org.dragon.memv2.core.MemoryRecallService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 记忆系统统一接口实现类
 * 提供上层访问记忆系统的统一入口，屏蔽底层复杂性
 *
 * @author wyj
 * @version 1.0
 */
@Service
public class DefaultMemoryFacade implements MemoryFacade {
    private final CharacterMemoryService characterMemoryService;
    private final WorkspaceMemoryService workspaceMemoryService;
    private final SessionMemoryService sessionMemoryService;
    private final MemoryRecallService memoryRecallService;

    public DefaultMemoryFacade(CharacterMemoryService characterMemoryService,
                               WorkspaceMemoryService workspaceMemoryService,
                               SessionMemoryService sessionMemoryService,
                               MemoryRecallService memoryRecallService) {
        this.characterMemoryService = characterMemoryService;
        this.workspaceMemoryService = workspaceMemoryService;
        this.sessionMemoryService = sessionMemoryService;
        this.memoryRecallService = memoryRecallService;
    }

    @Override
    public MemoryEntry saveCharacterMemory(String characterId, MemoryEntry entry) {
        return characterMemoryService.create(characterId, entry);
    }

    @Override
    public MemoryEntry saveWorkspaceMemory(String workspaceId, MemoryEntry entry) {
        return workspaceMemoryService.create(workspaceId, entry);
    }

    @Override
    public SessionSnapshot updateSession(String sessionId, SessionSnapshot snapshot) {
        return sessionMemoryService.update(sessionId, snapshot);
    }

    @Override
    public List<MemoryEntry> flushSessionToLongTerm(String sessionId) {
        return sessionMemoryService.promote(sessionId);
    }

    @Override
    public List<MemorySearchResult> recall(MemoryQuery query) {
        return memoryRecallService.recallComposite(query);
    }
}
