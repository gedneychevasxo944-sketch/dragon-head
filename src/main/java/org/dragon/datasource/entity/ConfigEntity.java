package org.dragon.datasource.entity;

import io.ebean.annotation.DbJson;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ConfigEntity 配置存储实体
 *
 * <p>一张 Config 表通过 scope_type 字段区分不同作用域，支持：
 * <ul>
 *   <li>GLOBAL：全局配置</li>
 *   <li>STUDIO：用户级配置</li>
 *   <li>WORKSPACE：工作空间配置</li>
 *   <li>CHARACTER/OBSERVER/SKILL/TOOL：资产配置</li>
 *   <li>MEMORY：记忆配置（隶属于 Character 或 Workspace）</li>
 *   <li>MEMBER：成员配置</li>
 *   <li>WORKSPACE_REF_OVERRIDE：Workspace 引用覆盖（最高优先级）</li>
 * </ul>
 *
 * <p>config_key 格式：{scopeType}:{scopeId}:{targetType}:{targetId}:{configKey}
 *
 * <p>示例：
 * <ul>
 *   <li>GLOBAL:-:self:-:jwt.secret</li>
 *   <li>WORKSPACE:ws_456:self:-:maxSteps</li>
 *   <li>WORKSPACE:ws_456:CHARACTER:char_123:maxSteps</li>
 *   <li>CHARACTER:char_789:self:-:systemPrompt</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "config_store", indexes = {
        @Index(name = "idx_scope", columnList = "scope_type, scope_id"),
        @Index(name = "idx_config_key", columnList = "config_key(64)"),
        @Index(name = "idx_reference", columnList = "reference_type, reference_id"),
        @Index(name = "idx_owner", columnList = "owner_type, owner_id"),
        @Index(name = "idx_status", columnList = "status")
})
public class ConfigEntity {

    @Id
    private String id;

    // ===== 作用域字段 =====

    /**
     * 作用域类型：GLOBAL, STUDIO, WORKSPACE, CHARACTER, MEMORY, OBSERVER, MEMBER, SKILL, TOOL, WORKSPACE_REF_OVERRIDE
     */
    @Column(name = "scope_type", nullable = false, length = 32)
    private String scopeType;

    /**
     * 作用域 ID（如 workspaceId、studioId 等）
     */
    @Column(name = "scope_id", length = 64)
    private String scopeId;

    /**
     * 目标类型：self（自有配置）或 CHARACTER/SKILL/OBSERVER/TOOL/MEMBER（引用配置）
     */
    @Column(name = "target_type", length = 32)
    private String targetType;

    /**
     * 目标 ID
     */
    @Column(name = "target_id", length = 64)
    private String targetId;

    /**
     * 配置键（不含 scope/target 前缀，只含最后的配置名）
     */
    @Column(name = "config_key", nullable = false, length = 255)
    private String configKey;

    /**
     * 配置值（JSON 格式存储）
     */
    @DbJson
    @Column(name = "config_value", columnDefinition = "JSON")
    private Object configValue;

    // ===== 引用关系字段 =====

    /**
     * 被引用资产类型：CHARACTER, SKILL, TOOL, OBSERVER 等
     * 用于 workspace_ref_override 或 skill_tool_usage 场景
     */
    @Column(name = "reference_type", length = 32)
    private String referenceType;

    /**
     * 被引用资产 ID
     */
    @Column(name = "reference_id", length = 64)
    private String referenceId;

    // ===== 归属关系字段 =====

    /**
     * 所属者类型：CHARACTER, WORKSPACE
     * 用于 Memory 等子资源的归属追踪
     */
    @Column(name = "owner_type", length = 32)
    private String ownerType;

    /**
     * 所属者 ID
     */
    @Column(name = "owner_id", length = 64)
    private String ownerId;

    // ===== 生命周期字段 =====

    /**
     * 状态：DRAFT（草稿）、PUBLISHED（已发布）
     */
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "PUBLISHED";

    /**
     * 版本号
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
     * 发布人
     */
    @Column(name = "published_by", length = 100)
    private String publishedBy;

    // ===== 审计字段 =====

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}