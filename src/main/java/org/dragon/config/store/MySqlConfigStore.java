package org.dragon.config.store;

import io.ebean.Database;
import org.dragon.config.enums.ConfigLevel;
import org.dragon.datasource.entity.ConfigEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MySqlConfigStore 配置存储 MySQL 实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlConfigStore implements ConfigStore {

    private final Database mysqlDb;

    public MySqlConfigStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void set(ConfigLevel level, String workspaceId, String characterId,
                    String toolId, String skillId, String memoryId,
                    String configKey, Object value) {
        String id = ConfigEntity.buildId(level.getScopeBit(), workspaceId, characterId,
                toolId, skillId, memoryId, configKey);
        ConfigEntity existing = mysqlDb.find(ConfigEntity.class, id);

        if (existing != null) {
            existing.setConfigValue(value);
            existing.setUpdatedAt(LocalDateTime.now());
            mysqlDb.update(existing);
        } else {
            ConfigEntity entity = ConfigEntity.builder()
                    .id(id)
                    .scopeBit(level.getScopeBit())
                    .workspaceId(workspaceId)
                    .characterId(characterId)
                    .toolId(toolId)
                    .skillId(skillId)
                    .memoryId(memoryId)
                    .configKey(configKey)
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
    public Optional<Object> get(ConfigLevel level, String workspaceId, String characterId,
                                 String toolId, String skillId, String memoryId,
                                 String configKey) {
        String id = ConfigEntity.buildId(level.getScopeBit(), workspaceId, characterId,
                toolId, skillId, memoryId, configKey);
        ConfigEntity entity = mysqlDb.find(ConfigEntity.class, id);
        return Optional.ofNullable(entity != null ? entity.getConfigValue() : null);
    }

    @Override
    public Optional<Object> get(ConfigLevel level, String workspaceId, String configKey) {
        return get(level, workspaceId, null, null, null, null, configKey);
    }

    @Override
    public Optional<Object> get(ConfigLevel level, String workspaceId, String characterId, String configKey) {
        return get(level, workspaceId, characterId, null, null, null, configKey);
    }

    @Override
    public void delete(ConfigLevel level, String workspaceId, String characterId,
                       String toolId, String skillId, String memoryId, String configKey) {
        String id = ConfigEntity.buildId(level.getScopeBit(), workspaceId, characterId,
                toolId, skillId, memoryId, configKey);
        ConfigEntity entity = mysqlDb.find(ConfigEntity.class, id);
        if (entity != null) {
            mysqlDb.delete(entity);
        }
    }

    @Override
    public void clear() {
        mysqlDb.find(ConfigEntity.class).delete();
    }

    @Override
    public List<ConfigStoreItem> listAll() {
        List<ConfigEntity> entities = mysqlDb.find(ConfigEntity.class).findList();
        return convertToItems(entities);
    }

    @Override
    public List<ConfigStoreItem> listByLevel(ConfigLevel level) {
        List<ConfigEntity> entities = mysqlDb.find(ConfigEntity.class)
                .where()
                .eq("scope_bit", level.getScopeBit())
                .findList();
        return convertToItems(entities);
    }

    private List<ConfigStoreItem> convertToItems(List<ConfigEntity> entities) {
        List<ConfigStoreItem> items = new ArrayList<>();
        for (ConfigEntity entity : entities) {
            items.add(new ConfigStoreItem(
                    ConfigLevel.fromScopeBit(entity.getScopeBit()),
                    entity.getWorkspaceId(),
                    entity.getCharacterId(),
                    entity.getToolId(),
                    entity.getSkillId(),
                    entity.getMemoryId(),
                    entity.getConfigKey(),
                    entity.getConfigValue()
            ));
        }
        return items;
    }

    @Override
    public ConfigMetadata getMetadata(String configKey) {
        // GLOBAL level has scopeBit = 1
        String id = ConfigEntity.buildId(1, null, null, null, null, null, configKey);
        ConfigEntity entity = mysqlDb.find(ConfigEntity.class, id);
        if (entity != null) {
            return new ConfigMetadata(
                    entity.getName(),
                    entity.getDescription(),
                    entity.getValidationRules(),
                    entity.getOptions(),
                    entity.getValueType(),
                    entity.getConfigValue()
            );
        }
        return null;
    }
}