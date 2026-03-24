package org.dragon.observer.collector.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MemoryObservationSnapshot Memory 观测快照
 * 记录 Memory 的状态、存储和检索数据
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryObservationSnapshot {

    /**
     * Memory ID
     */
    private String memoryId;

    /**
     * Memory 类型
     */
    private String memoryType;

    /**
     * 状态
     */
    private String status;

    /**
     * 记录总数
     */
    private int totalRecordCount;

    /**
     * 活跃记录数
     */
    private int activeRecordCount;

    /**
     * 归档记录数
     */
    private int archivedRecordCount;

    /**
     * 存储大小（字节）
     */
    private long storageSizeBytes;

    /**
     * 检索次数
     */
    private int retrievalCount;

    /**
     * 检索命中率
     */
    private double retrievalHitRate;

    /**
     * 平均检索延迟（毫秒）
     */
    private double avgRetrievalLatencyMs;

    /**
     * 最后检索时间
     */
    private LocalDateTime lastRetrievalTime;

    /**
     * 最后写入时间
     */
    private LocalDateTime lastWriteTime;

    /**
     * 扩展数据
     */
    private Map<String, Object> extensions;

    /**
     * 快照时间
     */
    private LocalDateTime snapshotTime;
}