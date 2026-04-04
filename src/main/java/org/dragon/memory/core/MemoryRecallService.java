package org.dragon.memory.core;

import java.util.List;

/**
 * 记忆检索服务接口
 * 负责从不同作用域（角色、工作空间、会话）中检索和召回相关记忆
 *
 * @author binarytom
 * @version 1.0
 */
public interface MemoryRecallService {
    List<MemorySearchResult> recallCharacter(String characterId, String query, int limit);

    List<MemorySearchResult> recallWorkspace(String workspaceId, String query, int limit);

    List<MemorySearchResult> recallSession(String sessionId, String query, int limit);

    List<MemorySearchResult> recallComposite(MemoryQuery query);
}
