package org.dragon.asset.tag.store;

import org.dragon.datasource.entity.AssetTagEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MemoryAssetTagStore 资产标签内存存储实现
 */
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryAssetTagStore implements AssetTagStore {

    private final Map<String, AssetTagEntity> store = new ConcurrentHashMap<>();

    private String key(String resourceType, String resourceId, String name) {
        return resourceType + ":" + resourceId + ":" + name;
    }

    @Override
    public void save(AssetTagEntity tag) {
        store.put(key(tag.getResourceType(), tag.getResourceId(), tag.getName()), tag);
    }

    @Override
    public void delete(String resourceType, String resourceId, String name) {
        store.remove(key(resourceType, resourceId, name));
    }

    @Override
    public List<AssetTagEntity> findByResource(String resourceType, String resourceId) {
        return store.values().stream()
                .filter(e -> resourceType.equals(e.getResourceType())
                        && resourceId.equals(e.getResourceId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetTagEntity> findByResources(String resourceType, List<String> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return List.of();
        }
        Set<String> idSet = resourceIds.stream().collect(Collectors.toSet());
        return store.values().stream()
                .filter(e -> resourceType.equals(e.getResourceType())
                        && idSet.contains(e.getResourceId()))
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> findTagNamesByResourceType(String resourceType) {
        return store.values().stream()
                .filter(e -> resourceType.equals(e.getResourceType()))
                .map(AssetTagEntity::getName)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean exists(String resourceType, String resourceId, String name) {
        return store.containsKey(key(resourceType, resourceId, name));
    }

    @Override
    public List<AssetTagEntity> findByTagNameAndResourceType(String tagName, String resourceType) {
        return store.values().stream()
                .filter(e -> tagName.equals(e.getName())
                        && resourceType.equals(e.getResourceType()))
                .collect(Collectors.toList());
    }
}
