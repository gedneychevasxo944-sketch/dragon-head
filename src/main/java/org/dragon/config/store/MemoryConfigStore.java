package org.dragon.config.store;

import org.dragon.config.enums.ConfigLevel;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 内存配置存储实现
 *
 * <p>使用 ConcurrentHashMap 存储，适配扁平化 ConfigEntity 结构
 * <p>存储结构：compositeKey -> value
 */
@Slf4j
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryConfigStore implements ConfigStore {

    private final ConcurrentMap<String, Object> store = new ConcurrentHashMap<>();

    @Override
    public void set(ConfigLevel level, String workspaceId, String characterId,
                    String toolId, String skillId, String memoryId,
                    String configKey, Object value) {
        String compositeKey = buildCompositeKey(level, workspaceId, characterId,
                toolId, skillId, memoryId, configKey);
        store.put(compositeKey, value);
        log.debug("[MemoryConfigStore] Set: {} = {}", compositeKey, value);
    }

    @Override
    public Optional<Object> get(ConfigLevel level, String workspaceId, String characterId,
                                 String toolId, String skillId, String memoryId,
                                 String configKey) {
        String compositeKey = buildCompositeKey(level, workspaceId, characterId,
                toolId, skillId, memoryId, configKey);
        return Optional.ofNullable(store.get(compositeKey));
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
        String compositeKey = buildCompositeKey(level, workspaceId, characterId,
                toolId, skillId, memoryId, configKey);
        store.remove(compositeKey);
    }

    @Override
    public void clear() {
        store.clear();
        log.info("[MemoryConfigStore] All config cleared");
    }

    @Override
    public List<ConfigStoreItem> listAll() {
        List<ConfigStoreItem> items = new ArrayList<>();
        store.forEach((key, value) -> {
            ConfigStoreItem item = parseCompositeKey(key, value);
            if (item != null) {
                items.add(item);
            }
        });
        return items;
    }

    @Override
    public List<ConfigStoreItem> listByLevel(ConfigLevel level) {
        List<ConfigStoreItem> items = new ArrayList<>();
        store.forEach((key, value) -> {
            ConfigStoreItem item = parseCompositeKey(key, value);
            if (item != null && item.level() == level) {
                items.add(item);
            }
        });
        return items;
    }

    /**
     * 构建组合键
     * 格式：{scopeBit}:{workspaceId}:{characterId}:{toolId}:{skillId}:{memoryId}:{configKey}
     */
    private String buildCompositeKey(ConfigLevel level, String workspaceId, String characterId,
                                     String toolId, String skillId, String memoryId, String configKey) {
        return String.format("%d:%s:%s:%s:%s:%s:%s",
                level.getScopeBit(),
                workspaceId != null ? workspaceId : "",
                characterId != null ? characterId : "",
                toolId != null ? toolId : "",
                skillId != null ? skillId : "",
                memoryId != null ? memoryId : "",
                configKey);
    }

    /**
     * 解析组合键
     */
    private ConfigStoreItem parseCompositeKey(String key, Object value) {
        String[] parts = key.split(":", 7);
        if (parts.length < 7) {
            return null;
        }
        int scopeBit = Integer.parseInt(parts[0]);
        ConfigLevel level = ConfigLevel.fromScopeBit(scopeBit);
        if (level == null) {
            return null;
        }
        return new ConfigStoreItem(
                level,
                parts[1].isEmpty() ? null : parts[1],
                parts[2].isEmpty() ? null : parts[2],
                parts[3].isEmpty() ? null : parts[3],
                parts[4].isEmpty() ? null : parts[4],
                parts[5].isEmpty() ? null : parts[5],
                parts[6],
                value
        );
    }

    @Override
    public ConfigMetadata getMetadata(String configKey) {
        // MemoryStore 不存储元数据，返回 null
        return null;
    }

    @Override
    public List<ConfigMetadata> listMetadata() {
        // MemoryStore 不存储元数据，返回空列表
        return List.of();
    }
}