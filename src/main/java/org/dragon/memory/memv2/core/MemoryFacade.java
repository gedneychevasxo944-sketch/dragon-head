package org.dragon.memory.memv2.core;

import java.util.List;

/**
 * 记忆系统统一接口
 * 提供上层访问记忆系统的统一入口，屏蔽底层复杂性
 *
 * @author wyj
 * @version 1.0
 */
public interface MemoryFacade {
    MemoryEntry saveCharacterMemory(String characterId, MemoryEntry entry);

    MemoryEntry saveWorkspaceMemory(String workspaceId, MemoryEntry entry);

    SessionSnapshot updateSession(String sessionId, SessionSnapshot snapshot);

    List<MemoryEntry> flushSessionToLongTerm(String sessionId);

    List<MemorySearchResult> recall(MemoryQuery query);
}
