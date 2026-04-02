package org.dragon.memory.memv2.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 记忆搜索结果类
 * 表示记忆检索的结果，包含记忆条目、分数和匹配原因
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorySearchResult {
    /**
     * 匹配的记忆条目
     */
    private MemoryEntry memory;

    /**
     * 匹配分数
     */
    private double score;

    /**
     * 匹配原因
     */
    private String reason;
}
