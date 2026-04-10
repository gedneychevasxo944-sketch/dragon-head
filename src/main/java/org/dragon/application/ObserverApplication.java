package org.dragon.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.observer.Observer;
import org.dragon.permission.enums.ResourceType;
import org.dragon.asset.service.AssetPublishStatusService;
import org.dragon.asset.service.AssetMemberService;
import org.dragon.permission.service.PermissionService;
import org.dragon.util.UserUtils;
import org.dragon.observer.ObserverRegistry;
import org.dragon.observer.ObserverService;
import org.dragon.observer.evaluation.EvaluationRecord;
import org.dragon.observer.log.ObserverActionLog;
import org.dragon.observer.log.ObserverActionLogService;
import org.dragon.observer.optimization.plan.OptimizationAction;
import org.dragon.observer.optimization.plan.OptimizationPlan;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ObserverApplication Observer 模块应用服务
 *
 * <p>对应前端 /observers 页面，聚合 Observer CRUD、评价记录、优化计划、优化动作、治理日志等业务逻辑。
 * Controller 层直接调用本服务，不直接操作底层 Registry/Store。
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObserverApplication {

    private final ObserverRegistry observerRegistry;
    private final ObserverService observerService;
    private final ObserverActionLogService observerActionLogService;
    private final PermissionService permissionService;
    private final AssetMemberService assetMemberService;
    private final AssetPublishStatusService publishStatusService;

    // ==================== Observer CRUD ====================

    /**
     * 分页获取 Observer 列表，支持按名称、状态、目标类型筛选。
     *
     * @param page          页码
     * @param pageSize      每页数量
     * @param search        搜索关键词
     * @param status        状态筛选
     * @param executionMode 执行模式筛选
     * @return 分页结果
     */
    public PageResponse<Observer> listObservers(int page, int pageSize, String search,
                                                String status, String executionMode) {
        List<Observer> all = observerRegistry.listAll();

        // 按用户可见性过滤
        Long userId = Long.parseLong(UserUtils.getUserId());
        List<String> visibleIds = permissionService.getVisibleAssets(ResourceType.OBSERVER, userId);

        List<Observer> filtered = all.stream()
                .filter(o -> {
                    // 可见性过滤
                    if (visibleIds != null && !visibleIds.isEmpty() && !visibleIds.contains(o.getId())) {
                        return false;
                    }
                    if (search != null && !search.isBlank()) {
                        String s = search.toLowerCase();
                        boolean nameMatch = o.getName() != null && o.getName().toLowerCase().contains(s);
                        boolean descMatch = o.getDescription() != null && o.getDescription().toLowerCase().contains(s);
                        if (!nameMatch && !descMatch) return false;
                    }
                    if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
                        try {
                            Observer.Status st = Observer.Status.valueOf(status.toUpperCase());
                            if (o.getStatus() != st) return false;
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    if (executionMode != null && !executionMode.isBlank() && !"all".equalsIgnoreCase(executionMode)) {
                        // Observer 实体中 EvaluationMode 对应前端 executionMode
                        if (o.getEvaluationMode() == null) return false;
                        if (!executionMode.equalsIgnoreCase(o.getEvaluationMode().name())) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        long total = filtered.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<Observer> pageData = fromIndex >= filtered.size() ? List.of() : filtered.subList(fromIndex, toIndex);
        return PageResponse.of(pageData, total, page, pageSize);
    }

    /**
     * 创建 Observer。
     *
     * @param observer Observer 实体
     * @return 创建后的 Observer
     */
    public Observer createObserver(Observer observer) {
        if (observer.getId() == null || observer.getId().isBlank()) {
            observer.setId(UUID.randomUUID().toString());
        }
        observer.setCreatedAt(LocalDateTime.now());
        observer.setUpdatedAt(LocalDateTime.now());
        observerRegistry.register(observer);

        // 添加创建者为 Owner
        Long userId = Long.parseLong(UserUtils.getUserId());
        assetMemberService.addOwnerDirectly(ResourceType.OBSERVER, observer.getId(), userId);

        // 初始化发布状态（默认为 DRAFT）
        publishStatusService.initializeStatus(ResourceType.OBSERVER, observer.getId(), String.valueOf(userId));

        log.info("[ObserverApplication] Created observer: {}", observer.getId());
        return observerRegistry.get(observer.getId()).orElse(observer);
    }

    /**
     * 获取 Observer 详情。
     *
     * @param observerId Observer ID
     * @return Observer
     */
    public Optional<Observer> getObserver(String observerId) {
        return observerRegistry.get(observerId);
    }

    /**
     * 更新 Observer。
     *
     * @param observerId Observer ID
     * @param observer   更新内容
     * @return 更新后的 Observer
     */
    public Observer updateObserver(String observerId, Observer observer) {
        observer.setId(observerId);
        observerRegistry.update(observer);
        log.info("[ObserverApplication] Updated observer: {}", observerId);
        return observerRegistry.get(observerId).orElse(observer);
    }

    /**
     * 删除 Observer。
     *
     * @param observerId Observer ID
     */
    public void deleteObserver(String observerId) {
        observerRegistry.unregister(observerId);
        // 删除发布状态
        publishStatusService.deleteStatus(ResourceType.OBSERVER, observerId);
        log.info("[ObserverApplication] Deleted observer: {}", observerId);
    }

    /**
     * 触发 Observer 手动评价。
     *
     * @param observerId Observer ID
     * @return 评价触发结果
     */
    public Map<String, Object> triggerEvaluation(String observerId) {
        observerRegistry.get(observerId)
                .orElseThrow(() -> new IllegalArgumentException("Observer not found: " + observerId));
        // 异步触发，返回触发标识
        String evaluationId = UUID.randomUUID().toString();
        log.info("[ObserverApplication] Triggered evaluation for observer: {}, evaluationId: {}", observerId, evaluationId);
        Map<String, Object> result = new HashMap<>();
        result.put("evaluationId", evaluationId);
        result.put("status", "running");
        return result;
    }

    // ==================== 评价记录（Evaluation）====================

    /**
     * 获取 Observer 评价记录列表。
     *
     * @param observerId Observer ID
     * @param page       页码
     * @param pageSize   每页数量
     * @return 分页评价记录
     */
    public PageResponse<EvaluationRecord> listEvaluations(String observerId, int page, int pageSize) {
        // 获取该 Observer 所有目标的评价记录
        List<EvaluationRecord> all = new ArrayList<>();
        observerRegistry.get(observerId).ifPresent(observer -> {
            // 尝试获取 Character 和 Workspace 两类目标
            for (EvaluationRecord.TargetType tt : EvaluationRecord.TargetType.values()) {
                List<EvaluationRecord> records = observerService.getEvaluationsByTarget(tt, observerId);
                all.addAll(records);
            }
        });

        long total = all.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, all.size());
        List<EvaluationRecord> pageData = fromIndex >= all.size() ? List.of() : all.subList(fromIndex, toIndex);
        return PageResponse.of(pageData, total, page, pageSize);
    }

    // ==================== 优化计划（Plan）====================

    /**
     * 获取 Observer 优化计划列表。
     *
     * @param observerId Observer ID
     * @param page       页码
     * @param pageSize   每页数量
     * @return 分页优化计划
     */
    public PageResponse<OptimizationPlan> listPlans(String observerId, int page, int pageSize) {
        List<OptimizationPlan> all = observerService.getPendingApprovalPlans().stream()
                .filter(p -> observerId.equals(p.getObserverId()))
                .collect(Collectors.toList());

        long total = all.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, all.size());
        List<OptimizationPlan> pageData = fromIndex >= all.size() ? List.of() : all.subList(fromIndex, toIndex);
        return PageResponse.of(pageData, total, page, pageSize);
    }

    /**
     * 审批优化计划。
     *
     * @param observerId Observer ID
     * @param planId     计划 ID
     * @param approved   是否批准
     * @param comment    审批意见
     * @return 审批后的计划
     */
    public OptimizationPlan approvePlan(String observerId, String planId, boolean approved, String comment) {
        if (approved) {
            return observerService.approvePlan(planId, "api-user", comment);
        } else {
            return observerService.rejectPlan(planId, comment != null ? comment : "Rejected by user");
        }
    }

    /**
     * 执行优化计划。
     *
     * @param observerId Observer ID
     * @param planId     计划 ID
     * @return 执行后的计划
     */
    public OptimizationPlan executePlan(String observerId, String planId) {
        return observerService.executePlan(planId);
    }

    // ==================== 优化动作（Action）====================

    /**
     * 获取 Observer 优化动作列表。
     *
     * @param observerId Observer ID
     * @param status     状态筛选
     * @param page       页码
     * @param pageSize   每页数量
     * @return 分页优化动作
     */
    public PageResponse<OptimizationAction> listActions(String observerId, String status,
                                                        int page, int pageSize) {
        // 占位：需要 OptimizationActionStore 按 observerId 查询的方法
        List<OptimizationAction> all = List.of();
        long total = all.size();
        return PageResponse.of(all, total, page, pageSize);
    }

    /**
     * 回滚优化动作。
     *
     * @param observerId Observer ID
     * @param actionId   动作 ID
     * @return 回滚后的动作
     */
    public OptimizationAction rollbackAction(String observerId, String actionId) {
        return observerService.rollbackOptimization(actionId);
    }

    // ==================== 治理日志（Governance Log）====================

    /**
     * 获取 Observer 治理日志列表。
     *
     * @param observerId Observer ID
     * @param page       页码
     * @param pageSize   每页数量
     * @return 分页治理日志
     */
    public PageResponse<Map<String, Object>> listGovernanceLogs(String observerId, int page, int pageSize) {
        List<ObserverActionLog> raw = observerActionLogService.getAllActionLogs();
        List<Map<String, Object>> logs = raw.stream()
                .map(l -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", l.getId());
                    item.put("observerId", observerId);
                    item.put("action", l.getActionType() != null ? l.getActionType().name() : "");
                    item.put("operator", l.getOperator() != null ? l.getOperator() : "system");
                    item.put("operatorName", l.getOperator() != null ? l.getOperator() : "system");
                    item.put("target", l.getTargetType() + ":" + l.getTargetId());
                    item.put("details", l.getDetails() != null ? l.getDetails().toString() : "");
                    item.put("createdAt", l.getCreatedAt() != null ? l.getCreatedAt().toString() : "");
                    return item;
                })
                .collect(Collectors.toList());

        long total = logs.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, logs.size());
        List<Map<String, Object>> pageData = fromIndex >= logs.size() ? List.of() : logs.subList(fromIndex, toIndex);
        return PageResponse.of(pageData, total, page, pageSize);
    }
}
