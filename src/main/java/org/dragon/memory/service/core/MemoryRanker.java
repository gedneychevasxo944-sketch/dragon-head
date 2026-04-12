package org.dragon.memory.service.core;

import org.dragon.memory.entity.MemoryEntry;
import org.dragon.memory.entity.MemorySearchResult;

import java.util.List;

/**
 * 记忆排序服务接口
 * 负责对记忆检索结果进行排序
 *
 * @author binarytom
 * @version 1.0
 */
public interface MemoryRanker {
    /**
     * 对记忆检索结果进行排序
     *
     * @param query 查询内容
     * @param candidates 待排序的记忆条目
     * @param limit 结果数量限制
     * @return 排序后的记忆检索结果
     */
    List<MemorySearchResult> rank(String query, List<MemoryEntry> candidates, int limit);
}
