package org.dragon.datasource.entity;

import io.ebean.annotation.DbJson;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.dragon.observer.optimization.plan.OptimizationAction;
import org.dragon.observer.optimization.plan.OptimizationPlan;
import org.dragon.observer.optimization.plan.OptimizationPlanItem;

/**
 * OptimizationPlanEntity 优化计划实体
 * 映射数据库 optimization_plan 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "optimization_plan")
public class OptimizationPlanEntity {

    @Id
    private String id;

    private String observerId;

    private String evaluationId;

    private String targetType;

    private String targetId;

    private String status;

    private String title;

    private String summary;

    private String rawContent;

    @DbJson
    @Builder.Default
    private List<OptimizationPlanItem> items = new ArrayList<>();

    private String approver;

    private LocalDateTime approvedAt;

    private String approvalComment;

    private LocalDateTime executedAt;

    private LocalDateTime completedAt;

    private String executionSummary;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 转换为OptimizationPlan
     */
    public OptimizationPlan toOptimizationPlan() {
        return OptimizationPlan.builder()
                .id(this.id)
                .observerId(this.observerId)
                .evaluationId(this.evaluationId)
                .targetType(this.targetType != null ? OptimizationAction.TargetType.valueOf(this.targetType) : null)
                .targetId(this.targetId)
                .status(this.status != null ? OptimizationPlan.Status.valueOf(this.status) : null)
                .title(this.title)
                .summary(this.summary)
                .rawContent(this.rawContent)
                .items(this.items)
                .approver(this.approver)
                .approvedAt(this.approvedAt)
                .approvalComment(this.approvalComment)
                .executedAt(this.executedAt)
                .completedAt(this.completedAt)
                .executionSummary(this.executionSummary)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    /**
     * 从OptimizationPlan创建Entity
     */
    public static OptimizationPlanEntity fromOptimizationPlan(OptimizationPlan plan) {
        return OptimizationPlanEntity.builder()
                .id(plan.getId())
                .observerId(plan.getObserverId())
                .evaluationId(plan.getEvaluationId())
                .targetType(plan.getTargetType() != null ? plan.getTargetType().name() : null)
                .targetId(plan.getTargetId())
                .status(plan.getStatus() != null ? plan.getStatus().name() : null)
                .title(plan.getTitle())
                .summary(plan.getSummary())
                .rawContent(plan.getRawContent())
                .items(plan.getItems())
                .approver(plan.getApprover())
                .approvedAt(plan.getApprovedAt())
                .approvalComment(plan.getApprovalComment())
                .executedAt(plan.getExecutedAt())
                .completedAt(plan.getCompletedAt())
                .executionSummary(plan.getExecutionSummary())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}
