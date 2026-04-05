package org.dragon.api.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 全局统计信息 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryOverviewStatsDTO {
    /**
     * 角色记忆总数
     */
    private int totalCharacterMemories;

    /**
     * 工作空间记忆总数
     */
    private int totalWorkspaceMemories;

    /**
     * 知识项总数
     */
    private int totalKnowledgeItems;

    /**
     * 总片段数
     */
    private int totalChunks;

    /**
     * 健康状态数量
     */
    private int healthyCount;

    /**
     * 警告状态数量
     */
    private int warningCount;

    /**
     * 错误状态数量
     */
    private int errorCount;

    /**
     * 最后同步时间
     */
    private String lastSyncOverall;

    /**
     * 是否启用嵌入功能
     */
    private boolean embeddingAvailable;

    /**
     * 是否启用向量功能
     */
    private boolean vectorAvailable;

    /**
     * 异常实例列表
     */
    private List<AbnormalMemoryDTO> abnormalInstances;
}
