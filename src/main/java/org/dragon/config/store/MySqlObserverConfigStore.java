package org.dragon.config.store;

import io.ebean.Database;
import org.dragon.config.enums.ConfigLevel;
import org.dragon.datasource.entity.ObserverConfigEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MySqlObserverConfigStore OBSERVER 配置存储 MySQL 实现
 */
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlObserverConfigStore implements ObserverConfigStore {

    private final Database mysqlDb;

    public MySqlObserverConfigStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void set(String observerId, ConfigLevel level, String workspaceId, String characterId,
                   String toolId, String skillId, String memoryId,
                   String configKey, Object value) {
        String id = ObserverConfigEntity.buildId(level.getScopeBit(), observerId, workspaceId,
                characterId, toolId, skillId, memoryId, configKey);
        ObserverConfigEntity existing = mysqlDb.find(ObserverConfigEntity.class, id);

        if (existing != null) {
            existing.setConfigValue(value);
            existing.setUpdatedAt(LocalDateTime.now());
            mysqlDb.update(existing);
        } else {
            ObserverConfigEntity entity = ObserverConfigEntity.builder()
                    .id(id)
                    .scopeBit(level.getScopeBit())
                    .observerId(observerId)
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
    public Optional<Object> get(String observerId, ConfigLevel level, String workspaceId, String characterId,
                               String toolId, String skillId, String memoryId,
                               String configKey) {
        String id = ObserverConfigEntity.buildId(level.getScopeBit(), observerId, workspaceId,
                characterId, toolId, skillId, memoryId, configKey);
        ObserverConfigEntity entity = mysqlDb.find(ObserverConfigEntity.class, id);
        return Optional.ofNullable(entity != null ? entity.getConfigValue() : null);
    }

    @Override
    public Optional<Object> get(String observerId, ConfigLevel level, String workspaceId, String configKey) {
        return get(observerId, level, workspaceId, null, null, null, null, configKey);
    }

    @Override
    public Optional<Object> get(String observerId, ConfigLevel level, String workspaceId, String characterId, String configKey) {
        return get(observerId, level, workspaceId, characterId, null, null, null, configKey);
    }

    @Override
    public void delete(String observerId, ConfigLevel level, String workspaceId, String characterId,
                      String toolId, String skillId, String memoryId, String configKey) {
        String id = ObserverConfigEntity.buildId(level.getScopeBit(), observerId, workspaceId,
                characterId, toolId, skillId, memoryId, configKey);
        ObserverConfigEntity entity = mysqlDb.find(ObserverConfigEntity.class, id);
        if (entity != null) {
            mysqlDb.delete(entity);
        }
    }

    @Override
    public void clear() {
        mysqlDb.find(ObserverConfigEntity.class).delete();
    }

    @Override
    public void clearByObserver(String observerId) {
        mysqlDb.find(ObserverConfigEntity.class)
                .where()
                .eq("observer_id", observerId)
                .delete();
    }

    @Override
    public List<ObserverConfigStoreItem> listAll() {
        List<ObserverConfigEntity> entities = mysqlDb.find(ObserverConfigEntity.class).findList();
        return convertToItems(entities);
    }

    @Override
    public List<ObserverConfigStoreItem> listByObserver(String observerId) {
        List<ObserverConfigEntity> entities = mysqlDb.find(ObserverConfigEntity.class)
                .where()
                .eq("observer_id", observerId)
                .findList();
        return convertToItems(entities);
    }

    @Override
    public List<ObserverConfigStoreItem> listByLevel(ConfigLevel level) {
        List<ObserverConfigEntity> entities = mysqlDb.find(ObserverConfigEntity.class)
                .where()
                .eq("scope_bit", level.getScopeBit())
                .findList();
        return convertToItems(entities);
    }

    private List<ObserverConfigStoreItem> convertToItems(List<ObserverConfigEntity> entities) {
        List<ObserverConfigStoreItem> items = new ArrayList<>();
        for (ObserverConfigEntity entity : entities) {
            items.add(new ObserverConfigStoreItem(
                    entity.getObserverId(),
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
}