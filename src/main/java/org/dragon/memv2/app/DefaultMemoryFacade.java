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
import org.dragon.memv2.core.AgentMemoryContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 记忆系统统一接口实现类
 * 提供上层访问记忆系统的统一入口，屏蔽底层复杂性
 *
 * @author binarytom
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
    public Optional<MemoryEntry> getCharacterMemory(String characterId, MemoryId memoryId) {
        return characterMemoryService.get(characterId, memoryId);
    }

    @Override
    public Optional<MemoryEntry> getWorkspaceMemory(String workspaceId, MemoryId memoryId) {
        return workspaceMemoryService.get(workspaceId, memoryId);
    }

    @Override
    public List<MemoryEntry> listCharacterMemories(String characterId) {
        return characterMemoryService.list(characterId);
    }

    @Override
    public List<MemoryEntry> listWorkspaceMemories(String workspaceId) {
        return workspaceMemoryService.list(workspaceId);
    }

    @Override
    public void deleteCharacterMemory(String characterId, MemoryId memoryId) {
        characterMemoryService.delete(characterId, memoryId);
    }

    @Override
    public void deleteWorkspaceMemory(String workspaceId, MemoryId memoryId) {
        workspaceMemoryService.delete(workspaceId, memoryId);
    }

    @Override
    public SessionSnapshot startSession(String sessionId, String workspaceId, String characterId) {
        return sessionMemoryService.start(sessionId, workspaceId, characterId);
    }

    @Override
    public SessionSnapshot updateSession(String sessionId, SessionSnapshot snapshot) {
        return sessionMemoryService.update(sessionId, snapshot);
    }

    @Override
    public SessionSnapshot getSession(String sessionId) {
        return sessionMemoryService.get(sessionId);
    }

    @Override
    public void checkpointSession(String sessionId) {
        sessionMemoryService.checkpoint(sessionId);
    }

    @Override
    public List<MemoryEntry> flushSessionToLongTerm(String sessionId) {
        return sessionMemoryService.promote(sessionId);
    }

    @Override
    public void closeSession(String sessionId) {
        sessionMemoryService.close(sessionId);
    }

    @Override
    public List<MemorySearchResult> recall(MemoryQuery query) {
        return memoryRecallService.recallComposite(query);
    }

    @Override
    public List<MemorySearchResult> recallForAgent(AgentMemoryContext context, MemoryQuery query) {
        // 使用代理上下文信息丰富查询条件
        if (context.getCharacterId() != null) {
            query.setCharacterId(context.getCharacterId());
        }
        if (context.getWorkspaceId() != null) {
            query.setWorkspaceId(context.getWorkspaceId());
        }
        if (context.getSessionId() != null) {
            query.setSessionId(context.getSessionId());
        }

        // 调用召回服务
        return memoryRecallService.recallComposite(query);
    }
}
