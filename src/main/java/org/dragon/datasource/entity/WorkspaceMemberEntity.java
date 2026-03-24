package org.dragon.datasource.entity;

import io.ebean.annotation.DbJson;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.dragon.workspace.member.WorkspaceMember;
import java.util.List;
import java.util.Map;

/**
 * WorkspaceMemberEntity 工作空间成员实体
 * 映射数据库 workspace_member 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "workspace_member")
public class WorkspaceMemberEntity {

    @Id
    private String id;

    private String workspaceId;

    private String characterId;

    private String role;

    private String layer;

    @DbJson
    private List<String> tags;

    private double weight;

    private int priority;

    private int reputation;

    @DbJson
    private WorkspaceMember.ResourceQuota resourceQuota;

    private LocalDateTime joinAt;

    private LocalDateTime lastActiveAt;

    @DbJson
    private Map<String, Object> metadata;

    /**
     * 转换为WorkspaceMember
     */
    public WorkspaceMember toWorkspaceMember() {
        return WorkspaceMember.builder()
                .id(this.id)
                .workspaceId(this.workspaceId)
                .characterId(this.characterId)
                .role(this.role)
                .layer(this.layer != null ? WorkspaceMember.Layer.valueOf(this.layer) : null)
                .tags(this.tags)
                .weight(this.weight)
                .priority(this.priority)
                .reputation(this.reputation)
                .resourceQuota(this.resourceQuota)
                .joinAt(this.joinAt)
                .lastActiveAt(this.lastActiveAt)
                .metadata(this.metadata)
                .build();
    }

    /**
     * 从WorkspaceMember创建Entity
     */
    public static WorkspaceMemberEntity fromWorkspaceMember(WorkspaceMember member) {
        return WorkspaceMemberEntity.builder()
                .id(member.getId())
                .workspaceId(member.getWorkspaceId())
                .characterId(member.getCharacterId())
                .role(member.getRole())
                .layer(member.getLayer() != null ? member.getLayer().name() : null)
                .tags(member.getTags())
                .weight(member.getWeight())
                .priority(member.getPriority())
                .reputation(member.getReputation())
                .resourceQuota(member.getResourceQuota())
                .joinAt(member.getJoinAt())
                .lastActiveAt(member.getLastActiveAt())
                .metadata(member.getMetadata())
                .build();
    }
}
