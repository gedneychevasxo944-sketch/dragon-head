package org.dragon.asset.store;

import org.dragon.asset.enums.AssociationType;
import org.dragon.datasource.entity.AssetAssociationEntity;
import org.dragon.permission.enums.ResourceType;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MemoryAssetAssociationStore 资产关联内存存储实现
 */
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryAssetAssociationStore implements AssetAssociationStore {

    private final Map<Long, AssetAssociationEntity> store = new ConcurrentHashMap<>();
    private Long idGenerator = 1L;

    @Override
    public void save(AssetAssociationEntity association) {
        if (association.getId() == null) {
            association.setId(idGenerator++);
        }
        store.put(association.getId(), association);
    }

    @Override
    public void update(AssetAssociationEntity association) {
        if (association.getId() == null || !store.containsKey(association.getId())) {
            throw new IllegalArgumentException("AssetAssociation not found: " + association.getId());
        }
        store.put(association.getId(), association);
    }

    @Override
    public void delete(Long id) {
        store.remove(id);
    }

    @Override
    public Optional<AssetAssociationEntity> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<AssetAssociationEntity> findBySource(AssociationType type, ResourceType sourceType, String sourceId) {
        return store.values().stream()
                .filter(a -> type == a.getAssociationType()
                        && sourceType == a.getSourceType()
                        && sourceId.equals(a.getSourceId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetAssociationEntity> findByTarget(AssociationType type, ResourceType targetType, String targetId) {
        return store.values().stream()
                .filter(a -> type == a.getAssociationType()
                        && targetType == a.getTargetType()
                        && targetId.equals(a.getTargetId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetAssociationEntity> findByType(AssociationType type) {
        return store.values().stream()
                .filter(a -> type == a.getAssociationType())
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(AssociationType type, ResourceType sourceType, String sourceId,
                          ResourceType targetType, String targetId) {
        return store.values().stream()
                .anyMatch(a -> type == a.getAssociationType()
                        && sourceType == a.getSourceType()
                        && sourceId.equals(a.getSourceId())
                        && targetType == a.getTargetType()
                        && targetId.equals(a.getTargetId()));
    }

    @Override
    public void deleteBySourceAndTarget(AssociationType type, ResourceType sourceType, String sourceId,
                                        ResourceType targetType, String targetId) {
        store.entrySet().removeIf(e -> {
            AssetAssociationEntity a = e.getValue();
            return type == a.getAssociationType()
                    && sourceType == a.getSourceType()
                    && sourceId.equals(a.getSourceId())
                    && targetType == a.getTargetType()
                    && targetId.equals(a.getTargetId());
        });
    }
}
