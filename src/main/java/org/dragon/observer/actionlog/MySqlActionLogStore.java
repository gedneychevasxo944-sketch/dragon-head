package org.dragon.observer.actionlog;

import io.ebean.Database;
import org.dragon.datasource.entity.ObserverActionLogEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MySqlActionLogStore 观察者动作日志MySQL存储实现
 */
@Component
public class MySqlActionLogStore implements ActionLogStore {

    private final Database mysqlDb;

    public MySqlActionLogStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(ObserverActionLog log) {
        ObserverActionLogEntity entity = ObserverActionLogEntity.fromObserverActionLog(log);
        mysqlDb.save(entity);
    }

    @Override
    public List<ObserverActionLog> findByTarget(String targetType, String targetId) {
        return mysqlDb.find(ObserverActionLogEntity.class)
                .where()
                .eq("targetType", targetType)
                .eq("targetId", targetId)
                .orderBy()
                .desc("createdAt")
                .findList()
                .stream()
                .map(ObserverActionLogEntity::toObserverActionLog)
                .collect(Collectors.toList());
    }

    @Override
    public List<ObserverActionLog> findByActionType(ActionType actionType) {
        return mysqlDb.find(ObserverActionLogEntity.class)
                .where()
                .eq("actionType", actionType.name())
                .orderBy()
                .desc("createdAt")
                .findList()
                .stream()
                .map(ObserverActionLogEntity::toObserverActionLog)
                .collect(Collectors.toList());
    }

    @Override
    public List<ObserverActionLog> findByTargetAndActionType(String targetType, String targetId, ActionType actionType) {
        return mysqlDb.find(ObserverActionLogEntity.class)
                .where()
                .eq("targetType", targetType)
                .eq("targetId", targetId)
                .eq("actionType", actionType.name())
                .orderBy()
                .desc("createdAt")
                .findList()
                .stream()
                .map(ObserverActionLogEntity::toObserverActionLog)
                .collect(Collectors.toList());
    }

    @Override
    public List<ObserverActionLog> findAll() {
        return mysqlDb.find(ObserverActionLogEntity.class)
                .orderBy()
                .desc("createdAt")
                .findList()
                .stream()
                .map(ObserverActionLogEntity::toObserverActionLog)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String id) {
        mysqlDb.delete(ObserverActionLogEntity.class, id);
    }

    @Override
    public void clear() {
        mysqlDb.find(ObserverActionLogEntity.class).delete();
    }
}
