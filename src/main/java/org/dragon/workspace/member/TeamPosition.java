package org.dragon.workspace.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TeamPosition 团队席位实体
 * 表示工作空间中的团队岗位/角色
 *
 * @author qieqie
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamPosition {

    /**
     * 创建复合 ID
     */
    public static String createId(String workspaceId, String positionName) {
        return workspaceId + "_" + positionName;
    }

    /**
     * 唯一标识 (workspaceId_positionName)
     */
    private String id;

    /**
     * Workspace ID
     */
    private String workspaceId;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色包/分类
     */
    private String rolePackage;

    /**
     * 岗位目的/职责
     */
    private String purpose;

    /**
     * 岗位范围
     */
    private String scope;

    /**
     * 分配的 Character ID（空表示空缺）
     */
    private String assignedCharacterId;

    /**
     * Built-in 类型（当分配的是 Built-in Character 时使用）
     * 格式: "builtin:{type}" 或 "builtin:{type}:{characterId}"
     * 例如:
     *   - "builtin:member_selector"  (workspace-scoped)
     *   - "builtin:member_selector:char_001"  (character-scoped)
     */
    private String assignedBuiltinType;

    /**
     * 是否启用
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 判断是否为 Built-in 角色
     */
    public boolean isBuiltin() {
        return assignedBuiltinType != null && !assignedBuiltinType.isBlank()
                && assignedBuiltinType.startsWith("builtin:");
    }

    /**
     * 获取 Built-in 类型
     * 解析 assignedBuiltinType 返回类型部分
     */
    public String getBuiltinType() {
        if (!isBuiltin()) {
            return null;
        }
        // 格式: "builtin:{type}" 或 "builtin:{type}:{characterId}"
        String typePart = assignedBuiltinType.substring("builtin:".length());
        int colonIndex = typePart.indexOf(':');
        return colonIndex > 0 ? typePart.substring(0, colonIndex) : typePart;
    }

    /**
     * 获取 Built-in 的 Character ID 部分（如果有）
     */
    public String getBuiltinCharacterId() {
        if (!isBuiltin()) {
            return null;
        }
        // 格式: "builtin:{type}:{characterId}"
        int colonIndex = assignedBuiltinType.indexOf(':', "builtin:".length());
        return colonIndex > 0 ? assignedBuiltinType.substring(colonIndex + 1) : null;
    }
}
