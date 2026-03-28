package org.dragon.user.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 权限类型定义
 */
public class PermissionEnums {

    /**
     * Workspace 权限
     */
    public enum WorkspacePermission {
        VIEW,  // 查看
        USE,   // 使用
        EDIT,  // 编辑
        ADMIN, // 管理
        OWNER  // 所有者
    }

    /**
     * 资源类型
     */
    public enum ResourceType {
        WORKSPACE,
        CHARACTER,
        SKILL
    }

    /**
     * 权限验证结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionResult {
        private boolean allowed;
        private String message;
        private ResourceType resourceType;
        private String resourceId;
    }

    /**
     * 可见性类型
     */
    public enum Visibility {
        PUBLIC,   // 公开
        PRIVATE,  // 私有
        WORKSPACE // Workspace级别
    }
}
