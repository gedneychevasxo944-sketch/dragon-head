package org.dragon.memv2.app;

import org.dragon.memv2.core.MemoryQuery;
import org.dragon.memv2.core.MemorySearchResult;
import org.dragon.memv2.core.MemoryRecallService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 记忆检索服务实现类
 * 负责从不同作用域（角色、工作空间、会话）中检索和召回相关记忆
 *
 * @author wyj
 * @version 1.0
 */
@Service
public class DefaultMemoryRecallService implements MemoryRecallService {
    @Override
    public List<MemorySearchResult> recallCharacter(String characterId, String query, int limit) {
        // 简化实现：实际应用中需要实现字符记忆检索
        return List.of();
    }

    @Override
    public List<MemorySearchResult> recallWorkspace(String workspaceId, String query, int limit) {
        // 简化实现：实际应用中需要实现工作空间记忆检索
        return List.of();
    }

    @Override
    public List<MemorySearchResult> recallComposite(MemoryQuery query) {
        // 简化实现：实际应用中需要实现综合记忆检索
        return List.of();
    }
}
