package org.dragon.observer.optimization.plan;

import java.time.LocalDateTime;

import org.dragon.observer.optimization.plan.OptimizationAction.ActionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OptimizationPlanItem 优化计划项目实体
 * 表示计划中的一个具体操作项
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationPlanItem {

    /**
     * 执行状态
     */
    public enum Status {
        PENDING,     // 待执行
        EXECUTING,   // 执行中
        SUCCESS,     // 成功
        FAILED,      // 失败
        SKIPPED,     // 跳过
        ROLLED_BACK  // 已回滚
    }

    /**
     * 计划项目唯一标识
     */
    private String id;

    /**
     * 所属计划 ID
     */
    private String planId;

    /**
     * 序号（用于排序）
     */
    private int sequence;

    /**
     * 操作类型
     */
    private OptimizationAction.ActionType actionType;

    /**
     * 目标 ID
     */
    private String targetId;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 操作参数 (JSON 格式)
     */
    private String parameters;

    /**
    * 执行状态
     */
    @Builder.Default
    private Status status = Status.PENDING;

    /**
     * 关联的优化动作 ID
     */
    private String actionId;

    /**
     * 执行时间
     */
    private LocalDateTime executedAt;

    /**
     * 执行完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 执行结果
     */
    private String result;

    /**
     * 回滚时间
     */
    private LocalDateTime rolledBackAt;

    /**
     * 回滚结果
     */
    private String rollbackResult;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 检查是否可执行
     */
    public boolean canExecute() {
        return status == Status.PENDING;
    }

    /**
     * 检查是否可回滚
     */
    public boolean canRollback() {
        return status == Status.SUCCESS;
    }

    /**
     * 标记为执行中
     */
    public void markExecuting() {
        this.status = Status.EXECUTING;
        this.executedAt = LocalDateTime.now();
    }

    /**
     * 标记为成功
     */
    public void markSuccess(String result) {
        this.status = Status.SUCCESS;
        this.completedAt = LocalDateTime.now();
        this.result = result;
    }

    /**
     * 标记为失败
     */
    public void markFailed(String result) {
        this.status = Status.FAILED;
        this.completedAt = LocalDateTime.now();
        this.result = result;
    }

    /**
     * 标记为跳过
     */
    public void markSkipped(String reason) {
        this.status = Status.SKIPPED;
        this.completedAt = LocalDateTime.now();
        this.result = reason;
    }

    /**
     * 标记为已回滚
     */
    public void markRolledBack(String result) {
        this.status = Status.ROLLED_BACK;
        this.rolledBackAt = LocalDateTime.now();
        this.rollbackResult = result;
    }
}