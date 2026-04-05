package org.dragon.permission.service;

import lombok.extern.slf4j.Slf4j;
import org.dragon.datasource.entity.AssetMemberEntity;
import org.dragon.datasource.entity.PermissionPolicyEntity;
import org.dragon.permission.enums.Permission;
import org.dragon.permission.enums.Role;
import org.dragon.permission.store.AssetMemberStore;
import org.dragon.permission.store.PermissionPolicyStore;
import org.dragon.store.StoreFactory;
import org.dragon.permission.enums.ResourceType;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PermissionService 统一权限服务
 * 提供所有资源权限的读取接口
 */
@Slf4j
@Service
public class PermissionService {

    private final AssetMemberStore assetMemberStore;
    private final PermissionPolicyStore permissionPolicyStore;
    private final ObjectMapper objectMapper;

    public PermissionService(StoreFactory storeFactory) {
        this.assetMemberStore = storeFactory.get(AssetMemberStore.class);
        this.permissionPolicyStore = storeFactory.get(PermissionPolicyStore.class);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取用户在某资产上的所有权限
     */
    public Set<Permission> getPermissions(ResourceType type, String resourceId, Long userId) {
        Optional<Role> role = getRole(type, resourceId, userId);
        if (role.isEmpty()) {
            return Collections.emptySet();
        }
        List<PermissionPolicyEntity> policies = permissionPolicyStore.findByRoleAndResourceType(role.get(), type);

        Set<Permission> permissions = new HashSet<>();
        for (PermissionPolicyEntity policy : policies) {
            permissions.addAll(parsePermissions(policy.getPermission()));
        }
        return permissions;
    }

    /**
     * 检查是否有某权限
     */
    public boolean hasPermission(ResourceType type, String resourceId, Long userId, Permission permission) {
        return getPermissions(type, resourceId, userId).contains(permission);
    }

    /**
     * 检查是否可以查看
     */
    public boolean canView(ResourceType type, String resourceId, Long userId) {
        return hasPermission(type, resourceId, userId, Permission.VIEW);
    }

    /**
     * 检查是否可以编辑
     */
    public boolean canEdit(ResourceType type, String resourceId, Long userId) {
        return hasPermission(type, resourceId, userId, Permission.EDIT);
    }

    /**
     * 检查是否可以删除
     */
    public boolean canDelete(ResourceType type, String resourceId, Long userId) {
        return hasPermission(type, resourceId, userId, Permission.DELETE);
    }

    /**
     * 检查是否可以使用（调用/执行）
     */
    public boolean canUse(ResourceType type, String resourceId, Long userId) {
        return hasPermission(type, resourceId, userId, Permission.USE);
    }

    /**
     * 检查是否可以管理成员
     */
    public boolean canManageMembers(ResourceType type, String resourceId, Long userId) {
        return hasPermission(type, resourceId, userId, Permission.MANAGE_COLLABORATOR);
    }

    /**
     * 获取用户在某资产的角色
     */
    public Optional<Role> getRole(ResourceType type, String resourceId, Long userId) {
        return assetMemberStore.findByResourceAndUser(type, resourceId, userId)
                .map(AssetMemberEntity::getRole);
    }

    /**
     * 是否为所有者
     */
    public boolean isOwner(ResourceType type, String resourceId, Long userId) {
        return getRole(type, resourceId, userId).map(r -> r == Role.OWNER).orElse(false);
    }

    /**
     * 获取用户可查看的所有资源ID列表
     */
    public List<String> getVisibleAssets(ResourceType type, Long userId) {
        return assetMemberStore.findByUserId(userId).stream()
                .filter(m -> m.getResourceType() == type && Boolean.TRUE.equals(m.getAccepted()))
                .map(AssetMemberEntity::getResourceId)
                .collect(Collectors.toList());
    }

    /**
     * 检查用户是否是资产成员（已接受邀请）
     */
    public boolean isMember(ResourceType type, String resourceId, Long userId) {
        return assetMemberStore.findByResourceAndUser(type, resourceId, userId)
                .map(m -> Boolean.TRUE.equals(m.getAccepted()))
                .orElse(false);
    }

    private Set<Permission> parsePermissions(String permissionJson) {
        if (permissionJson == null || permissionJson.isEmpty()) {
            return Collections.emptySet();
        }
        try {
            return objectMapper.readValue(permissionJson, new TypeReference<Set<Permission>>() {});
        } catch (Exception e) {
            log.error("[PermissionService] Failed to parse permission JSON: {}", permissionJson, e);
            return Collections.emptySet();
        }
    }
}
