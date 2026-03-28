package org.dragon.skill.entity;

import jakarta.persistence.*;
import lombok.*;
import org.dragon.skill.enums.BindType;

import java.time.LocalDateTime;

/**
 * Skill 统一绑定实体。
 * 记录 skill 与 workspace 或 character 的绑定关系，以及版本策略。
 *
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "skill_bind",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_skill_bind",
        columnNames = {"skill_id", "bind_type", "workspace_id", "character_id"}
    )
)
public class SkillBindEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的 Skill。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private SkillEntity skill;

    /**
     * 绑定类型。
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BindType bindType;

    /**
     * 归属的 workspace ID。
     * WORKSPACE 类型时必填；CHARACTER 类型时为 null；CHARACTER_WORKSPACE 时为 character 所属 workspace。
     */
    @Column
    private Long workspaceId;

    /**
     * 关联的 character ID。
     * CHARACTER 和 CHARACTER_WORKSPACE 类型时必填。
     */
    @Column(length = 128)
    private String characterId;

    /**
     * 圈选时锁定的版本号。
     * useLatest=true 时，此字段由系统自动维护为最新版本。
     * useLatest=false 时，此字段固定不变。
     */
    @Column(nullable = false)
    private Integer pinnedVersion;

    /**
     * 是否跟随最新版本。
     * true：每次 skill 发布新版本后，自动热更新到最新版本。
     * false：锁定 pinnedVersion 指定的版本，不自动更新。
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean useLatest = true;

    /**
     * 在当前绑定中是否启用此 skill。
     * 独立于 skill 表的全局 enabled 字段。
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}