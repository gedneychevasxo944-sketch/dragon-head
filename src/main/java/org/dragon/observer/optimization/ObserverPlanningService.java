package org.dragon.observer.optimization;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dragon.observer.evaluation.EvaluationRecord;
import org.dragon.observer.evaluation.EvaluationRecordStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import lombok.Data;

import lombok.RequiredArgsConstructor;

/**
 * ObserverPlanningService 观察者计划服务
 * Plan-first 模式：评价触发后先生成计划，审批后再执行
 * 提供计划的生成、审批、执行、回滚能力
 *
 * @author wyj
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class ObserverPlanningService {

    private static final Logger log = LoggerFactory.getLogger(ObserverPlanningService.class);

    private final OptimizationPlanStore planStore;
    private final OptimizationExecutor optimizationExecutor;
    private final EvaluationRecordStore evaluationRecordStore;
    private final LLMSuggestionGenerator suggestionGenerator;
    private final Gson gson = new Gson();

    /**
     * 从评价生成优化计划
     *
     * @param evaluationId 评价记录 ID
     * @param observerId   Observer ID
     * @return 生成的优化计划（草稿状态）
     */
    public OptimizationPlan generatePlan(String evaluationId, String observerId) {
        EvaluationRecord evaluation = evaluationRecordStore.findById(evaluationId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation not found: " + evaluationId));

        log.info("[ObserverPlanningService] Generating optimization plan for evaluation: {}", evaluationId);

        // 构建计划基本信息
        OptimizationPlan plan = OptimizationPlan.builder()
                .id(UUID.randomUUID().toString())
                .observerId(observerId)
                .evaluationId(evaluationId)
                .targetType(evaluation.getTargetType() == EvaluationRecord.TargetType.CHARACTER
                        ? OptimizationAction.TargetType.CHARACTER
                        : OptimizationAction.TargetType.WORKSPACE)
                .targetId(evaluation.getTargetId())
                .status(OptimizationPlan.Status.DRAFT)
                .title("优化计划 - " + evaluation.getTargetId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 生成计划项目
        List<OptimizationPlanItem> items = generatePlanItems(plan, evaluation);
        plan.setItems(items);

        // 生成摘要
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("基于评价（综合评分: %.2f），生成 %d 个优化项目。\n",
                evaluation.getOverallScore() != null ? evaluation.getOverallScore() : 0.0,
                items.size()));
        if (!items.isEmpty()) {
            summary.append("优化项目：\n");
            for (int i = 0; i < items.size(); i++) {
                OptimizationPlanItem item = items.get(i);
                summary.append(String.format("%d. [%s] %s\n", i + 1, item.getActionType(), item.getDescription()));
            }
        }
        plan.setSummary(summary.toString());

        // 保存计划
        planStore.save(plan);
        for (OptimizationPlanItem item : items) {
            planStore.saveItem(item);
        }

        log.info("[ObserverPlanningService] Generated plan {} with {} items", plan.getId(), items.size());
        return plan;
    }

    /**
     * 生成计划项目列表
     * 通过 LLM 分析评价数据，生成结构化的优化项目
     */
    private List<OptimizationPlanItem> generatePlanItems(OptimizationPlan plan, EvaluationRecord evaluation) {
        List<OptimizationPlanItem> items = new ArrayList<>();

        // 方式1：基于评价建议生成（使用现有的 LLM suggestion 生成器）
        String workspace = ""; // TODO: 需要从 evaluation 或 context 中获取
        List<String> suggestions = suggestionGenerator.generateSuggestions(
                workspace, null,
                plan.getTargetType(), plan.getTargetId(),
                10);

        int sequence = 1;
        for (String suggestion : suggestions) {
            OptimizationAction.ActionType actionType = inferActionType(suggestion);
            OptimizationPlanItem item = OptimizationPlanItem.builder()
                    .id(UUID.randomUUID().toString())
                    .planId(plan.getId())
                    .sequence(sequence++)
                    .actionType(actionType)
                    .targetId(plan.getTargetId())
                    .description(suggestion)
                    .parameters(gson.toJson(Map.of("suggestion", suggestion)))
                    .status(OptimizationPlanItem.Status.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            items.add(item);
        }

        // 如果没有生成任何项目，添加一个默认项目
        if (items.isEmpty()) {
            OptimizationPlanItem item = OptimizationPlanItem.builder()
                    .id(UUID.randomUUID().toString())
                    .planId(plan.getId())
                    .sequence(1)
                    .actionType(OptimizationAction.ActionType.UPDATE_MIND)
                    .targetId(plan.getTargetId())
                    .description("基于评价结果进行通用优化")
                    .parameters("{}")
                    .status(OptimizationPlanItem.Status.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            items.add(item);
        }

        return items;
    }

    /**
     * 从建议文本推断动作类型
     */
    private OptimizationAction.ActionType inferActionType(String suggestion) {
        String lower = suggestion.toLowerCase();
        if (lower.contains("性格") || lower.contains("personality") || lower.contains("个性")) {
            return OptimizationAction.ActionType.UPDATE_PERSONALITY;
        } else if (lower.contains("标签") || lower.contains("tag") || lower.contains("印象")) {
            return OptimizationAction.ActionType.UPDATE_TAG;
        } else if (lower.contains("技能") || lower.contains("skill")) {
            return OptimizationAction.ActionType.ADD_SKILL;
        } else if (lower.contains("记忆") || lower.contains("memory")) {
            return OptimizationAction.ActionType.UPDATE_MEMORY;
        } else if (lower.contains("权重") || lower.contains("weight")) {
            return OptimizationAction.ActionType.ADJUST_WEIGHT;
        } else if (lower.contains("配置") || lower.contains("config")) {
            return OptimizationAction.ActionType.UPDATE_CONFIG;
        }
        return OptimizationAction.ActionType.UPDATE_MIND;
    }

    /**
     * 审批计划
     *
     * @param planId   计划 ID
     * @param approver 审批人
     * @param comment  审批意见
     * @return 审批后的计划
     */
    public OptimizationPlan approvePlan(String planId, String approver, String comment) {
        OptimizationPlan plan = planStore.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        if (!plan.canApprove()) {
            throw new IllegalStateException("Plan cannot be approved in status: " + plan.getStatus());
        }

        plan.approve(approver, comment);
        planStore.save(plan);

        log.info("[ObserverPlanningService] Plan {} approved by {}", planId, approver);
        return plan;
    }

    /**
     * 拒绝计划
     *
     * @param planId  计划 ID
     * @param reason  拒绝原因
     * @return 拒绝后的计划
     */
    public OptimizationPlan rejectPlan(String planId, String reason) {
        OptimizationPlan plan = planStore.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        plan.cancel();
        planStore.save(plan);

        log.info("[ObserverPlanningService] Plan {} rejected: {}", planId, reason);
        return plan;
    }

    /**
     * 执行计划
     * 按顺序执行每个计划项目
     *
     * @param planId 计划 ID
     * @return 执行后的计划
     */
    public OptimizationPlan executePlan(String planId) {
        OptimizationPlan plan = planStore.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        if (!plan.canExecute()) {
            throw new IllegalStateException("Plan cannot be executed in status: " + plan.getStatus());
        }

        plan.markExecuting();
        planStore.save(plan);

        List<OptimizationPlanItem> items = planStore.findItemsByPlanId(planId);
        int successCount = 0;
        int failCount = 0;
        List<String> results = new ArrayList<>();

        for (OptimizationPlanItem item : items) {
            if (item.getStatus() != OptimizationPlanItem.Status.PENDING) {
                continue;
            }

            try {
                // 为每个项目创建 OptimizationAction 并执行
                OptimizationAction action = createActionFromItem(item, plan);
                OptimizationAction executed = optimizationExecutor.execute(action);

                // 更新项目状态
                item.setActionId(executed.getId());
                if (executed.getStatus() == OptimizationAction.Status.EXECUTED) {
                    item.markSuccess(executed.getResult());
                    successCount++;
                } else if (executed.getStatus() == OptimizationAction.Status.REJECTED) {
                    item.markFailed("Rejected: " + executed.getRejectionReason());
                    failCount++;
                } else {
                    item.markFailed("Execution failed: " + executed.getResult());
                    failCount++;
                }
                planStore.saveItem(item);
                results.add(String.format("[%s] %s: %s", item.getActionType(), item.getDescription(),
                        item.getStatus() == OptimizationPlanItem.Status.SUCCESS ? "成功" : "失败"));

            } catch (Exception e) {
                log.error("[ObserverPlanningService] Item execution failed: {}", item.getId(), e);
                item.markFailed("Exception: " + e.getMessage());
                planStore.saveItem(item);
                failCount++;
                results.add(String.format("[%s] %s: 异常-%s", item.getActionType(), item.getDescription(), e.getMessage()));
            }
        }

        // 更新计划状态
        if (failCount == 0) {
            plan.markCompleted("全部 " + successCount + " 个项目执行成功");
        } else if (successCount > 0) {
            plan.markPartial(successCount + " 个成功， " + failCount + " 个失败");
        } else {
            plan.markFailed("全部 " + failCount + " 个项目执行失败");
        }
        planStore.save(plan);

        log.info("[ObserverPlanningService] Plan {} executed: {} success, {} failed", planId, successCount, failCount);
        return plan;
    }

    /**
     * 从计划项目创建优化动作
     */
    private OptimizationAction createActionFromItem(OptimizationPlanItem item, OptimizationPlan plan) {
        Map<String, Object> params = new java.util.HashMap<>();
        try {
            params = gson.fromJson(item.getParameters(), java.util.Map.class);
        } catch (Exception e) {
            params.put("suggestion", item.getDescription());
        }
        params.put("planId", plan.getId());
        params.put("planItemId", item.getId());

        return OptimizationAction.builder()
                .id(UUID.randomUUID().toString())
                .evaluationId(plan.getEvaluationId())
                .targetType(plan.getTargetType())
                .targetId(item.getTargetId())
                .actionType(item.getActionType())
                .parameters(params)
                .status(OptimizationAction.Status.PENDING)
                .priority(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 回滚计划
     * 回滚所有已成功执行的项目
     *
     * @param planId 计划 ID
     * @return 回滚后的计划
     */
    public OptimizationPlan rollbackPlan(String planId) {
        OptimizationPlan plan = planStore.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        List<OptimizationPlanItem> items = planStore.findItemsByPlanId(planId);
        int rollbackCount = 0;

        for (OptimizationPlanItem item : items) {
            if (item.getStatus() == OptimizationPlanItem.Status.SUCCESS && item.getActionId() != null) {
                try {
                    OptimizationAction rolledBack = optimizationExecutor.rollback(item.getActionId());
                    if (rolledBack.getStatus() == OptimizationAction.Status.ROLLED_BACK) {
                        item.markRolledBack("Rollback successful");
                        rollbackCount++;
                    } else {
                        item.markRolledBack("Rollback returned status: " + rolledBack.getStatus());
                    }
                } catch (Exception e) {
                    log.error("[ObserverPlanningService] Rollback failed for item: {}", item.getId(), e);
                    item.markRolledBack("Rollback failed: " + e.getMessage());
                }
                planStore.saveItem(item);
            }
        }

        plan.markFailed("已回滚 " + rollbackCount + " 个项目");
        planStore.save(plan);

        log.info("[ObserverPlanningService] Plan {} rolled back {} items", planId, rollbackCount);
        return plan;
    }

    /**
     * 获取计划详情
     *
     * @param planId 计划 ID
     * @return 计划
     */
    public OptimizationPlan getPlan(String planId) {
        return planStore.findById(planId).orElse(null);
    }

    /**
     * 获取计划的所有项目
     *
     * @param planId 计划 ID
     * @return 项目列表
     */
    public List<OptimizationPlanItem> getPlanItems(String planId) {
        return planStore.findItemsByPlanId(planId);
    }

    /**
     * 查询待审批的计划
     *
     * @return 计划列表
     */
    public List<OptimizationPlan> getPendingApprovalPlans() {
        return planStore.findPendingApproval();
    }

    /**
     * 查询目标的优化计划历史
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @return 计划列表
     */
    public List<OptimizationPlan> getPlansByTarget(OptimizationAction.TargetType targetType, String targetId) {
        return planStore.findByTarget(targetType, targetId);
    }

    /**
     * 复核计划
     * 对生成的计划进行复核，评估其可行性和风险
     *
     * @param planId 计划 ID
     * @param reviewer 复核人（Reviewer Character）
     * @return 复核结果
     */
    public ReviewResult reviewPlan(String planId, String reviewer) {
        log.info("[ObserverPlanningService] Reviewing plan {} by {}", planId, reviewer);

        OptimizationPlan plan = planStore.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        List<OptimizationPlanItem> items = planStore.findItemsByPlanId(planId);

        // 构建复核结果
        ReviewResult result = new ReviewResult();
        result.setPlanId(planId);
        result.setReviewer(reviewer);
        result.setReviewedAt(LocalDateTime.now());

        // 检查计划项目数量
        if (items.isEmpty()) {
            result.setApproved(false);
            result.setComment("计划为空，无法审批");
            return result;
        }

        // 检查计划项目是否合理
        int validCount = 0;
        int riskyCount = 0;
        List<String> concerns = new ArrayList<>();

        for (OptimizationPlanItem item : items) {
            // 检查动作类型是否与目标类型匹配
            if (!isValidActionForTarget(item.getActionType(), plan.getTargetType())) {
                concerns.add(String.format("项目 [%s] 动作类型 %s 与目标类型 %s 不匹配",
                        item.getDescription(), item.getActionType(), plan.getTargetType()));
                riskyCount++;
            }
            validCount++;
        }

        // 基于评价记录评估风险
        EvaluationRecord evaluation = evaluationRecordStore.findById(plan.getEvaluationId()).orElse(null);
        if (evaluation != null && evaluation.getFindings() != null) {
            for (var finding : evaluation.getFindings()) {
                if (Boolean.TRUE.equals(finding.isUnsafe())) {
                    concerns.add(String.format("发现安全隐患 [%s]: %s", finding.getDimension(), finding.getSummary()));
                    riskyCount++;
                }
            }
        }

        result.setApproved(concerns.isEmpty());
        result.setConcerns(concerns);
        result.setValidItemCount(validCount);
        result.setRiskyItemCount(riskyCount);
        result.setComment(buildReviewComment(result));

        log.info("[ObserverPlanningService] Review result for plan {}: approved={}, concerns={}",
                planId, result.isApproved(), concerns.size());

        return result;
    }

    /**
     * 检查动作类型是否对目标类型有效
     */
    private boolean isValidActionForTarget(OptimizationAction.ActionType actionType, OptimizationAction.TargetType targetType) {
        // Character 目标支持所有动作类型
        if (targetType == OptimizationAction.TargetType.CHARACTER) {
            return true;
        }
        // Workspace 目标不支持 REMOVE_SKILL
        if (targetType == OptimizationAction.TargetType.WORKSPACE) {
            return actionType != OptimizationAction.ActionType.REMOVE_SKILL;
        }
        return true;
    }

    /**
     * 构建复核评论
     */
    private String buildReviewComment(ReviewResult result) {
        if (result.isApproved()) {
            return String.format("复核通过。计划包含 %d 个有效项目，无风险项。", result.getValidItemCount());
        } else {
            return String.format("复核未通过。存在 %d 个风险项：\n%s",
                    result.getRiskyItemCount(), String.join("\n", result.getConcerns()));
        }
    }

    /**
     * 构建计划摘要
     *
     * @param planId 计划 ID
     * @return 摘要文本
     */
    public String buildPlanSummary(String planId) {
        OptimizationPlan plan = planStore.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        List<OptimizationPlanItem> items = planStore.findItemsByPlanId(planId);

        StringBuilder summary = new StringBuilder();
        summary.append("# 优化计划摘要\n\n");
        summary.append(String.format("**计划ID**: %s\n", plan.getId()));
        summary.append(String.format("**目标类型**: %s\n", plan.getTargetType()));
        summary.append(String.format("**目标ID**: %s\n", plan.getTargetId()));
        summary.append(String.format("**状态**: %s\n", plan.getStatus()));
        summary.append(String.format("**创建时间**: %s\n\n", plan.getCreatedAt()));

        // 评价信息
        EvaluationRecord evaluation = evaluationRecordStore.findById(plan.getEvaluationId()).orElse(null);
        if (evaluation != null) {
            summary.append("## 评价信息\n\n");
            summary.append(String.format("- 综合评分: %.2f\n", evaluation.getOverallScore() != null ? evaluation.getOverallScore() : 0));
            summary.append(String.format("- 评价时间: %s\n", evaluation.getTimestamp()));
            if (evaluation.getFindings() != null && !evaluation.getFindings().isEmpty()) {
                summary.append(String.format("- 发现问题: %d 个\n", evaluation.getFindings().size()));
            }
            summary.append("\n");
        }

        // 计划项目
        summary.append("## 优化项目\n\n");
        if (items.isEmpty()) {
            summary.append("无优化项目。\n");
        } else {
            for (int i = 0; i < items.size(); i++) {
                OptimizationPlanItem item = items.get(i);
                summary.append(String.format("%d. **[%s]** %s\n", i + 1, item.getActionType(), item.getDescription()));
                summary.append(String.format("   - 状态: %s\n", item.getStatus()));
                summary.append(String.format("   - 序号: %d\n", item.getSequence()));
            }
        }

        // 风险提示
        if (evaluation != null && evaluation.getFindings() != null) {
            boolean hasUnsafe = evaluation.getFindings().stream().anyMatch(f -> Boolean.TRUE.equals(f.isUnsafe()));
            if (hasUnsafe) {
                summary.append("\n## 风险提示\n\n");
                summary.append("存在安全隐患，请谨慎审批。\n");
            }
        }

        return summary.toString();
    }

    /**
     * 复核结果
     */
    @Data
    public static class ReviewResult {
        private String planId;
        private String reviewer;
        private LocalDateTime reviewedAt;
        private boolean approved;
        private List<String> concerns;
        private int validItemCount;
        private int riskyItemCount;
        private String comment;
    }
}