package org.dragon.tools.store;

import io.ebean.Database;
import org.dragon.datasource.entity.ToolEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlToolStore 工具MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlToolStore implements ToolStore {

    private final Database mysqlDb;

    public MySqlToolStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(Map<String, Object> toolMetadata) {
        ToolEntity entity = new ToolEntity();
        entity.setName((String) toolMetadata.get("name"));
        entity.setDescription((String) toolMetadata.get("description"));
        entity.setParameterSchema((String) toolMetadata.get("parameterSchema"));
        entity.setEnabled((Boolean) toolMetadata.getOrDefault("enabled", true));

        // Check if exists, then update or insert
        ToolEntity existing = mysqlDb.find(ToolEntity.class, entity.getName());
        if (existing != null) {
            mysqlDb.update(entity);
        } else {
            mysqlDb.insert(entity);
        }
    }

    @Override
    public void update(Map<String, Object> toolMetadata) {
        ToolEntity entity = new ToolEntity();
        entity.setName((String) toolMetadata.get("name"));
        entity.setDescription((String) toolMetadata.get("description"));
        entity.setParameterSchema((String) toolMetadata.get("parameterSchema"));
        entity.setEnabled((Boolean) toolMetadata.getOrDefault("enabled", true));
        mysqlDb.update(entity);
    }

    @Override
    public void delete(String name) {
        mysqlDb.delete(ToolEntity.class, name);
    }

    @Override
    public Optional<Map<String, Object>> findByName(String name) {
        ToolEntity entity = mysqlDb.find(ToolEntity.class, name);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(entityToMap(entity));
    }

    @Override
    public List<Map<String, Object>> findAll() {
        return mysqlDb.find(ToolEntity.class)
                .findList()
                .stream()
                .map(this::entityToMap)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> findEnabled() {
        return mysqlDb.find(ToolEntity.class)
                .where()
                .eq("enabled", true)
                .findList()
                .stream()
                .map(this::entityToMap)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String name) {
        return mysqlDb.find(ToolEntity.class, name) != null;
    }

    @Override
    public int count() {
        return (int) mysqlDb.find(ToolEntity.class).findCount();
    }

    private Map<String, Object> entityToMap(ToolEntity entity) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", entity.getName());
        map.put("description", entity.getDescription());
        map.put("parameterSchema", entity.getParameterSchema());
        map.put("enabled", entity.getEnabled());
        return map;
    }
}