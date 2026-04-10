package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dragon.application.LogsApplication;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.api.controller.dto.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * LogsController 日志与监控模块 API
 *
 * <p>对应前端 /logs 页面，包含事件日志、链路追踪、健康状态、审计记录等接口。
 * Base URL: /api/v1/logs
 *
 * @author zhz
 * @version 1.0
 */
@Tag(name = "Logs", description = "日志与监控模块：事件日志、链路追踪、健康状态、审计记录")
@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class LogsController {

    private final LogsApplication logsApplication;

    // ==================== 28. Event（事件日志）====================

    /**
     * 28.1 获取事件日志列表
     * GET /api/v1/logs/events
     */
    @Operation(summary = "获取事件日志列表")
    @GetMapping("/events")
    public ApiResponse<PageResponse<Map<String, Object>>> listEventLogs(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String dateStart,
            @RequestParam(required = false) String dateEnd,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<Map<String, Object>> result = logsApplication.listEventLogs(
                targetType, targetId, eventType, severity, search, dateStart, dateEnd, page, pageSize);
        return ApiResponse.success(result);
    }

    /**
     * 28.2 获取事件详情
     * GET /api/v1/logs/events/:id
     */
    @Operation(summary = "获取事件日志详情")
    @GetMapping("/events/{id}")
    public ApiResponse<Map<String, Object>> getEventLog(@PathVariable String id) {
        Map<String, Object> event = logsApplication.getEventLog(id);
        if (event == null) {
            return ApiResponse.error(404, "Event not found: " + id);
        }
        return ApiResponse.success(event);
    }

    // ==================== 29. Trace（链路追踪）====================

    /**
     * 29.1 获取链路列表
     * GET /api/v1/logs/traces
     */
    @Operation(summary = "获取链路追踪列表")
    @GetMapping("/traces")
    public ApiResponse<PageResponse<Map<String, Object>>> listTraces(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<Map<String, Object>> result = logsApplication.listTraces(
                targetType, targetId, traceId, status, page, pageSize);
        return ApiResponse.success(result);
    }

    /**
     * 29.2 获取链路详情（含树形节点）
     * GET /api/v1/logs/traces/:traceId
     */
    @Operation(summary = "获取链路追踪详情（含 TraceNode 树）")
    @GetMapping("/traces/{traceId}")
    public ApiResponse<Map<String, Object>> getTrace(@PathVariable String traceId) {
        Map<String, Object> trace = logsApplication.getTrace(traceId);
        if (trace == null) {
            return ApiResponse.error(404, "Trace not found: " + traceId);
        }
        return ApiResponse.success(trace);
    }

    // ==================== 30. Health（健康状态）====================

    /**
     * 30.1 获取健康状态列表
     * GET /api/v1/logs/health
     */
    @Operation(summary = "获取系统健康状态列表")
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> getHealthStatus(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String status) {
        Map<String, Object> result = logsApplication.getHealthStatus(targetType, status);
        return ApiResponse.success(result);
    }

    /**
     * 30.2 获取全局健康统计
     * GET /api/v1/logs/health/stats
     */
    @Operation(summary = "获取全局健康统计数据")
    @GetMapping("/health/stats")
    public ApiResponse<Map<String, Object>> getHealthStats() {
        Map<String, Object> stats = logsApplication.getHealthStats();
        return ApiResponse.success(stats);
    }

    // ==================== 31. Audit（审计记录）====================

    /**
     * 31.1 获取审计记录
     * GET /api/v1/logs/audit
     */
    @Operation(summary = "获取审计记录列表")
    @GetMapping("/audit")
    public ApiResponse<PageResponse<Map<String, Object>>> listAuditRecords(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String dateStart,
            @RequestParam(required = false) String dateEnd,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<Map<String, Object>> result = logsApplication.listAuditRecords(
                targetType, operator, action, dateStart, dateEnd, page, pageSize);
        return ApiResponse.success(result);
    }
}
