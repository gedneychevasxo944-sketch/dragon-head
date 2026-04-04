package org.dragon.datasource.entity;

import io.ebean.annotation.DbJson;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.dragon.observer.Observer;

/**
 * ObserverEntity Observer实体
 * 映射数据库 observer 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "observer")
public class ObserverEntity {

    @Id
    private String id;

    private String name;

    private String description;

    @Column(name = "workspace_id")
    private String workspaceId;

    private String status;

    @Column(name = "evaluation_mode")
    private String evaluationMode;

    @Column(name = "optimization_threshold")
    private Double optimizationThreshold;

    @Column(name = "consecutive_low_score_threshold")
    private Integer consecutiveLowScoreThreshold;

    @Column(name = "common_sense_enabled")
    private Boolean commonSenseEnabled;

    @Column(name = "auto_optimization_enabled")
    private Boolean autoOptimizationEnabled;

    @Column(name = "periodic_evaluation_hours")
    private Integer periodicEvaluationHours;

    @DbJson
    private Map<String, Object> properties;

    @Column(name = "planner_character_ids")
    @DbJson
    private List<String> plannerCharacterIds;

    @Column(name = "reviewer_character_ids")
    @DbJson
    private List<String> reviewerCharacterIds;

    @Column(name = "supported_target_types")
    @DbJson
    private List<String> supportedTargetTypes;

    @Column(name = "manual_approval_required")
    private Boolean manualApprovalRequired;

    @Column(name = "schedule_cron")
    private String scheduleCron;

    @Column(name = "plan_window_hours")
    private Integer planWindowHours;

    @Column(name = "max_plan_items")
    private Integer maxPlanItems;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 转换为Observer
     */
    public Observer toObserver() {
        return Observer.builder()
                .id(this.id)
                .name(this.name)
                .description(this.description)
                .workspaceId(this.workspaceId)
                .status(this.status != null ? Observer.Status.valueOf(this.status) : Observer.Status.INACTIVE)
                .evaluationMode(this.evaluationMode != null ? Observer.EvaluationMode.valueOf(this.evaluationMode) : null)
                .optimizationThreshold(this.optimizationThreshold)
                .consecutiveLowScoreThreshold(this.consecutiveLowScoreThreshold)
                .commonSenseEnabled(this.commonSenseEnabled)
                .autoOptimizationEnabled(this.autoOptimizationEnabled)
                .periodicEvaluationHours(this.periodicEvaluationHours)
                .properties(this.properties)
                .plannerCharacterIds(this.plannerCharacterIds)
                .reviewerCharacterIds(this.reviewerCharacterIds)
                .supportedTargetTypes(this.supportedTargetTypes)
                .manualApprovalRequired(this.manualApprovalRequired)
                .scheduleCron(this.scheduleCron)
                .planWindowHours(this.planWindowHours)
                .maxPlanItems(this.maxPlanItems)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    /**
     * 从Observer创建Entity
     */
    public static ObserverEntity fromObserver(Observer observer) {
        return ObserverEntity.builder()
                .id(observer.getId())
                .name(observer.getName())
                .description(observer.getDescription())
                .workspaceId(observer.getWorkspaceId())
                .status(observer.getStatus() != null ? observer.getStatus().name() : null)
                .evaluationMode(observer.getEvaluationMode() != null ? observer.getEvaluationMode().name() : null)
                .optimizationThreshold(observer.getOptimizationThreshold())
                .consecutiveLowScoreThreshold(observer.getConsecutiveLowScoreThreshold())
                .commonSenseEnabled(observer.getCommonSenseEnabled())
                .autoOptimizationEnabled(observer.getAutoOptimizationEnabled())
                .periodicEvaluationHours(observer.getPeriodicEvaluationHours())
                .properties(observer.getProperties())
                .plannerCharacterIds(observer.getPlannerCharacterIds())
                .reviewerCharacterIds(observer.getReviewerCharacterIds())
                .supportedTargetTypes(observer.getSupportedTargetTypes())
                .manualApprovalRequired(observer.getManualApprovalRequired())
                .scheduleCron(observer.getScheduleCron())
                .planWindowHours(observer.getPlanWindowHours())
                .maxPlanItems(observer.getMaxPlanItems())
                .createdAt(observer.getCreatedAt())
                .updatedAt(observer.getUpdatedAt())
                .build();
    }
}