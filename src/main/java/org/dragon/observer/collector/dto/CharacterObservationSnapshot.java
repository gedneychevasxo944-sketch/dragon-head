package org.dragon.observer.collector.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CharacterObservationSnapshot Character 观测快照
 * 记录 Character 的状态、行为和性能数据
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterObservationSnapshot {

    /**
     * Character ID
     */
    private String characterId;

    /**
     * Character 名称
     */
    private String name;

    /**
     * 角色类型
     */
    private String roleType;

    /**
     * 当前状态
     */
    private String status;

    /**
     * 当前执行的任务数
     */
    private int activeTaskCount;

    /**
     * 累计完成任务数
     */
    private int completedTaskCount;

    /**
     * 累计失败任务数
     */
    private int failedTaskCount;

    /**
     * 平均任务执行时长（秒）
     */
    private double avgTaskDurationSeconds;

    /**
     * 成功率
     */
    private double successRate;

    /**
     * LLM 调用次数
     */
    private int llmCallCount;

    /**
     * LLM 调用失败次数
     */
    private int llmCallFailCount;

    /**
     * 平均响应时间（毫秒）
     */
    private double avgResponseTimeMs;

    /**
     * 技能标签
     */
    private java.util.List<String> skillTags;

    /**
     * 个性化描述
     */
    private String personalityDescription;

    /**
     * 心智状态
     */
    private Map<String, Object> mindState;

    /**
     * 扩展数据
     */
    private Map<String, Object> extensions;

    /**
     * 快照时间
     */
    private LocalDateTime snapshotTime;
}