package org.dragon.datasource.entity;

import io.ebean.annotation.DbJson;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AssetPublishStatusEntity 资产发布状态实体
 *
 * <p>独立于资产表，记录资产的草稿/发布状态
 *
 * <p>支持的资源类型：
 * <ul>
 *   <li>CHARACTER - Character 资产</li>
 *   <li>SKILL - Skill 资产</li>
 *   <li>OBSERVER - Observer 资产</li>
 *   <li>MODEL - Model 资产</li>
 *   <li>TEMPLATE - 模板资产</li>
 * </ul>
 *
 * <p>状态流转：
 * <pre>
 * DRAFT → PUBLISHED → ARCHIVED
 *         ↑____________↓
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "asset_publish_status", indexes = {
        @Index(name = "idx_aps_resource", columnList = "resource_type, resource_id", unique = true),
        @Index(name = "idx_aps_status", columnList = "status"),
        @Index(name = "idx_aps_published_by", columnList = "published_by")
})
public class AssetPublishStatusEntity {

    @Id
    private String id;

    /**
     * 资源类型：CHARACTER, SKILL, OBSERVER, MODEL, TEMPLATE
     */
    @Column(name = "resource_type", nullable = false, length = 32)
    private String resourceType;

    /**
     * 资源 ID
     */
    @Column(name = "resource_id", nullable = false, length = 64)
    private String resourceId;

    /**
     * 发布状态：DRAFT（草稿）, PUBLISHED（已发布）, ARCHIVED（已归档）
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";

    /**
     * 发布版本号（每次发布递增）
     */
    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;

    /**
     * 发布时间
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /**
     * 发布人 ID
     */
    @Column(name = "published_by", length = 100)
    private String publishedBy;

    /**
     * 归档时间
     */
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    /**
     * 归档人 ID
     */
    @Column(name = "archived_by", length = 100)
    private String archivedBy;

    /**
     * 发布时的快照（JSON），记录发布时的资产状态
     */
    @DbJson
    @Column(name = "snapshot", columnDefinition = "JSON")
    private Object snapshot;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}