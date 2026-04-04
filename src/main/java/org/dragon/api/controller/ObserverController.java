package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.application.ObserverApplication;
import org.dragon.api.dto.ApiResponse;
import org.dragon.api.dto.PageResponse;
import org.dragon.observer.Observer;
import org.dragon.observer.evaluation.EvaluationRecord;
import org.dragon.observer.optimization.plan.OptimizationAction;
import org.dragon.observer.optimization.plan.OptimizationPlan;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ObserverController Observer 模块 API
 *
 * <p>对应前端 /observers 页面，包含 Observer CRUD、评价记录、优化计划、优化动作、治理日志等接口。
 * Base URL: /api/v1/observers
 *
 * @author zhz
 * @version 1.0
 */
@Tag(name = "Observer", description = "Observer 观测者模块")
@RestController
@RequestMapping("/api/v1/observers")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ObserverController {

    private final ObserverApplication observerApplication;

    // ==================== 22. Observer CRUD ====================

    /**
     * 22.1 获取 Observer 列表
     * GET /api/v1/observers
     */
    @Operation(summary = "获取 Observer 列表")
    @GetMapping
    public ApiResponse<PageResponse<Observer>> listObservers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String executionMode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<Observer> result = observerApplication.listObservers(
                page, pageSize, search, status, executionMode);
        return ApiResponse.success(result);
    }

    /**
     * 22.2 创建 Observer
     * POST /api/v1/observers
     */
    @Operation(summary = "创建 Observer")
    @PostMapping
    public ApiResponse<Observer> createObserver(@RequestBody Observer observer) {
        Observer created = observerApplication.createObserver(observer);
        return ApiResponse.success(created);
    }

    /**
     * 22.3 获取 Observer 详情
     * GET /api/v1/observers/:id
     */
    @Operation(summary = "获取 Observer 详情")
    @GetMapping("/{id}")
    @PreAuthorize("canView(#id, 'OBSERVER')")
    public ApiResponse<Observer> getObserver(@PathVariable String id) {
        return observerApplication.getObserver(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Observer not found: " + id));
    }

    /**
     * 22.4 更新 Observer
     * PUT /api/v1/observers/:id
     */
    @Operation(summary = "更新 Observer")
    @PutMapping("/{id}")
    @PreAuthorize("canEdit(#id, 'OBSERVER')")
    public ApiResponse<Observer> updateObserver(
            @PathVariable String id,
            @RequestBody Observer observer) {
        Observer updated = observerApplication.updateObserver(id, observer);
        return ApiResponse.success(updated);
    }

    /**
     * 22.5 删除 Observer
     * DELETE /api/v1/observers/:id
     */
    @Operation(summary = "删除 Observer")
    @DeleteMapping("/{id}")
    @PreAuthorize("canDelete(#id, 'OBSERVER')")
    public ApiResponse<Map<String, Object>> deleteObserver(@PathVariable String id) {
        observerApplication.deleteObserver(id);
        return ApiResponse.success(Map.of("success", true));
    }

    /**
     * 22.6 触发评价
     * POST /api/v1/observers/:id/evaluate
     */
    @Operation(summary = "手动触发 Observer 评价")
    @PostMapping("/{id}/evaluate")
    @PreAuthorize("canUse(#id, 'OBSERVER')")
    public ApiResponse<Map<String, Object>> triggerEvaluation(@PathVariable String id) {
        Map<String, Object> result = observerApplication.triggerEvaluation(id);
        return ApiResponse.success(result);
    }

    // ==================== 23. Evaluation（评价记录）====================

    /**
     * 23.1 获取评价记录列表
     * GET /api/v1/observers/:observerId/evaluations
     */
    @Operation(summary = "获取 Observer 评价记录列表")
    @GetMapping("/{observerId}/evaluations")
    public ApiResponse<PageResponse<EvaluationRecord>> listEvaluations(
            @PathVariable String observerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status) {
        PageResponse<EvaluationRecord> result = observerApplication.listEvaluations(observerId, page, pageSize);
        return ApiResponse.success(result);
    }

    // ==================== 24. Plan（优化计划）====================

    /**
     * 24.1 获取优化计划列表
     * GET /api/v1/observers/:observerId/plans
     */
    @Operation(summary = "获取 Observer 优化计划列表")
    @GetMapping("/{observerId}/plans")
    public ApiResponse<PageResponse<OptimizationPlan>> listPlans(
            @PathVariable String observerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<OptimizationPlan> result = observerApplication.listPlans(observerId, page, pageSize);
        return ApiResponse.success(result);
    }

    /**
     * 24.2 审批计划
     * POST /api/v1/observers/:observerId/plans/:planId/approve
     */
    @Operation(summary = "审批优化计划")
    @PostMapping("/{observerId}/plans/{planId}/approve")
    public ApiResponse<OptimizationPlan> approvePlan(
            @PathVariable String observerId,
            @PathVariable String planId,
            @RequestBody ApprovePlanRequest request) {
        OptimizationPlan plan = observerApplication.approvePlan(
                observerId, planId, request.isApproved(), request.getComment());
        return ApiResponse.success(plan);
    }

    /**
     * 24.3 执行计划
     * POST /api/v1/observers/:observerId/plans/:planId/execute
     */
    @Operation(summary = "执行优化计划")
    @PostMapping("/{observerId}/plans/{planId}/execute")
    public ApiResponse<OptimizationPlan> executePlan(
            @PathVariable String observerId,
            @PathVariable String planId) {
        OptimizationPlan plan = observerApplication.executePlan(observerId, planId);
        return ApiResponse.success(plan);
    }

    // ==================== 25. Action（优化动作）====================

    /**
     * 25.1 获取优化动作列表
     * GET /api/v1/observers/:observerId/actions
     */
    @Operation(summary = "获取 Observer 优化动作列表")
    @GetMapping("/{observerId}/actions")
    public ApiResponse<PageResponse<OptimizationAction>> listActions(
            @PathVariable String observerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<OptimizationAction> result = observerApplication.listActions(observerId, status, page, pageSize);
        return ApiResponse.success(result);
    }

    /**
     * 25.2 回滚动作
     * POST /api/v1/observers/:observerId/actions/:actionId/rollback
     */
    @Operation(summary = "回滚优化动作")
    @PostMapping("/{observerId}/actions/{actionId}/rollback")
    public ApiResponse<OptimizationAction> rollbackAction(
            @PathVariable String observerId,
            @PathVariable String actionId) {
        OptimizationAction action = observerApplication.rollbackAction(observerId, actionId);
        return ApiResponse.success(action);
    }

    // ==================== 26. Governance Log（治理日志）====================

    /**
     * 26.1 获取治理日志
     * GET /api/v1/observers/:observerId/governance-logs
     */
    @Operation(summary = "获取 Observer 治理日志")
    @GetMapping("/{observerId}/governance-logs")
    public ApiResponse<PageResponse<Map<String, Object>>> listGovernanceLogs(
            @PathVariable String observerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<Map<String, Object>> result = observerApplication.listGovernanceLogs(observerId, page, pageSize);
        return ApiResponse.success(result);
    }

    // ==================== 请求体 DTO ====================

    /** 审批计划请求 */
    @Data
    public static class ApprovePlanRequest {
        /** 是否批准 */
        private boolean approved;
        /** 审批意见 */
        private String comment;
    }
}