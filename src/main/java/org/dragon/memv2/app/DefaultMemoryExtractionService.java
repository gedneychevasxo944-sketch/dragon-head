package org.dragon.memv2.app;

import org.dragon.memv2.core.MemoryEntry;
import org.dragon.memv2.core.SessionSnapshot;
import org.dragon.memv2.core.MemoryExtractionService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 记忆提取服务实现类
 * 负责从会话记忆中提取可长期保存的记忆候选，并进行固化处理
 *
 * @author wyj
 * @version 1.0
 */
@Service
public class DefaultMemoryExtractionService implements MemoryExtractionService {
    @Override
    public List<MemoryEntry> extract(SessionSnapshot snapshot, List<String> events) {
        // 简化实现：实际应用中需要实现记忆提取逻辑
        return List.of();
    }

    @Override
    public List<MemoryEntry> promote(String sessionId, SessionSnapshot snapshot, List<String> events) {
        // 简化实现：实际应用中需要实现记忆固化逻辑
        return List.of();
    }
}
