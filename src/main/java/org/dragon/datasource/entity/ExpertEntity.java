package org.dragon.datasource.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.permission.enums.ResourceType;

import java.time.LocalDateTime;

/**
 * ExpertEntity Expert 标记实体
 *
 * <p>用于标记某条资产记录是 Expert，并存储 Expert 特有的元信息。
 * expert_mark 表只做标记，不存储 name/description 等字段（这些直接在源资产表中）。
 *
 * @author yijunw
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "expert_mark")
public class ExpertEntity {

    /**
     * 主键 UUID
     */
    @Id
    private String id;

    /**
     * 资产类型：CHARACTER, SKILL, TRAIT, OBSERVER, WORKSPACE, MEMORY
     */
    @Column(name = "resource_type", nullable = false, length = 32)
    private ResourceType resourceType;

    /**
     * 资产 ID
     */
    @Column(name = "resource_id", nullable = false, length = 64)
    private String resourceId;

    /**
     * Expert 分类：助手/客服/创作/开发/研究/分析
     */
    @Column(name = "category", length = 64)
    private String category;

    /**
     * Expert 预览文本
     */
    @Column(name = "preview", columnDefinition = "TEXT")
    private String preview;

    /**
     * 目标用户群体
     */
    @Column(name = "target_audience", length = 128)
    private String targetAudience;

    /**
     * 被派生次数
     */
    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Integer usageCount = 0;

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

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}