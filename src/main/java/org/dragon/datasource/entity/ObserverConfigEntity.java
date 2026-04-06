package org.dragon.datasource.entity;

import io.ebean.annotation.DbJson;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.config.enums.ConfigLevel;

import java.time.LocalDateTime;

/**
 * OBSERVER 配置实体
 *
 * <p>对应 config_store_observer 表，OBSERVER 体系的配置存储。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "config_store_observer", indexes = {
        @Index(name = "idx_scope_bit", columnList = "scope_bit"),
        @Index(name = "idx_observer", columnList = "observer_id"),
        @Index(name = "idx_config_key", columnList = "config_key")
})
public class ObserverConfigEntity {

    @Id
    @Column(name = "id", length = 255)
    private String id;

    /**
     * 粒度标识（131-205，OBSERVER 粒度）
     */
    @Column(name = "scope_bit", nullable = false)
    private Integer scopeBit;

    /**
     * OBSERVER ID
     */
    @Column(name = "observer_id", length = 64, nullable = false)
    private String observerId;

    // ==================== 扁平化层级 ID 存储 ====================

    @Column(name = "workspace_id", length = 64)
    private String workspaceId;

    @Column(name = "character_id", length = 64)
    private String characterId;

    @Column(name = "tool_id", length = 64)
    private String toolId;

    @Column(name = "skill_id", length = 64)
    private String skillId;

    @Column(name = "memory_id", length = 64)
    private String memoryId;

    // ==================== 配置值 ====================

    @Column(name = "config_key", nullable = false, length = 255)
    private String configKey;

    @DbJson
    @Column(name = "config_value", columnDefinition = "JSON")
    private Object configValue;

    @Column(name = "value_type", length = 32)
    private String valueType;

    // ==================== 状态和版本 ====================

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "PUBLISHED";

    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "published_by", length = 100)
    private String publishedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ==================== 辅助方法 ====================

    /**
     * 获取配置粒度
     */
    public ConfigLevel getLevel() {
        return ConfigLevel.fromScopeBit(scopeBit);
    }

    /**
     * 构建数据库主键
     */
    public static String buildId(Integer scopeBit, String observerId, String workspaceId,
                                 String characterId, String toolId, String skillId,
                                 String memoryId, String configKey) {
        return String.format("%d:%s:%s:%s:%s:%s:%s:%s",
                scopeBit,
                observerId,
                workspaceId != null ? workspaceId : "",
                characterId != null ? characterId : "",
                toolId != null ? toolId : "",
                skillId != null ? skillId : "",
                memoryId != null ? memoryId : "",
                configKey);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (id == null) {
            id = buildId(scopeBit, observerId, workspaceId, characterId, toolId, skillId, memoryId, configKey);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}