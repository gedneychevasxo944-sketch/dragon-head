package org.dragon.memory.service.core;

import org.dragon.memory.entity.*;

import java.util.List;
import java.util.Optional;

/**
 * 记忆系统统一接口
 * 提供上层访问记忆系统的统一入口，屏蔽底层复杂性
 *
 * @author binarytom
 * @version 1.0
 */
public interface MemoryFacade {
    MemoryEntry saveCharacterMemory(String characterId, MemoryEntry entry);

    MemoryEntry saveWorkspaceMemory(String workspaceId, MemoryEntry entry);

    Optional<MemoryEntry> getCharacterMemory(String characterId, MemoryId memoryId);

    Optional<MemoryEntry> getWorkspaceMemory(String workspaceId, MemoryId memoryId);

    List<MemoryEntry> listCharacterMemories(String characterId);

    List<MemoryEntry> listWorkspaceMemories(String workspaceId);

    void deleteCharacterMemory(String characterId, MemoryId memoryId);

    void deleteWorkspaceMemory(String workspaceId, MemoryId memoryId);

    SessionSnapshot startSession(String sessionId, String workspaceId, String characterId);

    SessionSnapshot updateSession(String sessionId, SessionSnapshot snapshot);

    SessionSnapshot getSession(String sessionId);

    void checkpointSession(String sessionId);

    List<MemoryEntry> flushSessionToLongTerm(String sessionId);

    void closeSession(String sessionId);

    List<MemorySearchResult> recall(MemoryQuery query);

    /**
     * 为代理提供记忆召回功能
     *
     * @param context 代理记忆上下文
     * @param query 查询条件
     * @return 记忆搜索结果列表
     */
    List<MemorySearchResult> recallForAgent(AgentMemoryContext context, MemoryQuery query);
}
