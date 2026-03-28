package org.dragon.observer.optimization.store;

import io.ebean.Database;
import org.dragon.datasource.entity.OptimizationActionEntity;
import org.dragon.observer.optimization.plan.OptimizationAction;
import org.dragon.observer.optimization.plan.OptimizationAction.Status;
import org.dragon.observer.optimization.plan.OptimizationAction.TargetType;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlOptimizationActionStore 优化动作MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlOptimizationActionStore implements OptimizationActionStore {

    private final Database mysqlDb;

    public MySqlOptimizationActionStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public OptimizationAction save(OptimizationAction action) {
        if (action.getCreatedAt() == null) {
            action.setCreatedAt(LocalDateTime.now());
        }
        OptimizationActionEntity entity = OptimizationActionEntity.fromOptimizationAction(action);
        mysqlDb.save(entity);
        return action;
    }

    @Override
    public Optional<OptimizationAction> findById(String id) {
        OptimizationActionEntity entity = mysqlDb.find(OptimizationActionEntity.class, id);
        return entity != null ? Optional.of(entity.toOptimizationAction()) : Optional.empty();
    }

    @Override
    public List<OptimizationAction> findByEvaluationId(String evaluationId) {
        return mysqlDb.find(OptimizationActionEntity.class)
                .where()
                .eq("evaluationId", evaluationId)
                .findList()
                .stream()
                .map(OptimizationActionEntity::toOptimizationAction)
                .collect(Collectors.toList());
    }

    @Override
    public List<OptimizationAction> findPending() {
        return mysqlDb.find(OptimizationActionEntity.class)
                .where()
                .eq("status", OptimizationAction.Status.PENDING.name())
                .findList()
                .stream()
                .map(OptimizationActionEntity::toOptimizationAction)
                .collect(Collectors.toList());
    }

    @Override
    public List<OptimizationAction> findPendingOrdered(int limit) {
        return mysqlDb.find(OptimizationActionEntity.class)
                .where()
                .eq("status", OptimizationAction.Status.PENDING.name())
                .orderBy()
                .asc("priority")
                .setMaxRows(limit)
                .findList()
                .stream()
                .map(OptimizationActionEntity::toOptimizationAction)
                .collect(Collectors.toList());
    }

    @Override
    public List<OptimizationAction> findByTarget(OptimizationAction.TargetType targetType, String targetId) {
        return mysqlDb.find(OptimizationActionEntity.class)
                .where()
                .eq("targetType", targetType.name())
                .eq("targetId", targetId)
                .findList()
                .stream()
                .map(OptimizationActionEntity::toOptimizationAction)
                .collect(Collectors.toList());
    }

    @Override
    public List<OptimizationAction> findByStatus(OptimizationAction.Status status) {
        return mysqlDb.find(OptimizationActionEntity.class)
                .where()
                .eq("status", status.name())
                .findList()
                .stream()
                .map(OptimizationActionEntity::toOptimizationAction)
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateStatus(String id, OptimizationAction.Status status) {
        OptimizationActionEntity entity = mysqlDb.find(OptimizationActionEntity.class, id);
        if (entity == null) {
            return false;
        }

        entity.setStatus(status.name());
        switch (status) {
            case EXECUTED:
                entity.setExecutedAt(LocalDateTime.now());
                break;
            case ROLLED_BACK:
                entity.setRolledBackAt(LocalDateTime.now());
                break;
            default:
                break;
        }
        mysqlDb.update(entity);
        return true;
    }

    @Override
    public boolean delete(String id) {
        int rows = mysqlDb.find(OptimizationActionEntity.class, id) != null ? 1 : 0;
        mysqlDb.delete(OptimizationActionEntity.class, id);
        return rows > 0;
    }

    @Override
    public int count() {
        return (int) mysqlDb.find(OptimizationActionEntity.class).findCount();
    }

    @Override
    public void clear() {
        mysqlDb.find(OptimizationActionEntity.class).delete();
    }
}
