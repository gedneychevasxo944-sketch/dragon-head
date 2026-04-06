package org.dragon.datasource.entity;

import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import org.dragon.skill.enums.BindingType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * workspace           | NULL       | 非 NULL     | Workspace 公共 skill 池
 * character_workspace | 非 NULL     | 非 NULL     | Character 在某 Workspace 的专属 skill
 * </pre>
 *
 * <p>绑定到 Skill 本身，具体使用哪个版本由 skill.publishedVersionId 决定。
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

    @Enumerated(EnumType.STRING)
    @Column(name = "binding_type", nullable = false, length = 30)
    private BindingType bindingType;

    // ── 绑定主体 ─────────────────────────────────────────────────────

    @Column(name = "character_id")
    private String characterId;

    @Column(name = "workspace_id")
    private String workspaceId;

    // ── 绑定的 Skill ─────────────────────────────────────────────────

    @Column(name = "skill_id", nullable = false, length = 64)
    private String skillId;

    // ── 时间戳 ──────────────────────────────────────────────────────

    @Column(name = "created_at")
    @WhenCreated
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @WhenModified
    private LocalDateTime updatedAt;

    // ── 转换方法 ─────────────────────────────────────────────────────

    public org.dragon.skill.domain.SkillBindingDO toDomain() {
        org.dragon.skill.domain.SkillBindingDO domain = new org.dragon.skill.domain.SkillBindingDO();
        domain.setId(this.id);
        domain.setBindingType(this.bindingType);
        domain.setCharacterId(this.characterId);
        domain.setWorkspaceId(this.workspaceId);
        domain.setSkillId(this.skillId);
        domain.setCreatedAt(this.createdAt);
        domain.setUpdatedAt(this.updatedAt);
        return domain;
    }

    public static SkillBindingEntity fromDomain(org.dragon.skill.domain.SkillBindingDO domain) {
        return SkillBindingEntity.builder()
                .id(domain.getId())
                .bindingType(domain.getBindingType())
                .characterId(domain.getCharacterId())
                .workspaceId(domain.getWorkspaceId())
                .skillId(domain.getSkillId())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
