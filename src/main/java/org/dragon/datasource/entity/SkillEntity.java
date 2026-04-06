package org.dragon.datasource.entity;

import org.dragon.skill.enums.CreatorType;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.SkillVisibility;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.WhenCreated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SkillEntity — 映射 skills 表（技能本体，元信息）。
 *
 * <p>与 SkillVersionEntity 的关系：
 * <ul>
 *   <li>一个 SkillEntity 对应多个 SkillVersionEntity（1:N）</li>
 *   <li>publishedVersionId 指向当前已发布的版本</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "skills")
public class SkillEntity {

    @Id
    @Column(length = 64)
    private String id;

    // ── 基本元信息 ───────────────────────────────────────────────────

    /** 技能名称（管理页面显示用） */
    @Column(nullable = false, length = 100)
    private String name;

    /** 技能描述 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 技能分类 */
    @Column(length = 50)
    private SkillCategory category;

    /** 可见性 */
    @Column(length = 20)
    private SkillVisibility visibility;

    /** 标签列表 */
    @Column(columnDefinition = "JSON")
    @DbJson
    private List<String> tags;

    // ── 创建者 ───────────────────────────────────────────────────────

    /** 创建者类型 */
    @Column(name = "creator_type", length = 20)
    private CreatorType creatorType;

    /** 创建者用户 ID */
    @Column(name = "creator_id")
    private Long creatorId;

    /** 创建者用户名 */
    @Column(name = "creator_name", length = 100)
    private String creatorName;

    // ── 状态和版本指针 ───────────────────────────────────────────────

    /** 当前状态 */
    @Column(nullable = false, length = 10)
    private SkillStatus status;

    /** 已发布的版本 ID（指向 skill_versions.id） */
    @Column(name = "published_version_id")
    private Long publishedVersionId;

    // ── 时间戳 ──────────────────────────────────────────────────────

    /** 创建时间 */
    @Column(name = "created_at")
    @WhenCreated
    private LocalDateTime createdAt;

    /** 删除时间（软删除标记） */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ── 转换方法 ─────────────────────────────────────────────────────

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    public org.dragon.skill.domain.SkillDO toDomain() {
        org.dragon.skill.domain.SkillDO domain = new org.dragon.skill.domain.SkillDO();
        domain.setId(this.id);
        domain.setName(this.name);
        domain.setDescription(this.description);
        domain.setCategory(this.category);
        domain.setVisibility(this.visibility);
        domain.setTags(toJsonString(this.tags));
        domain.setCreatorType(this.creatorType);
        domain.setCreatorId(this.creatorId);
        domain.setCreatorName(this.creatorName);
        domain.setStatus(this.status);
        domain.setPublishedVersionId(this.publishedVersionId);
        domain.setCreatedAt(this.createdAt);
        domain.setDeletedAt(this.deletedAt);
        return domain;
    }

    public static SkillEntity fromDomain(org.dragon.skill.domain.SkillDO domain) {
        return SkillEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .category(domain.getCategory())
                .visibility(domain.getVisibility())
                .tags(fromJsonString(domain.getTags()))
                .creatorType(domain.getCreatorType())
                .creatorId(domain.getCreatorId())
                .creatorName(domain.getCreatorName())
                .status(domain.getStatus())
                .publishedVersionId(domain.getPublishedVersionId())
                .createdAt(domain.getCreatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }

    private static String toJsonString(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> fromJsonString(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, List.class);
        } catch (Exception e) {
            return null;
        }
    }
}
