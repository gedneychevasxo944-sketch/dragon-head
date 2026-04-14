package org.dragon.agent.model.store;

import io.ebean.Database;
import org.dragon.agent.model.ModelInstance;
import org.dragon.datasource.entity.ModelInstanceEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlModelStore 模型实例MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlModelStore implements ModelStore {

    private final Database mysqlDb;

    public MySqlModelStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(ModelInstance modelInstance) {
        ModelInstanceEntity entity = ModelInstanceEntity.fromModelInstance(modelInstance);
        mysqlDb.save(entity);
    }

    @Override
    public void update(ModelInstance modelInstance) {
        ModelInstanceEntity entity = ModelInstanceEntity.fromModelInstance(modelInstance);
        mysqlDb.update(entity);
    }

    @Override
    public void delete(String id) {
        mysqlDb.delete(ModelInstanceEntity.class, id);
    }

    @Override
    public Optional<ModelInstance> findById(String id) {
        ModelInstanceEntity entity = mysqlDb.find(ModelInstanceEntity.class, id);
        return entity != null ? Optional.of(entity.toModelInstance()) : Optional.empty();
    }

    @Override
    public List<ModelInstance> findAll() {
        return mysqlDb.find(ModelInstanceEntity.class)
                .findList()
                .stream()
                .map(ModelInstanceEntity::toModelInstance)
                .collect(Collectors.toList());
    }

    @Override
    public List<ModelInstance> findByProvider(ModelInstance.ModelProvider provider) {
        return mysqlDb.find(ModelInstanceEntity.class)
                .where()
                .eq("provider", provider.name())
                .findList()
                .stream()
                .map(ModelInstanceEntity::toModelInstance)
                .collect(Collectors.toList());
    }

    @Override
    public List<ModelInstance> findEnabled() {
        return mysqlDb.find(ModelInstanceEntity.class)
                .where()
                .eq("enabled", true)
                .findList()
                .stream()
                .map(ModelInstanceEntity::toModelInstance)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String id) {
        return mysqlDb.find(ModelInstanceEntity.class, id) != null;
    }

    @Override
    public int count() {
        return (int) mysqlDb.find(ModelInstanceEntity.class).findCount();
    }
}