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
import java.util.Map;

import org.dragon.observer.optimization.plan.OptimizationAction;

import java.util.Map;

/**
 * OptimizationActionEntity 优化动作实体
 * 映射数据库 optimization_action 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "optimization_action")
public class OptimizationActionEntity {

    @Id
    private String id;

    private String evaluationId;

    private String targetType;

    private String targetId;

    private String actionType;

    @DbJson
    private Map<String, Object> parameters;

    private String status;

    private String result;

    private LocalDateTime executedAt;

    private LocalDateTime rolledBackAt;

    private String rejectionReason;

    private int priority;

    private LocalDateTime createdAt;

    private String beforeSnapshot;

    private String afterSnapshot;

    /**
     * 转换为OptimizationAction
     */
    public OptimizationAction toOptimizationAction() {
        return OptimizationAction.builder()
                .id(this.id)
                .evaluationId(this.evaluationId)
                .targetType(this.targetType != null ? OptimizationAction.TargetType.valueOf(this.targetType) : null)
                .targetId(this.targetId)
                .actionType(this.actionType != null ? OptimizationAction.ActionType.valueOf(this.actionType) : null)
                .parameters(this.parameters)
                .status(this.status != null ? OptimizationAction.Status.valueOf(this.status) : null)
                .result(this.result)
                .executedAt(this.executedAt)
                .rolledBackAt(this.rolledBackAt)
                .rejectionReason(this.rejectionReason)
                .priority(this.priority)
                .createdAt(this.createdAt)
                .beforeSnapshot(this.beforeSnapshot)
                .afterSnapshot(this.afterSnapshot)
                .build();
    }

    /**
     * 从OptimizationAction创建Entity
     */
    public static OptimizationActionEntity fromOptimizationAction(OptimizationAction action) {
        return OptimizationActionEntity.builder()
                .id(action.getId())
                .evaluationId(action.getEvaluationId())
                .targetType(action.getTargetType() != null ? action.getTargetType().name() : null)
                .targetId(action.getTargetId())
                .actionType(action.getActionType() != null ? action.getActionType().name() : null)
                .parameters(action.getParameters())
                .status(action.getStatus() != null ? action.getStatus().name() : null)
                .result(action.getResult())
                .executedAt(action.getExecutedAt())
                .rolledBackAt(action.getRolledBackAt())
                .rejectionReason(action.getRejectionReason())
                .priority(action.getPriority())
                .createdAt(action.getCreatedAt())
                .beforeSnapshot(action.getBeforeSnapshot())
                .afterSnapshot(action.getAfterSnapshot())
                .build();
    }
}
