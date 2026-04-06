package org.dragon.config.store;

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
 * 使用 ConcurrentHashMap 存储，适配扁平化 ConfigEntity 结构
 *
 * <p>存储结构：使用组合键 "{scopeBits}:{workspaceId}:{characterId}:{toolId}:{skillId}:{configKey}"
 */
@Slf4j
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryConfigStore implements ConfigStore {

    /**
     * 存储结构: compositeKey -> value
     */
    private final ConcurrentMap<String, Object> store = new ConcurrentHashMap<>();

    @Override
    public void set(String configKey, Object value, int scopeBits,
                    String workspaceId, String characterId, String toolId, String skillId) {
        String compositeKey = buildFlatKey(scopeBits, workspaceId, characterId, toolId, skillId, configKey);
        store.put(compositeKey, value);
        log.debug("Config set: {} = {}", compositeKey, value);
    }

    @Override
    public Optional<Object> get(String configKey, int scopeBits,
                                String workspaceId, String characterId, String toolId, String skillId) {
        String compositeKey = buildFlatKey(scopeBits, workspaceId, characterId, toolId, skillId, configKey);
        return Optional.ofNullable(store.get(compositeKey));
    }

    @Override
    public void clear() {
        store.clear();
        log.info("All config cleared");
    }

    @Override
    public List<ConfigStoreItem> listAll() {
        List<ConfigStoreItem> items = new ArrayList<>();
        store.forEach((key, value) -> {
            String[] parts = key.split(":", 6);
            if (parts.length >= 6) {
                items.add(new ConfigStoreItem(
                        parts[5],
                        Integer.parseInt(parts[0]),
                        parts[1].isEmpty() ? null : parts[1],
                        parts[2].isEmpty() ? null : parts[2],
                        parts[3].isEmpty() ? null : parts[3],
                        parts[4].isEmpty() ? null : parts[4],
                        value
                ));
            }
        });
        return items;
    }

    /**
     * 构建扁平化组合键
     */
    private String buildFlatKey(int scopeBits, String workspaceId, String characterId,
                                String toolId, String skillId, String configKey) {
        return String.format("%d:%s:%s:%s:%s:%s",
                scopeBits,
                workspaceId != null ? workspaceId : "",
                characterId != null ? characterId : "",
                toolId != null ? toolId : "",
                skillId != null ? skillId : "",
                configKey);
    }
}