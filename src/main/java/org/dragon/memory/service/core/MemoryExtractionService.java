package org.dragon.memory.service.core;

import org.dragon.memory.entity.MemoryEntry;
import org.dragon.memory.entity.SessionSnapshot;

import java.util.List;

/**
 * 记忆提取服务接口
 * 负责从会话记忆中提取可长期保存的记忆候选，并进行固化处理
 *
 * @author binarytom
 * @version 1.0
 */
public interface MemoryExtractionService {
    List<MemoryEntry> extract(SessionSnapshot snapshot, List<String> events);

    List<MemoryEntry> promote(String sessionId, SessionSnapshot snapshot, List<String> events);
}
