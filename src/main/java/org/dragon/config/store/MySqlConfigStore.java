package org.dragon.config.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

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

        // Serialize value to JSON string
        String serializedValue;
        try {
            serializedValue = objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize config value", e);
        }

        if (existing != null) {
            existing.setConfigValue(serializedValue);
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
                    .configValue(serializedValue)
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
        if (entity == null || entity.getConfigValue() == null) {
            return Optional.empty();
        }
        try {
            // Deserialize JSON string back to Object
            Object value = objectMapper.readValue(entity.getConfigValue(), Object.class);
            return Optional.of(value);
        } catch (JsonProcessingException e) {
            // If deserialization fails, return the raw string value
            return Optional.of(entity.getConfigValue());
        }
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
            Object value = deserializeValue(entity.getConfigValue());
            items.add(new ConfigStoreItem(
                    ConfigLevel.fromScopeBit(entity.getScopeBit()),
                    entity.getWorkspaceId(),
                    entity.getCharacterId(),
                    entity.getToolId(),
                    entity.getSkillId(),
                    entity.getMemoryId(),
                    entity.getConfigKey(),
                    value
            ));
        }
        return items;
    }

    private Object deserializeValue(String jsonValue) {
        if (jsonValue == null) {
            return null;
        }
        try {
            return objectMapper.readValue(jsonValue, Object.class);
        } catch (JsonProcessingException e) {
            return jsonValue;
        }
    }

    @Override
    public ConfigMetadata getMetadata(String configKey) {
        // GLOBAL level has scopeBit = 1
        String id = ConfigEntity.buildId(1, null, null, null, null, null, configKey);
        ConfigEntity entity = mysqlDb.find(ConfigEntity.class, id);
        if (entity != null) {
            return new ConfigMetadata(
                    entity.getConfigKey(),
                    entity.getName(),
                    entity.getDescription(),
                    entity.getValidationRules(),
                    entity.getOptions(),
                    entity.getValueType(),
                    deserializeValue(entity.getConfigValue())
            );
        }
        return null;
    }

    @Override
    public List<ConfigMetadata> listMetadata() {
        // Query all ConfigEntity rows to get all configKeys from all levels
        List<ConfigEntity> allEntities = mysqlDb.find(ConfigEntity.class).findList();

        // For each distinct configKey, get the metadata from the first available entry
        java.util.Map<String, ConfigMetadata> metadataMap = new java.util.LinkedHashMap<>();

        for (ConfigEntity entity : allEntities) {
            if (!metadataMap.containsKey(entity.getConfigKey())) {
                metadataMap.put(entity.getConfigKey(), new ConfigMetadata(
                        entity.getConfigKey(),
                        entity.getName(),
                        entity.getDescription(),
                        entity.getValidationRules(),
                        entity.getOptions(),
                        entity.getValueType(),
                        deserializeValue(entity.getConfigValue())
                ));
            }
        }
        return new ArrayList<>(metadataMap.values());
    }
}