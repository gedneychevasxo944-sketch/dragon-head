package org.dragon.skill.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Workspace 与 Skill 的关联实体。
 * 记录某个 workspace 圈选了哪些 skill，以及版本策略。
 *
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "workspace_skill",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_workspace_skill",
        columnNames = {"workspace_id", "skill_id"}
    )
)
public class WorkspaceSkillEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 归属的 workspace ID。
     */
    @Column(nullable = false)
    private Long workspaceId;

    /**
     * 关联的 Skill。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private SkillEntity skill;

    /**
     * 圈选时锁定的版本号。
     * use_latest=true 时，此字段由系统自动维护为最新版本。
     * use_latest=false 时，此字段固定不变。
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
     * 在当前 workspace 中是否启用此 skill。
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