package org.dragon.api.controller.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 记忆条目DTO类
 * 表示一条完整的记忆内容，包含元数据和正文
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryEntryDTO {
    /**
     * 记忆ID
     */
    private String id;

    /**
     * 记忆标题
     */
    private String title;

    /**
     * 记忆描述
     */
    private String description;

    /**
     * 记忆类型
     */
    private String type;

    /**
     * 记忆作用域
     */
    private String scope;

    /**
     * 所属ID（角色ID/工作空间ID/会话ID）
     */
    private String ownerId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 记忆内容
     */
    private String content;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;

    /**
     * 标签
     */
    @Builder.Default
    private Map<String, String> tags = new HashMap<>();
}
