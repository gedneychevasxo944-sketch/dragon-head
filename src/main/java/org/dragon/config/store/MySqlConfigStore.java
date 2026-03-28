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

import javax.annotation.Nullable;

/**
 * MySqlConfigStore 配置存储 MySQL 实现
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
        String id = buildId(configKey);
        ConfigEntity existing = mysqlDb.find(ConfigEntity.class, id);

        if (existing != null) {
            existing.setConfigValue(value);
            existing.setUpdatedAt(LocalDateTime.now());
            mysqlDb.update(existing);
        } else {
            ConfigEntity entity = ConfigEntity.builder()
                    .id(id)
                    .workspace(configKey.getWorkspace())
                    .entityType(configKey.getEntityType())
                    .entityId(configKey.getEntityId())
                    .configKey(configKey.getKey())
                    .configValue(value)
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
        ConfigEntity entity = mysqlDb.find(ConfigEntity.class, buildId(configKey));
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
        mysqlDb.delete(ConfigEntity.class, buildId(configKey));
    }

    @Override
    public boolean exists(ConfigKey configKey) {
        if (configKey == null || configKey.getKey() == null) {
            return false;
        }
        return mysqlDb.find(ConfigEntity.class, buildId(configKey)) != null;
    }

    @Override
    public Map<String, Object> getAll(ConfigKey configKey) {
        if (configKey == null) {
            return new HashMap<>();
        }

        String prefix = buildIdPrefix(configKey);
        List<ConfigEntity> entities = mysqlDb.find(ConfigEntity.class)
                .where()
                .like("id", prefix + "%")
                .findList();

        Map<String, Object> result = new HashMap<>();
        for (ConfigEntity entity : entities) {
            // 从 id 中提取最后一个 | 后面的部分作为 configKey
            String id = entity.getId();
            String key = id.substring(id.lastIndexOf("|") + 1);
            result.put(key, entity.getConfigValue());
        }
        return result;
    }

    @Override
    public void deleteAll(ConfigKey configKey) {
        if (configKey == null) {
            return;
        }
        String prefix = buildIdPrefix(configKey);
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
     * 构建唯一 ID
     * 格式: workspace|entityType|entityId|key
     * 空值用空字符串表示
     */
    private String buildId(ConfigKey configKey) {
        String workspace = nullToEmpty(configKey.getWorkspace());
        String entityType = nullToEmpty(configKey.getEntityType());
        String entityId = nullToEmpty(configKey.getEntityId());
        String key = nullToEmpty(configKey.getKey());
        return workspace + "|" + entityType + "|" + entityId + "|" + key;
    }

    /**
     * 构建 ID 前缀（用于批量查询）
     */
    private String buildIdPrefix(ConfigKey configKey) {
        String workspace = nullToEmpty(configKey.getWorkspace());
        String entityType = nullToEmpty(configKey.getEntityType());
        String entityId = nullToEmpty(configKey.getEntityId());
        return workspace + "|" + entityType + "|" + entityId + "|";
    }

    private String nullToEmpty(@Nullable String s) {
        return s != null ? s : "";
    }
}
