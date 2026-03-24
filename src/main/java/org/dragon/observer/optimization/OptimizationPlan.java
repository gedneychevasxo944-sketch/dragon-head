package org.dragon.observer.optimization;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OptimizationPlan 优化计划实体
 * Plan-first 模式：评价触发后先生成计划，审批后再执行
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationPlan {

    /**
     * 计划状态
     */
    public enum Status {
        DRAFT,      // 草稿（刚生成，未审批）
        APPROVED,   // 已审批（可以执行）
        EXECUTING,  // 执行中
        COMPLETED,  // 已完成
        PARTIAL,    // 部分成功
        CANCELLED,  // 已取消
        FAILED      // 执行失败
    }

    /**
     * 计划唯一标识
     */
    private String id;

    /**
     * 关联的 Observer ID
     */
    private String observerId;

    /**
     * 关联的评价记录 ID
     */
    private String evaluationId;

    /**
     * 目标类型
     */
    private OptimizationAction.TargetType targetType;

    /**
     * 目标 ID
     */
    private String targetId;

    /**
     * 计划状态
     */
    @Builder.Default
    private Status status = Status.DRAFT;

    /**
     * 计划标题
     */
    private String title;

    /**
     * 计划摘要
     */
    private String summary;

    /**
     * 计划详情（LLM 生成的原始文本）
     */
    private String rawContent;

    /**
     * 计划项目列表
     */
    @Builder.Default
    private List<OptimizationPlanItem> items = new ArrayList<>();

    /**
     * 审批人
     */
    private String approver;

    /**
     * 审批时间
     */
    private LocalDateTime approvedAt;

    /**
     * 审批备注
     */
    private String approvalComment;

    /**
     * 执行开始时间
     */
    private LocalDateTime executedAt;

    /**
     * 执行完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 执行结果摘要
     */
    private String executionSummary;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 检查是否可审批
     */
    public boolean canApprove() {
        return status == Status.DRAFT;
    }

    /**
     * 检查是否可执行
     */
    public boolean canExecute() {
        return status == Status.APPROVED;
    }

    /**
     * 检查是否可取消
     */
    public boolean canCancel() {
        return status == Status.DRAFT;
    }

    /**
     * 审批通过
     */
    public void approve(String approver, String comment) {
        this.status = Status.APPROVED;
        this.approver = approver;
        this.approvedAt = LocalDateTime.now();
        this.approvalComment = comment;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 取消计划
     */
    public void cancel() {
        this.status = Status.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记为执行中
     */
    public void markExecuting() {
        this.status = Status.EXECUTING;
        this.executedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记为完成
     */
    public void markCompleted(String summary) {
        this.status = Status.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.executionSummary = summary;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记为部分成功
     */
    public void markPartial(String summary) {
        this.status = Status.PARTIAL;
        this.completedAt = LocalDateTime.now();
        this.executionSummary = summary;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记为失败
     */
    public void markFailed(String reason) {
        this.status = Status.FAILED;
        this.completedAt = LocalDateTime.now();
        this.executionSummary = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 添加计划项目
     */
    public void addItem(OptimizationPlanItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }
}