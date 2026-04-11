package org.dragon.datasource.entity;

import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import org.dragon.skill.enums.BindingType;
import org.dragon.skill.enums.VersionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SkillBindingEntity — 映射 skill_bindings 表。
 *
 * <p>统一存储三种绑定关系，通过 {@code bindingType} 区分：
 * <pre>
 * bindingType         | characterId | workspaceId | 场景
 * --------------------+-------------+-------------+-------------------------------
 * character           | 非 NULL     | NULL        | Character 自有 skill
 * workspace           | NULL        | 非 NULL     | Workspace 公共 skill 池
 * character_workspace | 非 NULL     | 非 NULL     | Character 在某 Workspace 的专属 skill
 * </pre>
 *
 * <p>唯一约束：{@code (binding_type, character_id, workspace_id, skill_id)}，
 * 防止重复绑定（NULL 值参与联合唯一校验时需在 Service 层单独处理）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "skill_bindings")
public class SkillBindingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── 绑定类型 ─────────────────────────────────────────────────────

    /** 绑定类型（不可 null，存储小写字符串如 'workspace'） */
    @Column(name = "binding_type", nullable = false, length = 30)
    private String bindingType;

    // ── 绑定主体 ─────────────────────────────────────────────────────

    /**
     * Character 主键（来自 characters 表，BIGINT）。
     * bindingType = 'character' 或 'character_workspace' 时非 NULL。
     */
    @Column(name = "character_id")
    private String characterId;

    /**
     * Workspace 主键（来自 workspaces 表，BIGINT）。
     * bindingType = 'workspace' 或 'character_workspace' 时非 NULL。
     */
    @Column(name = "workspace_id")
    private String workspaceId;

    // ── 绑定的 Skill ─────────────────────────────────────────────────

    /** 技能业务 UUID，对应 skills.skill_id */
    @Column(name = "skill_id", nullable = false, length = 64)
    private String skillId;

    // ── 版本策略 ─────────────────────────────────────────────────────

    /** 版本策略（不可 null，存储小写字符串如 'latest'） */
    @Column(name = "version_type", nullable = false, length = 10)
    private String versionType;

    /**
     * 固定版本号（对应 skills.version）。
     * versionType = 'fixed' 时非 NULL；versionType = 'latest' 时为 NULL。
     */
    @Column(name = "fixed_version")
    private Integer fixedVersion;

    // ── 时间戳 ──────────────────────────────────────────────────────

    /** 绑定创建时间 */
    @Column(name = "created_at")
    @WhenCreated
    private LocalDateTime createdAt;

    /** 绑定最后更新时间（如修改版本策略） */
    @Column(name = "updated_at")
    @WhenModified
    private LocalDateTime updatedAt;

    // ── 转换方法 ─────────────────────────────────────────────────────

    /**
     * 转换为 SkillBindingDO（Service 层领域对象）。
     */
    public org.dragon.skill.domain.SkillBindingDO toDomain() {
        org.dragon.skill.domain.SkillBindingDO domain = new org.dragon.skill.domain.SkillBindingDO();
        domain.setId(this.id);
        domain.setBindingType(BindingType.fromValue(this.bindingType));
        domain.setCharacterId(this.characterId);
        domain.setWorkspaceId(this.workspaceId);
        domain.setSkillId(this.skillId);
        domain.setVersionType(VersionType.fromValue(this.versionType));
        domain.setFixedVersion(this.fixedVersion);
        domain.setCreatedAt(this.createdAt);
        domain.setUpdatedAt(this.updatedAt);
        return domain;
    }

    /**
     * 从 SkillBindingDO 创建 Entity。
     */
    public static SkillBindingEntity fromDomain(org.dragon.skill.domain.SkillBindingDO domain) {
        return SkillBindingEntity.builder()
                .id(domain.getId())
                .bindingType(domain.getBindingType() != null ? domain.getBindingType().getValue() : null)
                .characterId(domain.getCharacterId())
                .workspaceId(domain.getWorkspaceId())
                .skillId(domain.getSkillId())
                .versionType(domain.getVersionType() != null ? domain.getVersionType().getValue() : null)
                .fixedVersion(domain.getFixedVersion())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}