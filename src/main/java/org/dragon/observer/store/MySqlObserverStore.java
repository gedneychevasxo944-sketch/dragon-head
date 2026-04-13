package org.dragon.observer.store;

import io.ebean.Database;
import org.dragon.datasource.entity.ObserverEntity;
import org.dragon.observer.Observer;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlObserverStore Observer MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlObserverStore implements ObserverStore {

    private final Database mysqlDb;

    public MySqlObserverStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(Observer observer) {
        ObserverEntity entity = ObserverEntity.fromObserver(observer);
        mysqlDb.save(entity);
    }

    @Override
    public void update(Observer observer) {
        ObserverEntity entity = ObserverEntity.fromObserver(observer);
        // Fetch existing entity first to preserve fields not being updated
        ObserverEntity existing = mysqlDb.find(ObserverEntity.class, observer.getId());
        if (existing != null) {
            entity.setCreatedAt(existing.getCreatedAt());
            mergeIfNotNull(entity, existing);
        }
        mysqlDb.update(entity);
    }

    private void mergeIfNotNull(ObserverEntity target, ObserverEntity source) {
        if (target.getName() == null) {
            target.setName(source.getName());
        }
        if (target.getDescription() == null) {
            target.setDescription(source.getDescription());
        }
        if (target.getWorkspaceId() == null) {
            target.setWorkspaceId(source.getWorkspaceId());
        }
        if (target.getStatus() == null) {
            target.setStatus(source.getStatus());
        }
        if (target.getEvaluationMode() == null) {
            target.setEvaluationMode(source.getEvaluationMode());
        }
        if (target.getOptimizationThreshold() == null) {
            target.setOptimizationThreshold(source.getOptimizationThreshold());
        }
        if (target.getConsecutiveLowScoreThreshold() == null) {
            target.setConsecutiveLowScoreThreshold(source.getConsecutiveLowScoreThreshold());
        }
        if (target.getCommonSenseEnabled() == null) {
            target.setCommonSenseEnabled(source.getCommonSenseEnabled());
        }
        if (target.getAutoOptimizationEnabled() == null) {
            target.setAutoOptimizationEnabled(source.getAutoOptimizationEnabled());
        }
        if (target.getPeriodicEvaluationHours() == null) {
            target.setPeriodicEvaluationHours(source.getPeriodicEvaluationHours());
        }
        if (target.getProperties() == null) {
            target.setProperties(source.getProperties());
        }
        if (target.getPlannerCharacterIds() == null) {
            target.setPlannerCharacterIds(source.getPlannerCharacterIds());
        }
        if (target.getReviewerCharacterIds() == null) {
            target.setReviewerCharacterIds(source.getReviewerCharacterIds());
        }
        if (target.getSupportedTargetTypes() == null) {
            target.setSupportedTargetTypes(source.getSupportedTargetTypes());
        }
        if (target.getManualApprovalRequired() == null) {
            target.setManualApprovalRequired(source.getManualApprovalRequired());
        }
        if (target.getScheduleCron() == null) {
            target.setScheduleCron(source.getScheduleCron());
        }
        if (target.getPlanWindowHours() == null) {
            target.setPlanWindowHours(source.getPlanWindowHours());
        }
        if (target.getMaxPlanItems() == null) {
            target.setMaxPlanItems(source.getMaxPlanItems());
        }
    }

    @Override
    public void delete(String id) {
        mysqlDb.delete(ObserverEntity.class, id);
    }

    @Override
    public Optional<Observer> findById(String id) {
        ObserverEntity entity = mysqlDb.find(ObserverEntity.class, id);
        return entity != null ? Optional.of(entity.toObserver()) : Optional.empty();
    }

    @Override
    public List<Observer> findAll() {
        return mysqlDb.find(ObserverEntity.class)
                .findList()
                .stream()
                .map(ObserverEntity::toObserver)
                .collect(Collectors.toList());
    }

    @Override
    public List<Observer> findByWorkspaceId(String workspaceId) {
        return mysqlDb.find(ObserverEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .findList()
                .stream()
                .map(ObserverEntity::toObserver)
                .collect(Collectors.toList());
    }

    @Override
    public List<Observer> findActive() {
        return mysqlDb.find(ObserverEntity.class)
                .where()
                .eq("status", Observer.Status.ACTIVE.name())
                .findList()
                .stream()
                .map(ObserverEntity::toObserver)
                .collect(Collectors.toList());
    }

    @Override
    public List<Observer> findByStatus(Observer.Status status) {
        return mysqlDb.find(ObserverEntity.class)
                .where()
                .eq("status", status.name())
                .findList()
                .stream()
                .map(ObserverEntity::toObserver)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String id) {
        return mysqlDb.find(ObserverEntity.class, id) != null;
    }

    @Override
    public int count() {
        return (int) mysqlDb.find(ObserverEntity.class).findCount();
    }
}