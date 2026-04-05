package org.dragon.api.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 数据源 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceDocumentDTO {
    /**
     * 数据源唯一标识符
     */
    private String id;

    /**
     * 数据源标题
     */
    private String title;

    /**
     * 数据源路径（文件系统路径或 URL）
     */
    private String sourcePath;

    /**
     * 数据源类型：file/url/api/chat/fused/character_memory/workspace_memory
     */
    private String sourceType;

    /**
     * 后端存储位置
     */
    private String backend;

    /**
     * 数据源提供者
     */
    private String provider;

    /**
     * 是否启用（默认 true）
     */
    private boolean enabled;

    /**
     * 同步状态：active/error/disabled/syncing
     */
    private String status;

    /**
     * 最后索引时间
     */
    private Instant lastIndexedAt;

    /**
     * 项目数量
     */
    private int itemCount;

    /**
     * 文件数量
     */
    private int fileCount;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 是否为融合数据源
     */
    private boolean isFusedSource;

    /**
     * 是否为内置数据源
     */
    private boolean isBuiltIn;

    /**
     * 记忆文件列表
     */
    private List<MemoryFileDTO> files;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;
}
