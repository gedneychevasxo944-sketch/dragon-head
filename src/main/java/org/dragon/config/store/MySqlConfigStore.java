package org.dragon.config.store;

import io.ebean.Database;
import org.dragon.datasource.entity.ConfigEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MySqlConfigStore 配置存储 MySQL 实现
 *
 * <p>适配新的 ConfigEntity 结构：
 * <ul>
 *   <li>id = fullKey = {scopeType}:{scopeId}:{targetType}:{targetId}:{configKey}</li>
 *   <li>支持 workspace/entityType/entityId 到 scopeType/scopeId 的迁移</li>
 * </ul>
 */
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlConfigStore implements ConfigStore {

    private final Database mysqlDb;

    public MySqlConfigStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void set(ConfigKey configKey, Object value) {
        if (configKey == null || configKey.getKey() == null) {
            throw new IllegalArgumentException("configKey and key cannot be null");
        }
        String id = configKey.toFullKey();
        ConfigEntity existing = mysqlDb.find(ConfigEntity.class, id);

        if (existing != null) {
            existing.setConfigValue(value);
            existing.setUpdatedAt(LocalDateTime.now());
            mysqlDb.update(existing);
        } else {
            ConfigEntity entity = ConfigEntity.builder()
                    .id(id)
                    .scopeType(configKey.getScopeType())
                    .scopeId(configKey.getScopeId())
                    .targetType(configKey.getTargetType())
                    .targetId(configKey.getTargetId())
                    .configKey(configKey.getKey())
                    .configValue(value)
                    .status("PUBLISHED")
                    .version(1)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            mysqlDb.save(entity);
        }
    }

    @Override
    public Optional<Object> get(ConfigKey configKey) {
        if (configKey == null || configKey.getKey() == null) {
            return Optional.empty();
        }
        ConfigEntity entity = mysqlDb.find(ConfigEntity.class, configKey.toFullKey());
        return Optional.ofNullable(entity != null ? entity.getConfigValue() : null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(ConfigKey configKey, T defaultValue) {
        return (T) get(configKey).orElse(defaultValue);
    }

    @Override
    public void delete(ConfigKey configKey) {
        if (configKey == null || configKey.getKey() == null) {
            return;
        }
        mysqlDb.delete(ConfigEntity.class, configKey.toFullKey());
    }

    @Override
    public boolean exists(ConfigKey configKey) {
        if (configKey == null || configKey.getKey() == null) {
            return false;
        }
        return mysqlDb.find(ConfigEntity.class, configKey.toFullKey()) != null;
    }

    @Override
    public Map<String, Object> getAll(ConfigKey configKey) {
        if (configKey == null) {
            return new HashMap<>();
        }

        String prefix = buildPrefix(configKey);
        List<ConfigEntity> entities = mysqlDb.find(ConfigEntity.class)
                .where()
                .like("id", prefix + "%")
                .findList();

        Map<String, Object> result = new HashMap<>();
        for (ConfigEntity entity : entities) {
            result.put(entity.getId(), entity.getConfigValue());
        }
        return result;
    }

    @Override
    public void deleteAll(ConfigKey configKey) {
        if (configKey == null) {
            return;
        }
        String prefix = buildPrefix(configKey);
        List<ConfigEntity> entities = mysqlDb.find(ConfigEntity.class)
                .where()
                .like("id", prefix + "%")
                .findList();
        for (ConfigEntity entity : entities) {
            mysqlDb.delete(entity);
        }
    }

    @Override
    public void clear() {
        mysqlDb.find(ConfigEntity.class).delete();
    }

    /**
     * 构建 ID 前缀（用于批量查询）
     */
    private String buildPrefix(ConfigKey configKey) {
        return configKey.getScopeType() + ":" + configKey.getScopeId() + ":" + configKey.getTargetType() + ":" + configKey.getTargetId() + ":";
    }
}