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
}
