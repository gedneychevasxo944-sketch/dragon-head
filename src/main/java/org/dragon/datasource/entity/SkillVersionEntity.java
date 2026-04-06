package org.dragon.datasource.entity;

import org.dragon.skill.enums.SkillVersionStatus;
import org.dragon.skill.enums.StorageType;
import io.ebean.annotation.WhenCreated;
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
 * SkillVersionEntity — 映射 skill_versions 表（技能版本）。
 *
 * <p>每次更新技能时 INSERT 新记录，skillId 不变，version +1。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "skill_versions")
public class SkillVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── 归属 ───────────────────────────────────────────────────────

    /** 所属技能 ID（UUID） */
    @Column(name = "skill_id", nullable = false, length = 64)
    private String skillId;

    /** 版本号（从 1 开始） */
    @Column(nullable = false)
    private Integer version;

    // ── 版本内容（从 frontmatter 解析）────────────────────────────────

    /** 版本名称（解析自 SKILL.md） */
    @Column(length = 100)
    private String name;

    /** 版本描述（解析自 SKILL.md） */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** SKILL.md 正文内容 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** SKILL.md 原始 frontmatter YAML */
    @Column(columnDefinition = "TEXT")
    private String frontmatter;

    /** 运行时配置 JSON */
    @Column(name = "runtime_config", columnDefinition = "JSON")
    private String runtimeConfig;

    // ── 编辑者 ───────────────────────────────────────────────────────

    /** 本次编辑者用户 ID */
    @Column(name = "editor_id")
    private Long editorId;

    /** 本次编辑者用户名 */
    @Column(name = "editor_name", length = 100)
    private String editorName;

    // ── 版本状态 ───────────────────────────────────────────────────

    /** 版本状态 */
    @Column(nullable = false, length = 10)
    private SkillVersionStatus status;

    /** 发版备注 */
    @Column(name = "release_note", columnDefinition = "TEXT")
    private String releaseNote;

    // ── 存储信息 ───────────────────────────────────────────────────

    /** 存储类型 */
    @Column(name = "storage_type", length = 10)
    private StorageType storageType;

    /** 存储详情，StorageInfoVO 的 JSON 序列化 */
    @Column(name = "storage_info", columnDefinition = "JSON")
    private String storageInfo;

    // ── 时间戳 ──────────────────────────────────────────────────────

    /** 版本创建时间 */
    @Column(name = "created_at")
    @WhenCreated
    private LocalDateTime createdAt;

    /** 发布时间 */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // ── 转换方法 ─────────────────────────────────────────────────────

    public org.dragon.skill.domain.SkillVersionDO toDomain() {
        org.dragon.skill.domain.SkillVersionDO domain = new org.dragon.skill.domain.SkillVersionDO();
        domain.setId(this.id);
        domain.setSkillId(this.skillId);
        domain.setVersion(this.version);
        domain.setName(this.name);
        domain.setDescription(this.description);
        domain.setContent(this.content);
        domain.setFrontmatter(this.frontmatter);
        domain.setRuntimeConfig(this.runtimeConfig);
        domain.setEditorId(this.editorId);
        domain.setEditorName(this.editorName);
        domain.setStatus(this.status);
        domain.setStorageType(this.storageType);
        domain.setStorageInfo(this.storageInfo);
        domain.setCreatedAt(this.createdAt);
        domain.setPublishedAt(this.publishedAt);
        return domain;
    }

    public static SkillVersionEntity fromDomain(org.dragon.skill.domain.SkillVersionDO domain) {
        return SkillVersionEntity.builder()
                .id(domain.getId())
                .skillId(domain.getSkillId())
                .version(domain.getVersion())
                .name(domain.getName())
                .description(domain.getDescription())
                .content(domain.getContent())
                .frontmatter(domain.getFrontmatter())
                .runtimeConfig(domain.getRuntimeConfig())
                .editorId(domain.getEditorId())
                .editorName(domain.getEditorName())
                .status(domain.getStatus())
                .storageType(domain.getStorageType())
                .storageInfo(domain.getStorageInfo())
                .createdAt(domain.getCreatedAt())
                .publishedAt(domain.getPublishedAt())
                .build();
    }
}
