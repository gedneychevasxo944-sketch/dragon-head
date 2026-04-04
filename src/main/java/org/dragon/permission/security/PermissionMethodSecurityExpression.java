package org.dragon.permission.security;

import org.dragon.permission.enums.Permission;
import org.dragon.permission.enums.ResourceType;
import org.dragon.permission.service.PermissionService;
import org.dragon.util.UserUtils;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import java.util.Set;

/**
 * PermissionMethodSecurityExpression SpEL 权限表达式操作类
 * 将 PermissionService 方法暴露给 @PreAuthorize SpEL 表达式
 */
public class PermissionMethodSecurityExpression
        implements MethodSecurityExpressionOperations {

    private PermissionService permissionService;
    private Authentication authentication;
    private Object filterObject;
    private Object returnObject;
    private Object target;

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    @Override
    public Authentication getAuthentication() {
        return authentication;
    }

    @Override
    public void setFilterObject(Object filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public Object getFilterObject() {
        return filterObject;
    }

    @Override
    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }

    @Override
    public Object getReturnObject() {
        return returnObject;
    }

    @Override
    public Object getThis() {
        return target;
    }

    @Override
    public boolean denyAll() {
        return false;
    }

    @Override
    public boolean permitAll() {
        return true;
    }

    @Override
    public boolean hasAuthority(String authority) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }

    @Override
    public boolean hasAnyAuthority(String... authorities) {
        for (String authority : authorities) {
            if (hasAuthority(authority)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasRole(String role) {
        return hasAuthority("ROLE_" + role);
    }

    @Override
    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAuthenticated() {
        return authentication != null && authentication.isAuthenticated();
    }

    @Override
    public boolean isAnonymous() {
        return isAuthenticated() && "anonymousUser".equals(authentication.getPrincipal());
    }

    @Override
    public boolean isRememberMe() {
        return false;
    }

    @Override
    public boolean isFullyAuthenticated() {
        return isAuthenticated() && !isAnonymous() && !isRememberMe();
    }

    public Object getPrincipal() {
        return authentication != null ? authentication.getPrincipal() : null;
    }

    @Override
    public boolean hasPermission(Object target, Object permission) {
        return false;
    }

    @Override
    public boolean hasPermission(Object targetId, String targetType, Object permission) {
        return false;
    }

    /**
     * 将资源ID转换为字符串
     */
    private String toResourceIdString(Object resourceId) {
        if (resourceId == null) {
            return null;
        }
        return String.valueOf(resourceId);
    }

    /**
     * 检查当前用户是否有权限查看指定资源
     */
    public boolean canView(Object resourceId, String resourceType) {
        return permissionService.canView(
                ResourceType.valueOf(resourceType),
                toResourceIdString(resourceId),
                getCurrentUserId()
        );
    }

    /**
     * 检查当前用户是否有权限编辑指定资源
     */
    public boolean canEdit(Object resourceId, String resourceType) {
        return permissionService.canEdit(
                ResourceType.valueOf(resourceType),
                toResourceIdString(resourceId),
                getCurrentUserId()
        );
    }

    /**
     * 检查当前用户是否有权限删除指定资源
     */
    public boolean canDelete(Object resourceId, String resourceType) {
        return permissionService.canDelete(
                ResourceType.valueOf(resourceType),
                toResourceIdString(resourceId),
                getCurrentUserId()
        );
    }

    /**
     * 检查当前用户是否有权限使用（调用/执行）指定资源
     */
    public boolean canUse(Object resourceId, String resourceType) {
        return permissionService.canUse(
                ResourceType.valueOf(resourceType),
                toResourceIdString(resourceId),
                getCurrentUserId()
        );
    }

    /**
     * 检查当前用户是否有权限管理指定资源的协作者
     */
    public boolean canManage(Object resourceId, String resourceType) {
        return permissionService.canManageMembers(
                ResourceType.valueOf(resourceType),
                toResourceIdString(resourceId),
                getCurrentUserId()
        );
    }

    /**
     * 检查当前用户是否是指定资源的所有者
     */
    public boolean isOwner(Object resourceId, String resourceType) {
        return permissionService.isOwner(
                ResourceType.valueOf(resourceType),
                toResourceIdString(resourceId),
                getCurrentUserId()
        );
    }

    /**
     * 通用权限检查
     */
    public boolean hasPermission(Object resourceId, String resourceType, String permission) {
        return permissionService.hasPermission(
                ResourceType.valueOf(resourceType),
                toResourceIdString(resourceId),
                getCurrentUserId(),
                Permission.valueOf(permission)
        );
    }

    /**
     * 获取当前用户在指定资源类型上的所有权限
     */
    public Set<Permission> getPermissions(Object resourceId, String resourceType) {
        return permissionService.getPermissions(
                ResourceType.valueOf(resourceType),
                toResourceIdString(resourceId),
                getCurrentUserId()
        );
    }

    private Long getCurrentUserId() {
        String userIdStr = UserUtils.getUserId();
        if (userIdStr == null || userIdStr.isEmpty()) {
            throw new IllegalStateException("User not authenticated");
        }
        return Long.parseLong(userIdStr);
    }
}