package org.dragon.schedule.store;

import io.ebean.Database;
import org.dragon.datasource.entity.CronDefinitionEntity;
import org.dragon.schedule.entity.CronDefinition;
import org.dragon.schedule.entity.CronStatus;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlCronStore Cron MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlCronStore implements CronStore {

    private final Database mysqlDb;

    public MySqlCronStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(CronDefinition definition) {
        CronDefinitionEntity entity = CronDefinitionEntity.fromCronDefinition(definition);
        mysqlDb.save(entity);
    }

    @Override
    public void update(CronDefinition definition) {
        CronDefinitionEntity entity = CronDefinitionEntity.fromCronDefinition(definition);
        mysqlDb.update(entity);
    }

    @Override
    public void delete(String id) {
        mysqlDb.delete(CronDefinitionEntity.class, id);
    }

    @Override
    public Optional<CronDefinition> findById(String id) {
        CronDefinitionEntity entity = mysqlDb.find(CronDefinitionEntity.class, id);
        return entity != null ? Optional.of(entity.toCronDefinition()) : Optional.empty();
    }

    @Override
    public List<CronDefinition> findAll() {
        return mysqlDb.find(CronDefinitionEntity.class)
                .findList()
                .stream()
                .map(CronDefinitionEntity::toCronDefinition)
                .collect(Collectors.toList());
    }

    @Override
    public List<CronDefinition> findByStatus(CronStatus status) {
        return mysqlDb.find(CronDefinitionEntity.class)
                .where()
                .eq("status", status.name())
                .findList()
                .stream()
                .map(CronDefinitionEntity::toCronDefinition)
                .collect(Collectors.toList());
    }

    @Override
    public void batchSave(List<CronDefinition> definitions) {
        for (CronDefinition definition : definitions) {
            save(definition);
        }
    }

    @Override
    public boolean exists(String id) {
        return mysqlDb.find(CronDefinitionEntity.class, id) != null;
    }

    @Override
    public long count() {
        return mysqlDb.find(CronDefinitionEntity.class).findCount();
    }

    @Override
    public long countByStatus(CronStatus status) {
        return mysqlDb.find(CronDefinitionEntity.class)
                .where()
                .eq("status", status.name())
                .findCount();
    }
}
