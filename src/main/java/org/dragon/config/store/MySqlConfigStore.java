package org.dragon.config.store;

import io.ebean.Database;
import org.dragon.datasource.entity.ConfigEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MySqlConfigStore 配置存储 MySQL 实现
 *
 * <p>适配扁平化的 ConfigEntity 结构：
 * <ul>
 *   <li>id = {scopeBits}:{workspaceId}:{characterId}:{toolId}:{skillId}:{configKey}</li>
 *   <li>通过 scopeBits 位掩码标识激活的层级</li>
 * </ul>
 */
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlConfigStore implements ConfigStore {

    private final Database mysqlDb;

    public MySqlConfigStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void set(String configKey, Object value, int scopeBits,
                    String workspaceId, String characterId, String toolId, String skillId) {
        String id = ConfigEntity.buildId(scopeBits, workspaceId, characterId, toolId, skillId, configKey);
        ConfigEntity existing = mysqlDb.find(ConfigEntity.class, id);

        if (existing != null) {
            existing.setConfigValue(value);
            existing.setUpdatedAt(LocalDateTime.now());
            mysqlDb.update(existing);
        } else {
            ConfigEntity entity = ConfigEntity.builder()
                    .id(id)
                    .scopeBits(scopeBits)
                    .workspaceId(workspaceId)
                    .characterId(characterId)
                    .toolId(toolId)
                    .skillId(skillId)
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
    public Optional<Object> get(String configKey, int scopeBits,
                               String workspaceId, String characterId, String toolId, String skillId) {
        String id = ConfigEntity.buildId(scopeBits, workspaceId, characterId, toolId, skillId, configKey);
        ConfigEntity entity = mysqlDb.find(ConfigEntity.class, id);
        return Optional.ofNullable(entity != null ? entity.getConfigValue() : null);
    }

    @Override
    public void clear() {
        mysqlDb.find(ConfigEntity.class).delete();
    }

    @Override
    public List<ConfigStoreItem> listAll() {
        List<ConfigEntity> entities = mysqlDb.find(ConfigEntity.class).findList();
        List<ConfigStoreItem> items = new ArrayList<>();
        for (ConfigEntity entity : entities) {
            items.add(new ConfigStoreItem(
                    entity.getConfigKey(),
                    entity.getScopeBits(),
                    entity.getWorkspaceId(),
                    entity.getCharacterId(),
                    entity.getToolId(),
                    entity.getSkillId(),
                    entity.getConfigValue()
            ));
        }
        return items;
    }
}