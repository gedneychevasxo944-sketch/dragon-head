package org.dragon.api.controller.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 运行时状态 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuntimeStatusDTO {
    /**
     * 后端连接状态：connected/disconnected/error
     */
    private String backend;

    /**
     * 提供者
     */
    private String provider;

    /**
     * 模型
     */
    private String model;

    /**
     * 是否启用降级
     */
    private boolean fallbackEnabled;

    /**
     * 总文件数
     */
    private int totalFiles;

    /**
     * 总片段数
     */
    private int totalChunks;

    /**
     * 脏数据数
     */
    private int dirtyCount;

    /**
     * 缓存状态：active/inactive/error
     */
    private String cacheStatus;

    /**
     * 全文搜索状态：active/inactive/error
     */
    private String ftsStatus;

    /**
     * 向量搜索状态：active/inactive/error
     */
    private String vectorStatus;

    /**
     * 批处理状态：idle/processing/error
     */
    private String batchStatus;

    /**
     * 最后同步时间
     */
    private String lastSyncTime;

    /**
     * 最近失败信息
     */
    private List<String> recentFailures;
}
