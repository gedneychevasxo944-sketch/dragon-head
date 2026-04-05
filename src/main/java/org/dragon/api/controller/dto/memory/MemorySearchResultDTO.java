package org.dragon.api.controller.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 记忆搜索结果DTO类
 * 表示记忆检索的结果，包含记忆条目、分数和匹配原因
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorySearchResultDTO {
    /**
     * 匹配的记忆条目
     */
    private MemoryEntryDTO memory;

    /**
     * 匹配分数
     */
    private double score;

    /**
     * 匹配原因
     */
    private String reason;
}
