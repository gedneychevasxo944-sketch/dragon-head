package org.dragon.memory.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 记忆索引条目类
 * 表示MEMORY.md索引文件中的一条记录，包含记忆导航信息但不包含正文内容
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryIndexItem {
    /**
     * 记忆ID
     */
    private MemoryId memoryId;

    /**
     * 记忆标题
     */
    private String title;

    /**
     * 相对路径
     */
    private String relativePath;

    /**
     * 摘要信息
     */
    private String summaryLine;

    /**
     * 记忆类型
     */
    private MemoryType type;

    /**
     * 更新时间
     */
    private Instant updatedAt;
}
