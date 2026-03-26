package org.dragon.observer.optimization.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.dragon.observer.optimization.plan.OptimizationAction;
import org.dragon.observer.optimization.plan.OptimizationPlan;
import org.dragon.observer.optimization.plan.OptimizationPlanItem;
import org.dragon.observer.optimization.plan.OptimizationAction.TargetType;
import org.dragon.observer.optimization.plan.OptimizationPlanItem.Status;
import org.springframework.stereotype.Component;

/**
 * MemoryOptimizationPlanStore 内存实现
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class MemoryOptimizationPlanStore implements OptimizationPlanStore {

    private final Map<String, OptimizationPlan> plans = new ConcurrentHashMap<>();
    private final Map<String, OptimizationPlanItem> items = new ConcurrentHashMap<>();

    @Override
    public OptimizationPlan save(OptimizationPlan plan) {
        plans.put(plan.getId(), plan);
        return plan;
    }

    @Override
    public Optional<OptimizationPlan> findById(String id) {
        return Optional.ofNullable(plans.get(id));
    }

    @Override
    public List<OptimizationPlan> findByEvaluationId(String evaluationId) {
        return plans.values().stream()
                .filter(p -> evaluationId.equals(p.getEvaluationId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<OptimizationPlan> findByTarget(OptimizationAction.TargetType targetType, String targetId) {
        return plans.values().stream()
                .filter(p -> p.getTargetType() == targetType && targetId.equals(p.getTargetId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<OptimizationPlan> findPendingApproval() {
        return plans.values().stream()
                .filter(p -> p.getStatus() == OptimizationPlan.Status.DRAFT)
                .collect(Collectors.toList());
    }

    @Override
    public List<OptimizationPlan> findExecuting() {
        return plans.values().stream()
                .filter(p -> p.getStatus() == OptimizationPlan.Status.EXECUTING)
                .collect(Collectors.toList());
    }

    @Override
    public List<OptimizationPlan> findByStatus(OptimizationPlan.Status status) {
        return plans.values().stream()
                .filter(p -> p.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public boolean delete(String id) {
        // 删除关联的项目
        List<OptimizationPlanItem> planItems = findItemsByPlanId(id);
        for (OptimizationPlanItem item : planItems) {
            items.remove(item.getId());
        }
        return plans.remove(id) != null;
    }

    @Override
    public int count() {
        return plans.size();
    }

    @Override
    public void clear() {
        plans.clear();
        items.clear();
    }

    // ==================== PlanItem 实现 ====================

    @Override
    public OptimizationPlanItem saveItem(OptimizationPlanItem item) {
        items.put(item.getId(), item);
        return item;
    }

    @Override
    public Optional<OptimizationPlanItem> findItemById(String id) {
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public List<OptimizationPlanItem> findItemsByPlanId(String planId) {
        return items.values().stream()
                .filter(i -> planId.equals(i.getPlanId()))
                .sorted((a, b) -> Integer.compare(a.getSequence(), b.getSequence()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateItemStatus(String id, OptimizationPlanItem.Status status) {
        OptimizationPlanItem item = items.get(id);
        if (item == null) {
            return false;
        }
        item.setStatus(status);
        return true;
    }
}