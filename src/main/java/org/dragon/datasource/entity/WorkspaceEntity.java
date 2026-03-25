package org.dragon.datasource.entity;

import io.ebean.annotation.DbJson;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

import org.dragon.workspace.Workspace;
import org.dragon.workspace.WorkspacePersonality;
import java.util.Map;

/**
 * WorkspaceEntity 工作空间实体
 * 映射数据库 workspace 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "workspace")
public class WorkspaceEntity {

    @Id
    private String id;

    private String name;

    private String description;

    private String owner;

    private String status;

    @DbJson
    private Map<String, Object> properties;

    @DbJson
    private WorkspacePersonality personality;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 转换为Workspace
     */
    public Workspace toWorkspace() {
        return Workspace.builder()
                .id(this.id)
                .name(this.name)
                .description(this.description)
                .owner(this.owner)
                .status(Workspace.Status.valueOf(this.status))
                .properties(this.properties)
                .personality(this.personality)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    /**
     * 从Workspace创建Entity
     */
    public static WorkspaceEntity fromWorkspace(Workspace workspace) {
        return WorkspaceEntity.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .owner(workspace.getOwner())
                .status(workspace.getStatus() != null ? workspace.getStatus().name() : null)
                .properties(workspace.getProperties())
                .personality(workspace.getPersonality())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .build();
    }
}
