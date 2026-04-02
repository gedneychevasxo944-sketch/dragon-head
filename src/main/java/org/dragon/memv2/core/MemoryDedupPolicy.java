package org.dragon.memv2.core;

import java.util.List;
import java.util.Optional;

/**
 * 记忆去重策略接口
 * 负责处理记忆的去重逻辑
 *
 * @author binarytom
 * @version 1.0
 */
public interface MemoryDedupPolicy {
    /**
     * 查找重复的记忆条目
     *
     * @param candidate 候选记忆条目
     * @param existing 已存在的记忆条目列表
     * @return 找到的重复记忆条目，如未找到则返回Optional.empty()
     */
    Optional<MemoryEntry> findDuplicate(MemoryEntry candidate, List<MemoryEntry> existing);

    /**
     * 合并重复的记忆条目
     *
     * @param existing 已存在的记忆条目
     * @param candidate 候选记忆条目
     * @return 合并后的记忆条目
     */
    MemoryEntry merge(MemoryEntry existing, MemoryEntry candidate);
}
