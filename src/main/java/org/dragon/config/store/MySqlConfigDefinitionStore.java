package org.dragon.config.store;

import io.ebean.Database;
import org.dragon.datasource.entity.ConfigDefinition;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


/**
 * MySQL ConfigDefinitionStore 实现
 */
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlConfigDefinitionStore implements ConfigDefinitionStore {

    private final Database mysqlDb;

    public MySqlConfigDefinitionStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(ConfigDefinition definition) {
        if (definition.getId() == null) {
            throw new IllegalArgumentException("ConfigDefinition id cannot be null");
        }
        definition.setCreatedAt(LocalDateTime.now());
        definition.setUpdatedAt(LocalDateTime.now());
        mysqlDb.save(definition);
    }

    @Override
    public void update(ConfigDefinition definition) {
        if (definition.getId() == null) {
            throw new IllegalArgumentException("ConfigDefinition id cannot be null");
        }
        definition.setUpdatedAt(LocalDateTime.now());
        mysqlDb.update(definition);
    }

    @Override
    public Optional<ConfigDefinition> findById(String id) {
        ConfigDefinition definition = mysqlDb.find(ConfigDefinition.class, id);
        return Optional.ofNullable(definition);
    }

    @Override
    public List<ConfigDefinition> findByScopeType(String scopeType) {
        return mysqlDb.find(ConfigDefinition.class)
                .where()
                .eq("scope_type", scopeType)
                .findList();
    }

    @Override
    public Optional<ConfigDefinition> findByScopeTypeAndKey(String scopeType, String configKey) {
        ConfigDefinition definition = mysqlDb.find(ConfigDefinition.class)
                .where()
                .eq("scope_type", scopeType)
                .eq("config_key", configKey)
                .findOne();
        return Optional.ofNullable(definition);
    }

    @Override
    public List<ConfigDefinition> findAll() {
        return mysqlDb.find(ConfigDefinition.class).findList();
    }

    @Override
    public void delete(String id) {
        mysqlDb.delete(ConfigDefinition.class, id);
    }

    @Override
    public boolean exists(String scopeType, String configKey) {
        return findByScopeTypeAndKey(scopeType, configKey).isPresent();
    }
}