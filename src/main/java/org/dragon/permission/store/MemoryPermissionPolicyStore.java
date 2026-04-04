package org.dragon.permission.store;

import org.dragon.permission.entity.PermissionPolicyEntity;
import org.dragon.permission.enums.Role;
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
 * MemoryPermissionPolicyStore 权限策略内存存储实现
 */
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryPermissionPolicyStore implements PermissionPolicyStore {

    private final Map<Long, PermissionPolicyEntity> store = new ConcurrentHashMap<>();

    @Override
    public List<PermissionPolicyEntity> findByRoleAndResourceType(Role role, ResourceType resourceType) {
        return store.values().stream()
                .filter(p -> role == p.getRole())
                .filter(p -> {
                    ResourceType rt = p.getResourceType();
                    return rt == null || "*".equals(rt.name()) || rt == resourceType;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionPolicyEntity> findByRole(Role role) {
        return store.values().stream()
                .filter(p -> role == p.getRole())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PermissionPolicyEntity> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<PermissionPolicyEntity> findAll() {
        return List.copyOf(store.values());
    }
}
