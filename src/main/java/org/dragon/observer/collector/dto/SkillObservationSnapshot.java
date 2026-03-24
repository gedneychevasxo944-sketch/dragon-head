package org.dragon.observer.collector.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SkillObservationSnapshot Skill 观测快照
 * 记录 Skill 的状态、使用和性能数据
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillObservationSnapshot {

    /**
     * Skill ID
     */
    private String skillId;

    /**
     * Skill 名称
     */
    private String name;

    /**
     * Skill 类型
     */
    private String skillType;

    /**
     * 状态
     */
    private String status;

    /**
     * 版本
     */
    private String version;

    /**
     * 被引用次数
     */
    private int referenceCount;

    /**
     * 使用次数
     */
    private int usageCount;

    /**
     * 成功率
     */
    private double successRate;

    /**
     * 平均执行时长（毫秒）
     */
    private double avgExecutionDurationMs;

    /**
     * 最后使用时间
     */
    private LocalDateTime lastUsedTime;

    /**
     * 所属 Character ID 列表
     */
    private java.util.List<String> ownerCharacterIds;

    /**
     * 标签
     */
    private java.util.List<String> tags;

    /**
     * 扩展数据
     */
    private Map<String, Object> extensions;

    /**
     * 快照时间
     */
    private LocalDateTime snapshotTime;
}