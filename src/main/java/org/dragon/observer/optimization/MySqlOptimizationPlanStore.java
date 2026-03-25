package org.dragon.observer.optimization;

import io.ebean.Database;
import org.dragon.datasource.entity.OptimizationPlanEntity;
import org.dragon.datasource.entity.OptimizationPlanItemEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlOptimizationPlanStore 优化计划MySQL存储实现
 */
@Component
public class MySqlOptimizationPlanStore implements OptimizationPlanStore {

    private final Database mysqlDb;

    public MySqlOptimizationPlanStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public OptimizationPlan save(OptimizationPlan plan) {
        OptimizationPlanEntity entity = OptimizationPlanEntity.fromOptimizationPlan(plan);
        mysqlDb.save(entity);
        return plan;
    }

    @Override
    public Optional<OptimizationPlan> findById(String id) {
        OptimizationPlanEntity entity = mysqlDb.find(OptimizationPlanEntity.class, id);
        return entity != null ? Optional.of(entity.toOptimizationPlan()) : Optional.empty();
    }

    @Override
    public List<OptimizationPlan> findByEvaluationId(String evaluationId) {
        return mysqlDb.find(OptimizationPlanEntity.class)
                .where()
                .eq("evaluationId", evaluationId)
                .findList()
                .stream()
                .map(OptimizationPlanEntity::toOptimizationPlan)
                .collect(Collectors.toList());
    }

    @Override
    public List<OptimizationPlan> findByTarget(OptimizationAction.TargetType targetType, String targetId) {
        return mysqlDb.find(OptimizationPlanEntity.class)
                .where()
                .eq("targetType", targetType.name())
                .eq("targetId", targetId)
                .findList()
                .stream()
                .map(OptimizationPlanEntity::toOptimizationPlan)
                .collect(Collectors.toList());
    }

    @Override
    public List<OptimizationPlan> findPendingApproval() {
        return mysqlDb.find(OptimizationPlanEntity.class)
                .where()
                .eq("status", OptimizationPlan.Status.DRAFT.name())
                .findList()
                .stream()
                .map(OptimizationPlanEntity::toOptimizationPlan)
                .collect(Collectors.toList());
    }

    @Override
    public List<OptimizationPlan> findExecuting() {
        return mysqlDb.find(OptimizationPlanEntity.class)
                .where()
                .eq("status", OptimizationPlan.Status.EXECUTING.name())
                .findList()
                .stream()
                .map(OptimizationPlanEntity::toOptimizationPlan)
                .collect(Collectors.toList());
    }

    @Override
    public List<OptimizationPlan> findByStatus(OptimizationPlan.Status status) {
        return mysqlDb.find(OptimizationPlanEntity.class)
                .where()
                .eq("status", status.name())
                .findList()
                .stream()
                .map(OptimizationPlanEntity::toOptimizationPlan)
                .collect(Collectors.toList());
    }

    @Override
    public boolean delete(String id) {
        // 删除关联的项目
        mysqlDb.find(OptimizationPlanItemEntity.class)
                .where()
                .eq("planId", id)
                .delete();

        int rows = mysqlDb.find(OptimizationPlanEntity.class, id) != null ? 1 : 0;
        mysqlDb.delete(OptimizationPlanEntity.class, id);
        return rows > 0;
    }

    @Override
    public int count() {
        return (int) mysqlDb.find(OptimizationPlanEntity.class).findCount();
    }

    @Override
    public void clear() {
        mysqlDb.find(OptimizationPlanItemEntity.class).delete();
        mysqlDb.find(OptimizationPlanEntity.class).delete();
    }

    // ==================== PlanItem 实现 ====================

    @Override
    public OptimizationPlanItem saveItem(OptimizationPlanItem item) {
        OptimizationPlanItemEntity entity = OptimizationPlanItemEntity.fromOptimizationPlanItem(item);
        mysqlDb.save(entity);
        return item;
    }

    @Override
    public Optional<OptimizationPlanItem> findItemById(String id) {
        OptimizationPlanItemEntity entity = mysqlDb.find(OptimizationPlanItemEntity.class, id);
        return entity != null ? Optional.of(entity.toOptimizationPlanItem()) : Optional.empty();
    }

    @Override
    public List<OptimizationPlanItem> findItemsByPlanId(String planId) {
        return mysqlDb.find(OptimizationPlanItemEntity.class)
                .where()
                .eq("planId", planId)
                .orderBy()
                .asc("sequence")
                .findList()
                .stream()
                .map(OptimizationPlanItemEntity::toOptimizationPlanItem)
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateItemStatus(String id, OptimizationPlanItem.Status status) {
        OptimizationPlanItemEntity entity = mysqlDb.find(OptimizationPlanItemEntity.class, id);
        if (entity == null) {
            return false;
        }
        entity.setStatus(status.name());
        mysqlDb.update(entity);
        return true;
    }
}
