package org.dragon.datasource.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import org.dragon.commonsense.CommonSenseFolder;

/**
 * CommonSenseFolderEntity 常识文件夹实体
 * 映射数据库 common_sense_folder 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "common_sense_folder")
public class CommonSenseFolderEntity {

    @Id
    private String id;

    private String workspaceId;

    private String parentId;

    private String name;

    private String description;

    private int sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 转换为CommonSenseFolder
     */
    public CommonSenseFolder toCommonSenseFolder() {
        return CommonSenseFolder.builder()
                .id(this.id)
                .workspaceId(this.workspaceId)
                .parentId(this.parentId)
                .name(this.name)
                .description(this.description)
                .sortOrder(this.sortOrder)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    /**
     * 从CommonSenseFolder创建Entity
     */
    public static CommonSenseFolderEntity fromCommonSenseFolder(CommonSenseFolder folder) {
        return CommonSenseFolderEntity.builder()
                .id(folder.getId())
                .workspaceId(folder.getWorkspaceId())
                .parentId(folder.getParentId())
                .name(folder.getName())
                .description(folder.getDescription())
                .sortOrder(folder.getSortOrder())
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }
}
