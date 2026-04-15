package org.dragon.api.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TeamPositionResponse 团队席位响应DTO
 *
 * @author qieqie
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamPositionResponse {

    /**
     * 岗位 ID
     */
    private String id;

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
     * 分配的 Character ID
     */
    private String assignedCharacterId;

    /**
     * Built-in 类型（当分配的是 Built-in Character 时）
     */
    private String builtinType;

    /**
     * 是否为 Built-in 角色
     */
    private boolean builtin;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 岗位状态: vacant/filled/inactive
     */
    private String status;

    /**
     * 从 WorkspaceMember 转换（统一岗位和成员）
     *
     * @param member WorkspaceMember（作为岗位使用时）
     */
    public static TeamPositionResponse from(org.dragon.workspace.member.WorkspaceMember member) {
        String status;
        boolean isBuiltin = member.getHandlerType() == org.dragon.workspace.member.HandlerType.BUILTIN_CHARACTER;
        String assignedCharId = member.getHandlerId();

        if ((assignedCharId == null || assignedCharId.isEmpty()) && !isBuiltin) {
            status = "vacant";
        } else if (member.isEnabled()) {
            status = isBuiltin ? "filled_builtin" : "filled";
        } else {
            status = "inactive";
        }

        return TeamPositionResponse.builder()
                .id(member.getId())
                .roleName(member.getRoleName())
                .rolePackage(member.getRolePackage())
                .purpose(member.getPurpose())
                .scope(member.getScope())
                .assignedCharacterId(assignedCharId)
                .builtinType(isBuiltin ? assignedCharId : null)
                .builtin(isBuiltin)
                .enabled(member.isEnabled())
                .status(status)
                .build();
    }
}