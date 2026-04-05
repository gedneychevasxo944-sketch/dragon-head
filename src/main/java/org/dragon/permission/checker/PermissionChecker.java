package org.dragon.permission.checker;

import lombok.RequiredArgsConstructor;
import org.dragon.permission.enums.ResourceType;
import org.dragon.permission.service.PermissionService;
import org.dragon.util.UserUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * 权限检查器
 * 封装 PermissionService 的调用，提供简洁的权限检查方法
 */
@Component
@RequiredArgsConstructor
public class PermissionChecker {

    private final PermissionService permissionService;

    /**
     * 检查查看权限
     */
    public void checkView(String resourceType, String resourceId) {
        if (!permissionService.canView(ResourceType.valueOf(resourceType), resourceId, getCurrentUserId())) {
            throw new AccessDeniedException("No view permission for " + resourceType + ":" + resourceId);
        }
    }

    /**
     * 检查编辑权限
     */
    public void checkEdit(String resourceType, String resourceId) {
        if (!permissionService.canEdit(ResourceType.valueOf(resourceType), resourceId, getCurrentUserId())) {
            throw new AccessDeniedException("No edit permission for " + resourceType + ":" + resourceId);
        }
    }

    /**
     * 检查删除权限
     */
    public void checkDelete(String resourceType, String resourceId) {
        if (!permissionService.canDelete(ResourceType.valueOf(resourceType), resourceId, getCurrentUserId())) {
            throw new AccessDeniedException("No delete permission for " + resourceType + ":" + resourceId);
        }
    }

    /**
     * 检查使用权限
     */
    public void checkUse(String resourceType, String resourceId) {
        if (!permissionService.canUse(ResourceType.valueOf(resourceType), resourceId, getCurrentUserId())) {
            throw new AccessDeniedException("No use permission for " + resourceType + ":" + resourceId);
        }
    }

    /**
     * 检查管理权限
     */
    public void checkManage(String resourceType, String resourceId) {
        if (!permissionService.canManageMembers(ResourceType.valueOf(resourceType), resourceId, getCurrentUserId())) {
            throw new AccessDeniedException("No manage permission for " + resourceType + ":" + resourceId);
        }
    }

    /**
     * 检查指定权限
     */
    public void checkPermission(String resourceType, String resourceId, String permission) {
        if (!permissionService.hasPermission(ResourceType.valueOf(resourceType), resourceId, getCurrentUserId(),
                org.dragon.permission.enums.Permission.valueOf(permission))) {
            throw new AccessDeniedException("No " + permission + " permission for " + resourceType + ":" + resourceId);
        }
    }

    private Long getCurrentUserId() {
        String userId = UserUtils.getUserId();
        if (userId == null || userId.isEmpty()) {
            throw new IllegalStateException("User not authenticated");
        }
        return Long.parseLong(userId);
    }
}
