package org.dragon.datasource.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import org.dragon.workspace.member.TeamPosition;

/**
 * TeamPositionEntity 团队席位实体
 * 映射数据库 team_position 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "team_position")
public class TeamPositionEntity {

    @Id
    private String id;

    private String workspaceId;

    private String roleName;

    private String rolePackage;

    private String purpose;

    private String scope;

    private String assignedCharacterId;

    private boolean enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 转换为 TeamPosition
     */
    public TeamPosition toTeamPosition() {
        return TeamPosition.builder()
                .id(this.id)
                .workspaceId(this.workspaceId)
                .roleName(this.roleName)
                .rolePackage(this.rolePackage)
                .purpose(this.purpose)
                .scope(this.scope)
                .assignedCharacterId(this.assignedCharacterId)
                .enabled(this.enabled)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    /**
     * 从 TeamPosition 创建 Entity
     */
    public static TeamPositionEntity fromTeamPosition(TeamPosition position) {
        return TeamPositionEntity.builder()
                .id(position.getId())
                .workspaceId(position.getWorkspaceId())
                .roleName(position.getRoleName())
                .rolePackage(position.getRolePackage())
                .purpose(position.getPurpose())
                .scope(position.getScope())
                .assignedCharacterId(position.getAssignedCharacterId())
                .enabled(position.isEnabled())
                .createdAt(position.getCreatedAt())
                .updatedAt(position.getUpdatedAt())
                .build();
    }
}
