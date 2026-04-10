package org.dragon.material;

import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * MemoryMaterialStore 物料内存存储实现
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryMaterialStore implements MaterialStore {

    private final Map<String, Material> store = new ConcurrentHashMap<>();

    @Override
    public void save(Material material) {
        store.put(material.getId(), material);
        log.debug("[MemoryMaterialStore] Saved material: {}", material.getId());
    }

    @Override
    public void update(Material material) {
        if (store.containsKey(material.getId())) {
            store.put(material.getId(), material);
            log.debug("[MemoryMaterialStore] Updated material: {}", material.getId());
        }
    }

    @Override
    public void delete(String id) {
        store.remove(id);
        log.debug("[MemoryMaterialStore] Deleted material: {}", id);
    }

    @Override
    public Optional<Material> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Material> findByWorkspaceId(String workspaceId) {
        return store.values().stream()
                .filter(material -> workspaceId.equals(material.getWorkspaceId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Material> findByName(String workspaceId, String name) {
        return store.values().stream()
                .filter(material -> workspaceId.equals(material.getWorkspaceId())
                        && name.equals(material.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String id) {
        return store.containsKey(id);
    }
}
