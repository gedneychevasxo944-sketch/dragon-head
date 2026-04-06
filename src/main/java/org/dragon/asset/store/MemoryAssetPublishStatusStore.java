package org.dragon.asset.store;

import org.dragon.datasource.entity.AssetPublishStatusEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MemoryAssetPublishStatusStore 资产发布状态内存存储实现
 */
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryAssetPublishStatusStore implements AssetPublishStatusStore {

    private final Map<String, AssetPublishStatusEntity> store = new ConcurrentHashMap<>();

    @Override
    public void save(AssetPublishStatusEntity entity) {
        if (entity.getId() == null) {
            throw new IllegalArgumentException("AssetPublishStatusEntity id cannot be null");
        }
        store.put(entity.getId(), entity);
    }

    @Override
    public void update(AssetPublishStatusEntity entity) {
        if (entity.getId() == null || !store.containsKey(entity.getId())) {
            throw new IllegalArgumentException("AssetPublishStatusEntity not found: " + entity.getId());
        }
        store.put(entity.getId(), entity);
    }

    @Override
    public Optional<AssetPublishStatusEntity> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<AssetPublishStatusEntity> findByResource(String resourceType, String resourceId) {
        return store.values().stream()
                .filter(e -> resourceType.equals(e.getResourceType()) && resourceId.equals(e.getResourceId()))
                .findFirst();
    }

    @Override
    public List<AssetPublishStatusEntity> findByResourceTypeAndStatus(String resourceType, String status) {
        return store.values().stream()
                .filter(e -> resourceType.equals(e.getResourceType()) && status.equals(e.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetPublishStatusEntity> findByResourceType(String resourceType) {
        return store.values().stream()
                .filter(e -> resourceType.equals(e.getResourceType()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetPublishStatusEntity> findByStatus(String status) {
        return store.values().stream()
                .filter(e -> status.equals(e.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String resourceType, String resourceId) {
        return findByResource(resourceType, resourceId).isPresent();
    }

    @Override
    public void delete(String resourceType, String resourceId) {
        store.values().removeIf(e ->
                resourceType.equals(e.getResourceType()) && resourceId.equals(e.getResourceId()));
    }
}