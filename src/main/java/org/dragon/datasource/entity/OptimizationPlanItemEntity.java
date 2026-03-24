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

import org.dragon.observer.optimization.OptimizationPlanItem;
import org.dragon.observer.optimization.OptimizationAction;

/**
 * OptimizationPlanItemEntity 优化计划项目实体
 * 映射数据库 optimization_plan_item 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "optimization_plan_item")
public class OptimizationPlanItemEntity {

    @Id
    private String id;

    private String planId;

    private int sequence;

    private String actionType;

    private String targetId;

    private String description;

    private String parameters;

    private String status;

    private String actionId;

    private LocalDateTime executedAt;

    private LocalDateTime completedAt;

    private String result;

    private LocalDateTime rolledBackAt;

    private String rollbackResult;

    private LocalDateTime createdAt;

    /**
     * 转换为OptimizationPlanItem
     */
    public OptimizationPlanItem toOptimizationPlanItem() {
        return OptimizationPlanItem.builder()
                .id(this.id)
                .planId(this.planId)
                .sequence(this.sequence)
                .actionType(this.actionType != null ? OptimizationAction.ActionType.valueOf(this.actionType) : null)
                .targetId(this.targetId)
                .description(this.description)
                .parameters(this.parameters)
                .status(this.status != null ? OptimizationPlanItem.Status.valueOf(this.status) : null)
                .actionId(this.actionId)
                .executedAt(this.executedAt)
                .completedAt(this.completedAt)
                .result(this.result)
                .rolledBackAt(this.rolledBackAt)
                .rollbackResult(this.rollbackResult)
                .createdAt(this.createdAt)
                .build();
    }

    /**
     * 从OptimizationPlanItem创建Entity
     */
    public static OptimizationPlanItemEntity fromOptimizationPlanItem(OptimizationPlanItem item) {
        return OptimizationPlanItemEntity.builder()
                .id(item.getId())
                .planId(item.getPlanId())
                .sequence(item.getSequence())
                .actionType(item.getActionType() != null ? item.getActionType().name() : null)
                .targetId(item.getTargetId())
                .description(item.getDescription())
                .parameters(item.getParameters())
                .status(item.getStatus() != null ? item.getStatus().name() : null)
                .actionId(item.getActionId())
                .executedAt(item.getExecutedAt())
                .completedAt(item.getCompletedAt())
                .result(item.getResult())
                .rolledBackAt(item.getRolledBackAt())
                .rollbackResult(item.getRollbackResult())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
