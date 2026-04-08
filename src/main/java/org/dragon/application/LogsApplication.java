package org.dragon.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.api.dto.PageResponse;
import org.dragon.character.CharacterRegistry;
import org.dragon.observer.actionlog.ObserverActionLog;
import org.dragon.observer.actionlog.ObserverActionLogService;
import org.dragon.approval.service.ApprovalService;
import org.dragon.skill.service.SkillLifecycleService;
import org.dragon.workspace.service.lifecycle.WorkspaceLifecycleService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LogsApplication 日志模块应用服务
 *
 * <p>对应前端 /logs 页面，聚合事件日志、链路追踪、健康状态、审计记录等业务逻辑。
 * 当前大部分日志数据依赖 ObserverActionLog 系统，链路追踪和健康监控为占位实现。
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogsApplication {

    private final ObserverActionLogService observerActionLogService;
    private final CharacterRegistry characterRegistry;
    private final WorkspaceLifecycleService workspaceLifecycleService;
    private final ApprovalService approvalService;
    private final SkillLifecycleService skillLifecycleService;

    // ==================== 事件日志（Event）====================

    /**
     * 分页获取事件日志列表。
     *
     * @param targetType 对象类型筛选
     * @param targetId   对象 ID 筛选
     * @param eventType  事件类型筛选
     * @param severity   严重程度筛选
     * @param search     搜索关键词
     * @param dateStart  开始时间
     * @param dateEnd    结束时间
     * @param page       页码
     * @param pageSize   每页数量
     * @return 分页事件日志
     */
    public PageResponse<Map<String, Object>> listEventLogs(String targetType, String targetId,
                                                           String eventType, String severity,
                                                           String search, String dateStart, String dateEnd,
                                                           int page, int pageSize) {
        List<ObserverActionLog> all;
        if (targetType != null && !targetType.isBlank() && targetId != null && !targetId.isBlank()) {
            all = observerActionLogService.getActionLogs(targetType.toUpperCase(), targetId);
        } else {
            all = observerActionLogService.getAllActionLogs();
        }

        List<Map<String, Object>> events = all.stream()
                .filter(l -> {
                    if (search != null && !search.isBlank()) {
                        String s = search.toLowerCase();
                        boolean match = (l.getTargetId() != null && l.getTargetId().toLowerCase().contains(s))
                                || (l.getOperator() != null && l.getOperator().toLowerCase().contains(s));
                        if (!match) return false;
                    }
                    return true;
                })
                .map(l -> toEventMap(l, severity))
                .filter(m -> {
                    if (severity != null && !severity.isBlank() && !"all".equalsIgnoreCase(severity)) {
                        return severity.equalsIgnoreCase((String) m.get("severity"));
                    }
                    return true;
                })
                .collect(Collectors.toList());

        long total = events.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, events.size());
        List<Map<String, Object>> pageData = fromIndex >= events.size() ? List.of() : events.subList(fromIndex, toIndex);
        return PageResponse.of(pageData, total, page, pageSize);
    }

    /**
     * 获取单个事件日志详情（占位）。
     *
     * @param eventId 事件 ID
     * @return 事件详情
     */
    public Map<String, Object> getEventLog(String eventId) {
        List<ObserverActionLog> all = observerActionLogService.getAllActionLogs();
        return all.stream()
                .filter(l -> eventId.equals(l.getId()))
                .findFirst()
                .map(l -> toEventMap(l, null))
                .orElse(null);
    }

    // ==================== 链路追踪（Trace）====================

    /**
     * 分页获取链路列表（占位）。
     *
     * @param targetType 对象类型
     * @param targetId   对象 ID
     * @param traceId    Trace ID 精确查找
     * @param status     状态筛选
     * @param page       页码
     * @param pageSize   每页数量
     * @return 分页链路
     */
    public PageResponse<Map<String, Object>> listTraces(String targetType, String targetId,
                                                        String traceId, String status,
                                                        int page, int pageSize) {
        log.info("[LogsApplication] listTraces targetType={} targetId={}", targetType, targetId);
        return PageResponse.of(List.of(), 0, page, pageSize);
    }

    /**
     * 获取链路详情（占位）。
     *
     * @param traceId Trace ID
     * @return 链路详情（含嵌套 TraceNode 树）
     */
    public Map<String, Object> getTrace(String traceId) {
        log.info("[LogsApplication] getTrace traceId={}", traceId);
        return null;
    }

    // ==================== 健康状态（Health）====================

    /**
     * 获取系统健康状态列表。
     *
     * @param targetType 对象类型筛选
     * @param status     状态筛选
     * @return 健康状态列表和统计
     */
    public Map<String, Object> getHealthStatus(String targetType, String status) {
        log.info("[LogsApplication] getHealthStatus targetType={} status={}", targetType, status);

        List<Map<String, Object>> items = new ArrayList<>();

        // 获取所有 Character 健康状态
        if (targetType == null || "character".equalsIgnoreCase(targetType)) {
            characterRegistry.listAll().forEach(c -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", c.getId());
                item.put("targetType", "character");
                item.put("targetId", c.getId());
                item.put("targetName", c.getName());
                item.put("status", c.getStatus() != null && c.getStatus().name().equalsIgnoreCase("RUNNING")
                        ? "healthy" : "degraded");
                item.put("lastCheckAt", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : "");
                item.put("errorCount", 0);
                items.add(item);
            });
        }

        // 获取所有 Workspace 健康状态
        if (targetType == null || "workspace".equalsIgnoreCase(targetType)) {
            workspaceLifecycleService.listWorkspaces().forEach(w -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", w.getId());
                item.put("targetType", "workspace");
                item.put("targetId", w.getId());
                item.put("targetName", w.getName());
                item.put("status", w.getStatus() != null && w.getStatus().name().equalsIgnoreCase("ACTIVE")
                        ? "healthy" : "degraded");
                item.put("lastCheckAt", w.getUpdatedAt() != null ? w.getUpdatedAt().toString() : "");
                item.put("errorCount", 0);
                items.add(item);
            });
        }

        // 按状态筛选
        List<Map<String, Object>> filtered = items.stream()
                .filter(item -> {
                    if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
                        return status.equalsIgnoreCase((String) item.get("status"));
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // 返回 Map 格式，key 为 id，value 为健康状态对象（前端期望的格式）
        Map<String, Object> result = new HashMap<>();
        for (Map<String, Object> item : filtered) {
            result.put((String) item.get("id"), item);
        }
        result.put("stats", buildHealthStats());
        return result;
    }

    /**
     * 获取全局健康统计。
     *
     * @return 健康统计数据
     */
    public Map<String, Object> getHealthStats() {
        return buildHealthStats();
    }

    // ==================== 审计记录（Audit）====================

    /**
     * 分页获取审计记录。
     *
     * @param targetType 对象类型
     * @param operator   操作者
     * @param action     操作类型
     * @param dateStart  开始时间
     * @param dateEnd    结束时间
     * @param page       页码
     * @param pageSize   每页数量
     * @return 分页审计记录
     */
    public PageResponse<Map<String, Object>> listAuditRecords(String targetType, String operator,
                                                              String action, String dateStart, String dateEnd,
                                                              int page, int pageSize) {
        List<ObserverActionLog> all;
        if (targetType != null && !targetType.isBlank()) {
            all = observerActionLogService.getAllActionLogs().stream()
                    .filter(l -> targetType.equalsIgnoreCase(l.getTargetType()))
                    .collect(Collectors.toList());
        } else {
            all = observerActionLogService.getAllActionLogs();
        }

        List<Map<String, Object>> records = all.stream()
                .filter(l -> {
                    if (operator != null && !operator.isBlank()) {
                        if (!operator.equalsIgnoreCase(l.getOperator())) return false;
                    }
                    if (action != null && !action.isBlank()) {
                        if (l.getActionType() == null || !action.equalsIgnoreCase(l.getActionType().name())) return false;
                    }
                    return true;
                })
                .map(l -> toAuditMap(l))
                .collect(Collectors.toList());

        long total = records.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, records.size());
        List<Map<String, Object>> pageData = fromIndex >= records.size() ? List.of() : records.subList(fromIndex, toIndex);
        return PageResponse.of(pageData, total, page, pageSize);
    }

    // ==================== 内部工具 ====================

    private Map<String, Object> toEventMap(ObserverActionLog log, String defaultSeverity) {
        Map<String, Object> event = new HashMap<>();
        event.put("id", log.getId());
        event.put("targetType", log.getTargetType() != null ? log.getTargetType().toLowerCase() : "");
        event.put("targetId", log.getTargetId());
        event.put("targetName", log.getTargetId()); // 使用 targetId 作为名称占位
        event.put("eventType", log.getActionType() != null ? log.getActionType().name().toLowerCase() : "");
        event.put("sourceModule", "Observer");
        event.put("operator", log.getOperator() != null ? log.getOperator() : "system");
        event.put("operatorName", log.getOperator() != null ? log.getOperator() : "系统");
        event.put("severity", "info");
        event.put("message", log.getActionType() != null ? log.getActionType().name() : "");
        event.put("details", log.getDetails());
        event.put("traceId", log.getId());
        event.put("correlationId", log.getId());
        event.put("createdAt", log.getCreatedAt() != null ? log.getCreatedAt().toString() : "");
        return event;
    }

    private Map<String, Object> toAuditMap(ObserverActionLog log) {
        Map<String, Object> audit = new HashMap<>();
        audit.put("id", log.getId());
        audit.put("operator", log.getOperator() != null ? log.getOperator() : "system");
        audit.put("operatorName", log.getOperator() != null ? log.getOperator() : "系统");
        audit.put("action", log.getActionType() != null ? log.getActionType().name() : "");
        audit.put("targetType", log.getTargetType() != null ? log.getTargetType().toLowerCase() : "");
        audit.put("targetId", log.getTargetId());
        audit.put("targetName", log.getTargetId());
        audit.put("details", log.getDetails());
        audit.put("createdAt", log.getCreatedAt() != null ? log.getCreatedAt().toString() : "");
        return audit;
    }

    private Map<String, Object> buildHealthStats() {
        long totalCharacters = characterRegistry.listAll().size();
        long activeCharacters = characterRegistry.listAll().stream()
                .filter(c -> c.getStatus() != null && c.getStatus().name().equalsIgnoreCase("RUNNING"))
                .count();
        long totalWorkspaces = workspaceLifecycleService.listWorkspaces().size();
        long activeWorkspaces = workspaceLifecycleService.countActiveWorkspaces();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCharacters", totalCharacters);
        stats.put("activeCharacters", activeCharacters);
        stats.put("totalWorkspaces", totalWorkspaces);
        stats.put("activeWorkspaces", activeWorkspaces);
        stats.put("failedSkills", 0);
        stats.put("memoryAnomalies", 0);
        stats.put("pendingApprovals", approvalService.countPendingApprovals());
        stats.put("recentFailedTasks", 0);
        stats.put("highPriorityExceptions", 0);
        return stats;
    }
}
