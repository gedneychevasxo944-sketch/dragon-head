package org.dragon.tools.store;

import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MemoryToolStore 工具内存存储实现
 */
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryToolStore implements ToolStore {

    private final Map<String, Map<String, Object>> store = new ConcurrentHashMap<>();

    @Override
    public void save(Map<String, Object> toolMetadata) {
        if (toolMetadata == null || toolMetadata.get("name") == null) {
            throw new IllegalArgumentException("ToolMetadata or tool name cannot be null");
        }
        store.put((String) toolMetadata.get("name"), toolMetadata);
    }

    @Override
    public void update(Map<String, Object> toolMetadata) {
        if (toolMetadata == null || toolMetadata.get("name") == null) {
            throw new IllegalArgumentException("ToolMetadata or tool name cannot be null");
        }
        if (!store.containsKey(toolMetadata.get("name"))) {
            throw new IllegalArgumentException("Tool not found: " + toolMetadata.get("name"));
        }
        store.put((String) toolMetadata.get("name"), toolMetadata);
    }

    @Override
    public void delete(String name) {
        store.remove(name);
    }

    @Override
    public Optional<Map<String, Object>> findByName(String name) {
        return Optional.ofNullable(store.get(name));
    }

    @Override
    public List<Map<String, Object>> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Map<String, Object>> findEnabled() {
        return store.values().stream()
                .filter(tool -> {
                    Object enabled = tool.get("enabled");
                    return enabled == null || Boolean.TRUE.equals(enabled);
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String name) {
        return store.containsKey(name);
    }

    @Override
    public int count() {
        return store.size();
    }
}