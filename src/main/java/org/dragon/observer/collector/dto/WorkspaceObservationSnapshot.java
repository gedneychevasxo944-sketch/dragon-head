package org.dragon.observer.collector.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WorkspaceObservationSnapshot Workspace 观测快照
 * 记录 Workspace 的状态、资源使用和性能数据
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceObservationSnapshot {

    /**
     * Workspace ID
     */
    private String workspaceId;

    /**
     * Workspace 名称
     */
    private String name;

    /**
     * 状态
     */
    private String status;

    /**
     * Character 数量
     */
    private int characterCount;

    /**
     * 活跃 Character 数量
     */
    private int activeCharacterCount;

    /**
     * 任务总数
     */
    private int totalTaskCount;

    /**
     * 活跃任务数
     */
    private int activeTaskCount;

    /**
     * 完成任务数
     */
    private int completedTaskCount;

    /**
     * 失败任务数
     */
    private int failedTaskCount;

    /**
     * 成功率
     */
    private double successRate;

    /**
     * 平均任务执行时长（秒）
     */
    private double avgTaskDurationSeconds;

    /**
     * 物料数量
     */
    private int materialCount;

    /**
     * 内存占用（字节）
     */
    private long memoryUsageBytes;

    /**
     * 扩展数据
     */
    private Map<String, Object> extensions;

    /**
     * 快照时间
     */
    private LocalDateTime snapshotTime;
}