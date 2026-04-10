package org.dragon.observer.store;

import io.ebean.Database;
import org.dragon.datasource.entity.ModificationLogEntity;
import org.dragon.observer.log.ModificationLog;
import org.dragon.observer.log.ModificationLog.TargetType;
import org.dragon.observer.log.ModificationLog.TriggerSource;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlModificationLogStore 修改日志MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlModificationLogStore implements ModificationLogStore {

    private final Database mysqlDb;

    public MySqlModificationLogStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public ModificationLog save(ModificationLog log) {
        ModificationLogEntity entity = ModificationLogEntity.fromModificationLog(log);
        mysqlDb.save(entity);
        return log;
    }

    @Override
    public Optional<ModificationLog> findById(String id) {
        ModificationLogEntity entity = mysqlDb.find(ModificationLogEntity.class, id);
        return entity != null ? Optional.of(entity.toModificationLog()) : Optional.empty();
    }

    @Override
    public List<ModificationLog> findByTarget(ModificationLog.TargetType targetType, String targetId) {
        return mysqlDb.find(ModificationLogEntity.class)
                .where()
                .eq("targetType", targetType.name())
                .eq("targetId", targetId)
                .orderBy()
                .desc("timestamp")
                .findList()
                .stream()
                .map(ModificationLogEntity::toModificationLog)
                .collect(Collectors.toList());
    }

    @Override
    public List<ModificationLog> findRecentByTarget(ModificationLog.TargetType targetType, String targetId, int limit) {
        return mysqlDb.find(ModificationLogEntity.class)
                .where()
                .eq("targetType", targetType.name())
                .eq("targetId", targetId)
                .orderBy()
                .desc("timestamp")
                .setMaxRows(limit)
                .findList()
                .stream()
                .map(ModificationLogEntity::toModificationLog)
                .collect(Collectors.toList());
    }

    @Override
    public List<ModificationLog> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return mysqlDb.find(ModificationLogEntity.class)
                .where()
                .ge("timestamp", startTime)
                .le("timestamp", endTime)
                .orderBy()
                .desc("timestamp")
                .findList()
                .stream()
                .map(ModificationLogEntity::toModificationLog)
                .collect(Collectors.toList());
    }

    @Override
    public List<ModificationLog> findByOperator(String operator) {
        return mysqlDb.find(ModificationLogEntity.class)
                .where()
                .eq("operator", operator)
                .orderBy()
                .desc("timestamp")
                .findList()
                .stream()
                .map(ModificationLogEntity::toModificationLog)
                .collect(Collectors.toList());
    }

    @Override
    public List<ModificationLog> findByTriggerSource(ModificationLog.TriggerSource triggerSource) {
        return mysqlDb.find(ModificationLogEntity.class)
                .where()
                .eq("triggerSource", triggerSource.name())
                .orderBy()
                .desc("timestamp")
                .findList()
                .stream()
                .map(ModificationLogEntity::toModificationLog)
                .collect(Collectors.toList());
    }

    @Override
    public boolean delete(String id) {
        int rows = mysqlDb.find(ModificationLogEntity.class, id) != null ? 1 : 0;
        mysqlDb.delete(ModificationLogEntity.class, id);
        return rows > 0;
    }

    @Override
    public int count() {
        return (int) mysqlDb.find(ModificationLogEntity.class).findCount();
    }

    @Override
    public void clear() {
        mysqlDb.find(ModificationLogEntity.class).delete();
    }
}
