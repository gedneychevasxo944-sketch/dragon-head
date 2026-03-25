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
import java.util.Map;

import org.dragon.workspace.material.Material;
import java.util.Map;

/**
 * MaterialEntity 物料实体
 * 映射数据库 material 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "material")
public class MaterialEntity {

    @Id
    private String id;

    private String workspaceId;

    private String name;

    private long size;

    private String type;

    private String storageKey;

    private String uploader;

    private LocalDateTime uploadedAt;

    @DbJson
    private Map<String, Object> metadata;

    private String kind;

    private String parseStatus;

    private String parsedContentId;

    private String sourceChannel;

    private String sourceMessageId;

    /**
     * 转换为Material
     */
    public Material toMaterial() {
        return Material.builder()
                .id(this.id)
                .workspaceId(this.workspaceId)
                .name(this.name)
                .size(this.size)
                .type(this.type)
                .storageKey(this.storageKey)
                .uploader(this.uploader)
                .uploadedAt(this.uploadedAt)
                .metadata(this.metadata)
                .kind(this.kind)
                .parseStatus(this.parseStatus)
                .parsedContentId(this.parsedContentId)
                .sourceChannel(this.sourceChannel)
                .sourceMessageId(this.sourceMessageId)
                .build();
    }

    /**
     * 从Material创建Entity
     */
    public static MaterialEntity fromMaterial(Material material) {
        return MaterialEntity.builder()
                .id(material.getId())
                .workspaceId(material.getWorkspaceId())
                .name(material.getName())
                .size(material.getSize())
                .type(material.getType())
                .storageKey(material.getStorageKey())
                .uploader(material.getUploader())
                .uploadedAt(material.getUploadedAt())
                .metadata(material.getMetadata())
                .kind(material.getKind())
                .parseStatus(material.getParseStatus())
                .parsedContentId(material.getParsedContentId())
                .sourceChannel(material.getSourceChannel())
                .sourceMessageId(material.getSourceMessageId())
                .build();
    }
}
