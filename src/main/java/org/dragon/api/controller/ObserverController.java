package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.observer.Observer;
import org.dragon.observer.ObserverRegistry;
import org.dragon.observer.ObserverService;
import org.dragon.workspace.commons.CommonSense;
import org.dragon.workspace.commons.CommonSenseValidator;
import org.dragon.workspace.commons.store.WorkspaceCommonSenseStore;
import org.dragon.observer.collector.DataCollector;
import org.dragon.observer.collector.dto.ObservationDataset;
import org.dragon.observer.evaluation.EvaluationEngine;
import org.dragon.observer.evaluation.EvaluationRecord;
import org.dragon.observer.optimization.plan.ObserverPlanningService;
import org.dragon.observer.optimization.plan.OptimizationAction;
import org.dragon.observer.optimization.plan.OptimizationPlan;
import org.dragon.observer.optimization.plan.OptimizationPlanItem;
import org.dragon.observer.optimization.plan.OptimizationPlanParser;
import org.dragon.store.StoreFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ObserverController 观察者管理 API
 *
 * <p>职责范围：
 * <ul>
 *   <li>Observer 生命周期（注册/注销/激活/暂停）</li>
 *   <li>评价记录查询（按目标/按阈值）</li>
 *   <li>手动触发评价</li>
 *   <li>优化动作管理（查询/手动执行/回滚）</li>
 *   <li>常识库（CommonSense）的 CRUD 管理</li>
 *   <li>统计仪表盘</li>
 * </ul>
 *
 * @author zhz
 * @version 1.0
 */
@RestController
@RequestMapping("/api/observers")
@RequiredArgsConstructor
public class ObserverController {

    @Autowired
    private final ObserverRegistry observerRegistry;
    @Autowired
    private final ObserverService observerService;
    @Autowired
    private final ObserverPlanningService planningService;
    @Autowired
    private final OptimizationPlanParser planParser;
    @Autowired
    private final DataCollector dataCollector;
    @Autowired
    private final StoreFactory storeFactory;

    private WorkspaceCommonSenseStore getCommonSenseStore() {
        return storeFactory.get(WorkspaceCommonSenseStore.class);
    }

    // ==================== Observer 生命周期 ====================

    @Operation(summary = "注册新Observer")
    @PostMapping
    public ResponseEntity<Observer> registerObserver(@RequestBody Observer observer) {
        observerRegistry.register(observer);
        return ResponseEntity.status(HttpStatus.CREATED).body(observer);
    }

    @Operation(summary = "查询所有Observer")
    @GetMapping
    public ResponseEntity<List<Observer>> listObservers(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<Observer> list = activeOnly ? observerRegistry.listActive() : observerRegistry.listAll();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "查询指定Observer")
    @GetMapping("/{observerId}")
    public ResponseEntity<Observer> getObserver(@PathVariable String observerId) {
        return observerRegistry.get(observerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "根据绑定的 Workspace 查询对应 Observer")
    @GetMapping("/by-workspace/{workspaceId}")
    public ResponseEntity<Observer> getObserverByWorkspace(@PathVariable String workspaceId) {
        return observerRegistry.getByWorkspace(workspaceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "更新 Observer 配置（如修改评价阈值、开关自动优化）")
    @PutMapping("/{observerId}")
    public ResponseEntity<Observer> updateObserver(
            @PathVariable String observerId,
            @RequestBody Observer observer) {
        observer.setId(observerId);
        observerRegistry.update(observer);
        return ResponseEntity.ok(observer);
    }

    @Operation(summary = "注销 Observer")
    @DeleteMapping("/{observerId}")
    public ResponseEntity<Void> unregisterObserver(@PathVariable String observerId) {
        observerRegistry.unregister(observerId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "激活Observer（INACTIVE/PAUSED → ACTIVE）")
    @PostMapping("/{observerId}/activate")
    public ResponseEntity<Void> activateObserver(@PathVariable String observerId) {
        observerRegistry.activate(observerId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "暂停 Observer（ACTIVE → PAUSED）")
    @PostMapping("/{observerId}/pause")
    public ResponseEntity<Void> pauseObserver(@PathVariable String observerId) {
        observerRegistry.pause(observerId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "设置默认 Observer")
    @PostMapping("/{observerId}/set-default")
    public ResponseEntity<Void> setDefaultObserver(@PathVariable String observerId) {
        observerRegistry.setDefaultObserver(observerId);
        return ResponseEntity.ok().build();
    }

    // ==================== 统计仪表盘 ====================

    @Operation(summary = "查询 Observer 运行统计（评价数/优化数/常识数等）")
    @GetMapping("/{observerId}/stats")
    public ResponseEntity<ObserverService.ObserverStats> getStats(@PathVariable String observerId) {
        ObserverService.ObserverStats stats = observerService.getStats(observerId);
        return ResponseEntity.ok(stats);
    }

    // ==================== 评价记录 ====================

    @Operation(summary = "查询某个目标（Character/Workspace）的所有评价记录")
    @GetMapping("/{observerId}/evaluations")
    public ResponseEntity<List<EvaluationRecord>> getEvaluations(
            @PathVariable String observerId,
            @RequestParam EvaluationRecord.TargetType targetType,
            @RequestParam String targetId) {
        List<EvaluationRecord> records = observerService.getEvaluationsByTarget(targetType, targetId);
        return ResponseEntity.ok(records);
    }

    @Operation(summary = "查询综合评分低于阈值的评价记录（需要关注的问题项）")
    @GetMapping("/{observerId}/evaluations/below-threshold")
    public ResponseEntity<List<EvaluationRecord>> getBelowThresholdEvaluations(
            @PathVariable String observerId) {
        List<EvaluationRecord> records = observerService.getBelowThresholdEvaluations(observerId);
        return ResponseEntity.ok(records);
    }

    @Operation(summary = "查询指定评价记录详情")
    @GetMapping("/{observerId}/evaluations/{evaluationId}")
    public ResponseEntity<EvaluationRecord> getEvaluation(
            @PathVariable String observerId,
            @PathVariable String evaluationId) {
        EvaluationRecord record = observerService.getEvaluation(evaluationId);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(record);
    }

    @Operation(summary = "手动触发单次任务评价")
    @PostMapping("/{observerId}/evaluations/trigger")
    public ResponseEntity<EvaluationRecord> triggerEvaluation(
            @PathVariable String observerId,
            @RequestBody EvaluationEngine.TaskData taskData) {
        EvaluationRecord record = observerService.evaluateTask(observerId, taskData);
        if (record == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return ResponseEntity.ok(record);
    }

    @Operation(summary = "手动触发周期性评价")
    @PostMapping("/{observerId}/evaluations/periodic")
    public ResponseEntity<EvaluationRecord> triggerPeriodicEvaluation(
            @PathVariable String observerId,
            @RequestBody PeriodicEvaluationRequest body) {
        EvaluationRecord record = observerService.evaluatePeriodically(
                observerId, body.getTargetType(), body.getTargetId(), body.getPeriodHours());
        if (record == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return ResponseEntity.ok(record);
    }

    // ==================== 优化动作管理 ====================

    @Operation(summary = "查询目标的优化历史")
    @GetMapping("/{observerId}/optimizations")
    public ResponseEntity<List<OptimizationAction>> getOptimizationHistory(
            @PathVariable String observerId,
            @RequestParam EvaluationRecord.TargetType targetType,
            @RequestParam String targetId) {
        List<OptimizationAction> actions = observerService.getOptimizationHistory(targetType, targetId);
        return ResponseEntity.ok(actions);
    }

    @Operation(summary = "查询指定优化动作详情")
    @GetMapping("/{observerId}/optimizations/{actionId}")
    public ResponseEntity<OptimizationAction> getOptimizationAction(
            @PathVariable String observerId,
            @PathVariable String actionId) {
        OptimizationAction action = observerService.getOptimizationAction(actionId);
        if (action == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(action);
    }

    @Operation(summary = "手动执行所有待处理的优化动作（autoOptimizationEnabled=false 时依赖此接口）")
    @PostMapping("/{observerId}/optimizations/execute-pending")
    public ResponseEntity<List<OptimizationAction>> executePendingOptimizations(
            @PathVariable String observerId) {
        List<OptimizationAction> executed = observerService.executePendingOptimizations(observerId);
        return ResponseEntity.ok(executed);
    }

    @Operation(summary = "回滚已执行的优化动作")
    @PostMapping("/{observerId}/optimizations/{actionId}/rollback")
    public ResponseEntity<OptimizationAction> rollbackOptimization(
            @PathVariable String observerId,
            @PathVariable String actionId) {
        OptimizationAction action = observerService.rollbackOptimization(actionId);
        return ResponseEntity.ok(action);
    }

    // ==================== 常识库（CommonSense）管理 ====================

    @Operation(summary = "新增常识规则")
    @PostMapping("/common-senses")
    public ResponseEntity<CommonSense> createCommonSense(@RequestBody CommonSense commonSense) {
        CommonSense saved = getCommonSenseStore().save(commonSense);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "查询某个目标（Character/Workspace）的所有评价记录")
    @GetMapping("/common-senses")
    public ResponseEntity<List<CommonSense>> listCommonSenses(
            @RequestParam String workspaceId,
            @RequestParam(required = false) CommonSense.Category category,
            @RequestParam(required = false) CommonSense.Severity severity,
            @RequestParam(required = false, defaultValue = "false") boolean enabledOnly) {
        List<CommonSense> list;
        if (category != null) {
            list = getCommonSenseStore().findByWorkspaceAndCategory(workspaceId, category);
        } else if (enabledOnly) {
            list = getCommonSenseStore().findEnabled(workspaceId);
        } else {
            list = getCommonSenseStore().findByWorkspace(workspaceId);
        }
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "查询指定常识规则")
    @GetMapping("/common-senses/{commonSenseId}")
    public ResponseEntity<CommonSense> getCommonSense(@PathVariable String commonSenseId) {
        return getCommonSenseStore().findById(commonSenseId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "更新常识规则")
    @PutMapping("/common-senses/{commonSenseId}")
    public ResponseEntity<CommonSense> updateCommonSense(
            @PathVariable String commonSenseId,
            @RequestBody CommonSense commonSense) {
        commonSense.setId(commonSenseId);
        // 关键级别常识不允许被禁用
        CommonSense existing = getCommonSenseStore().findById(commonSenseId)
                .orElseThrow(() -> new IllegalArgumentException("CommonSense not found: " + commonSenseId));
        if (existing.isCritical() && !commonSense.isEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        CommonSense saved = getCommonSenseStore().save(commonSense);
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "删除常识规则（CRITICAL 级别不允许删除）")
    @DeleteMapping("/common-senses/{commonSenseId}")
    public ResponseEntity<Void> deleteCommonSense(@PathVariable String commonSenseId) {
        CommonSense existing = getCommonSenseStore().findById(commonSenseId)
                .orElseThrow(() -> new IllegalArgumentException("CommonSense not found: " + commonSenseId));
        if (existing.isCritical()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        getCommonSenseStore().delete(commonSenseId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "校验某个优化动作是否会触犯常识规则（调试接口）")
    @PostMapping("/common-senses/validate")
    public ResponseEntity<CommonSenseValidator.ValidationResult> validateAction(
            @RequestBody ValidateActionRequest body) {
        CommonSenseValidator.ValidationResult result = observerService.validateAgainstCommonSense(
                body.getActionTargetType(), body.getActionType(), body.getParameters());
        return ResponseEntity.ok(result);
    }

    // ==================== 优化计划管理（Plan-first 模式） ====================

    @Operation(summary = "从评价记录生成优化计划")
    @PostMapping("/{observerId}/plans/generate")
    public ResponseEntity<OptimizationPlan> generatePlan(
            @PathVariable String observerId,
            @RequestBody GeneratePlanRequest request) {
        EvaluationRecord record = observerService.getEvaluation(request.getEvaluationId());
        if (record == null) {
            return ResponseEntity.notFound().build();
        }

        // 如果提供了原始文本，使用解析器解析
        if (request.getPlanText() != null && !request.getPlanText().isEmpty()) {
            OptimizationPlan plan = planParser.parse(
                    request.getPlanText(),
                    observerId,
                    request.getEvaluationId(),
                    OptimizationAction.TargetType.valueOf(request.getTargetType()),
                    request.getTargetId());
            return ResponseEntity.status(HttpStatus.CREATED).body(plan);
        }

        // 否则使用默认的基于评价的计划生成
        OptimizationPlan plan = planningService.generatePlan(request.getEvaluationId());
        return ResponseEntity.status(HttpStatus.CREATED).body(plan);
    }

    @Operation(summary = "查询优化计划详情")
    @GetMapping("/{observerId}/plans/{planId}")
    public ResponseEntity<OptimizationPlan> getPlan(
            @PathVariable String observerId,
            @PathVariable String planId) {
        OptimizationPlan plan = planningService.getPlan(planId);
        if (plan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(plan);
    }

    @Operation(summary = "查询计划的所有项目")
    @GetMapping("/{observerId}/plans/{planId}/items")
    public ResponseEntity<List<OptimizationPlanItem>> getPlanItems(
            @PathVariable String observerId,
            @PathVariable String planId) {
        List<OptimizationPlanItem> items = planningService.getPlanItems(planId);
        return ResponseEntity.ok(items);
    }

    @Operation(summary = "查询待审批的计划列表")
    @GetMapping("/{observerId}/plans/pending")
    public ResponseEntity<List<OptimizationPlan>> getPendingApprovalPlans(
            @PathVariable String observerId) {
        List<OptimizationPlan> plans = planningService.getPendingApprovalPlans();
        return ResponseEntity.ok(plans);
    }

    @Operation(summary = "复核优化计划（Reviewer Character 执行）")
    @PostMapping("/{observerId}/plans/{planId}/review")
    public ResponseEntity<ObserverPlanningService.ReviewResult> reviewPlan(
            @PathVariable String observerId,
            @PathVariable String planId,
            @RequestBody ReviewPlanRequest request) {
        ObserverPlanningService.ReviewResult result = planningService.reviewPlan(planId, request.getReviewer());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "审批通过优化计划")
    @PostMapping("/{observerId}/plans/{planId}/approve")
    public ResponseEntity<OptimizationPlan> approvePlan(
            @PathVariable String observerId,
            @PathVariable String planId,
            @RequestBody ApprovePlanRequest request) {
        try {
            OptimizationPlan plan = planningService.approvePlan(planId, request.getApprover(), request.getComment());
            return ResponseEntity.ok(plan);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Operation(summary = "拒绝优化计划")
    @PostMapping("/{observerId}/plans/{planId}/reject")
    public ResponseEntity<OptimizationPlan> rejectPlan(
            @PathVariable String observerId,
            @PathVariable String planId,
            @RequestBody RejectPlanRequest request) {
        OptimizationPlan plan = planningService.rejectPlan(planId, request.getReason());
        return ResponseEntity.ok(plan);
    }

    @Operation(summary = "执行优化计划")
    @PostMapping("/{observerId}/plans/{planId}/execute")
    public ResponseEntity<OptimizationPlan> executePlan(
            @PathVariable String observerId,
            @PathVariable String planId) {
        try {
            OptimizationPlan plan = planningService.executePlan(planId);
            return ResponseEntity.ok(plan);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Operation(summary = "回滚优化计划")
    @PostMapping("/{observerId}/plans/{planId}/rollback")
    public ResponseEntity<OptimizationPlan> rollbackPlan(
            @PathVariable String observerId,
            @PathVariable String planId) {
        OptimizationPlan plan = planningService.rollbackPlan(planId);
        return ResponseEntity.ok(plan);
    }

    @Operation(summary = "获取计划摘要")
    @GetMapping("/{observerId}/plans/{planId}/summary")
    public ResponseEntity<Map<String, Object>> getPlanSummary(
            @PathVariable String observerId,
            @PathVariable String planId) {
        String summary = planningService.buildPlanSummary(planId);
        return ResponseEntity.ok(Map.of("summary", summary));
    }

    @Operation(summary = "查询目标的优化计划历史")
    @GetMapping("/{observerId}/plans")
    public ResponseEntity<List<OptimizationPlan>> getPlansByTarget(
            @PathVariable String observerId,
            @RequestParam OptimizationAction.TargetType targetType,
            @RequestParam String targetId) {
        List<OptimizationPlan> plans = planningService.getPlansByTarget(targetType, targetId);
        return ResponseEntity.ok(plans);
    }

    // ==================== 请求体 DTO ====================

    @Data
    public static class PeriodicEvaluationRequest {
        private EvaluationRecord.TargetType targetType;
        private String targetId;
        /** 评价时间窗口（小时） */
        private int periodHours;
    }

    @Data
    public static class ValidateActionRequest {
        private String actionTargetType;
        private String actionType;
        private Map<String, Object> parameters;
    }

    @Data
    public static class GeneratePlanRequest {
        private String evaluationId;
        private String planText;
        private String targetType;
        private String targetId;
    }

    @Data
    public static class ReviewPlanRequest {
        private String reviewer;
    }

    @Data
    public static class ApprovePlanRequest {
        private String approver;
        private String comment;
    }

    @Data
    public static class RejectPlanRequest {
        private String reason;
    }
}