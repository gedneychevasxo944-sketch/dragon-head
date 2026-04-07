package org.dragon.asset.store;

import org.dragon.datasource.entity.AssetMemberEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.dragon.permission.enums.ResourceType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MemoryAssetMemberStore 资产成员内存存储实现
 */
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryAssetMemberStore implements AssetMemberStore {

    private final Map<Long, AssetMemberEntity> store = new ConcurrentHashMap<>();

    @Override
    public void save(AssetMemberEntity member) {
        if (member.getId() == null) {
            throw new IllegalArgumentException("AssetMember id cannot be null");
        }
        store.put(member.getId(), member);
    }

    @Override
    public void update(AssetMemberEntity member) {
        if (member.getId() == null || !store.containsKey(member.getId())) {
            throw new IllegalArgumentException("AssetMember not found: " + member.getId());
        }
        store.put(member.getId(), member);
    }

    @Override
    public void delete(Long id) {
        store.remove(id);
    }

    @Override
    public Optional<AssetMemberEntity> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<AssetMemberEntity> findByResource(ResourceType resourceType, String resourceId) {
        return store.values().stream()
                .filter(m -> resourceType == m.getResourceType() && resourceId.equals(m.getResourceId()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AssetMemberEntity> findByResourceAndUser(ResourceType resourceType, String resourceId, Long userId) {
        return store.values().stream()
                .filter(m -> resourceType == m.getResourceType()
                        && resourceId.equals(m.getResourceId())
                        && userId.equals(m.getUserId()))
                .findFirst();
    }

    @Override
    public List<AssetMemberEntity> findByUserId(Long userId) {
        return store.values().stream()
                .filter(m -> userId.equals(m.getUserId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetMemberEntity> findPendingInvitationsByUserId(Long userId) {
        return store.values().stream()
                .filter(m -> userId.equals(m.getUserId()) && !Boolean.TRUE.equals(m.getAccepted()))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByResource(ResourceType resourceType, String resourceId) {
        store.entrySet().removeIf(e ->
                resourceType == e.getValue().getResourceType()
                        && resourceId.equals(e.getValue().getResourceId()));
    }

    @Override
    public boolean exists(ResourceType resourceType, String resourceId, Long userId) {
        return findByResourceAndUser(resourceType, resourceId, userId).isPresent();
    }
}