package org.dragon.observer;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Observer 观察者实体
 * 负责监控、评估 Workspace 内的任务执行质量，触发优化动作，并维护常识库
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Observer {

    /**
     * Observer 状态
     */
    public enum Status {
        ACTIVE,    // 活跃
        INACTIVE,  // 未激活
        PAUSED     // 暂停
    }

    /**
     * Observer 全局唯一标识
     */
    private String id;

    /**
     * Observer 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 关联的 Workspace ID
     * Observer 可以绑定到特定 Workspace，也可以为 null 表示全局 Observer
     */
    private String workspaceId;

    /**
     * Observer 状态
     */
    @Builder.Default
    private Status status = Status.INACTIVE;

    /**
     * 评价模式
     */
    private EvaluationMode evaluationMode;

    /**
     * 优化触发阈值
     * 当评分低于此阈值时触发优化
     */
    @Builder.Default
    private double optimizationThreshold = 0.6;

    /**
     * 连续低分触发优化的次数阈值
     */
    @Builder.Default
    private int consecutiveLowScoreThreshold = 3;

    /**
     * 是否启用常识校验
     */
    @Builder.Default
    private boolean commonSenseEnabled = true;

    /**
     * 是否启用自动优化
     */
    @Builder.Default
    private boolean autoOptimizationEnabled = true;

    /**
     * 周期性评价周期（小时）
     * 0 表示不启用周期性评价
     */
    @Builder.Default
    private int periodicEvaluationHours = 24;

    /**
     * 扩展属性
     */
    private Map<String, Object> properties;

    /**
     * Planner Character ID 列表
     * 用于生成优化计划的 Character
     */
    private java.util.List<String> plannerCharacterIds;

    /**
     * Reviewer Character ID 列表
     * 用于复核优化计划的 Character
     */
    private java.util.List<String> reviewerCharacterIds;

    /**
     * 支持的目标类型
     */
    private java.util.List<String> supportedTargetTypes;

    /**
     * 是否需要人工确认
     * 默认 true - 生成的计划需要人工确认后执行
     */
    @Builder.Default
    private boolean manualApprovalRequired = true;

    /**
     * 定时 cron 表达式
     * 用于定时触发评价和计划生成
     */
    private String scheduleCron;

    /**
     * 计划时间窗口（小时）
     * 用于限定收集数据的时间范围
     */
    @Builder.Default
    private int planWindowHours = 24;

    /**
     * 最大计划项数量
     * 限制单次计划生成的最大条目数
     */
    @Builder.Default
    private int maxPlanItems = 50;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 评价模式枚举
     */
    public enum EvaluationMode {
        RULE_BASED,      // 基于规则的评价
        MODEL_DRIVEN,    // 模型驱动的评价
        HYBRID,          // 混合模式
        MANUAL           // 人工审核
    }

    /**
     * 激活 Observer
     */
    public void activate() {
        this.status = Status.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 暂停 Observer
     */
    public void pause() {
        this.status = Status.PAUSED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 停用 Observer
     */
    public void deactivate() {
        this.status = Status.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查是否处于活跃状态
     */
    public boolean isActive() {
        return status == Status.ACTIVE;
    }
}
