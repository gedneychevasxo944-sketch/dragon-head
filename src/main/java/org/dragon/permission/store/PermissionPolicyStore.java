package org.dragon.permission.store;

import org.dragon.datasource.entity.PermissionPolicyEntity;
import org.dragon.permission.enums.Role;
import org.dragon.store.Store;
import org.dragon.permission.enums.ResourceType;

import java.util.List;
import java.util.Optional;

/**
 * PermissionPolicyStore 权限策略存储接口
 */
public interface PermissionPolicyStore extends Store {

    /**
     * 根据角色和资源类型查找策略
     */
    List<PermissionPolicyEntity> findByRoleAndResourceType(Role role, ResourceType resourceType);

    /**
     * 根据角色和资源类型查找策略
     */
    List<PermissionPolicyEntity> findByRolesAndResourceType(List<Role> roleList, ResourceType resourceType);

    /**
     * 根据角色查找所有策略
     */
    List<PermissionPolicyEntity> findByRole(Role role);

    /**
     * 根据ID查找
     */
    Optional<PermissionPolicyEntity> findById(Long id);

    /**
     * 获取所有策略
     */
    List<PermissionPolicyEntity> findAll();
}
