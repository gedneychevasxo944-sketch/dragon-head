package org.dragon.api.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 记忆文件 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryFileDTO {
    /**
     * 文件唯一标识符
     */
    private String id;

    /**
     * 所属数据源 ID
     */
    private String sourceId;

    /**
     * 文件标题
     */
    private String title;

    /**
     * 文件描述
     */
    private String description;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件类型：markdown/json/text/other
     */
    private String fileType;

    /**
     * 包含的片段数量
     */
    private int chunkCount;

    /**
     * 文件大小（字节）
     */
    private long totalSize;

    /**
     * 同步状态：synced/syncing/pending/failed/disabled
     */
    private String syncStatus;

    /**
     * 健康状态：healthy/warning/error/unknown
     */
    private String healthStatus;

    /**
     * 与 Character/Workspace 的绑定关系
     */
    private List<BindingDTO> bindings;

    /**
     * 最后同步时间
     */
    private Instant lastSyncAt;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;
}
