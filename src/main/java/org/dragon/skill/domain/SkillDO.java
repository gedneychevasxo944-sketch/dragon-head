package org.dragon.skill.domain;

import lombok.Data;
import org.dragon.skill.enums.CreatorType;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.SkillVisibility;

import java.time.LocalDateTime;

/**
 * SkillDO — 技能本体领域对象（不含版本内容）。
 *
 * <p>字段分类：
 * <ul>
 *   <li>标识：id (String)</li>
 *   <li>基本元信息：name, introduction, category, visibility, tags</li>
 *   <li>创建者：creatorType, creatorId, creatorName</li>
 *   <li>状态与版本指针：status, publishedVersionId</li>
 *   <li>时间戳：createdAt, deletedAt</li>
 * </ul>
 *
 * <p>版本内容（content / frontmatter / runtimeConfig）存储在 {@link SkillVersionDO} 中。
 */
@Data
public class SkillDO {

    // ── 标识 ─────────────────────────────────────────────────────

    /** 技能 ID（String 类型，对应表主键） */
    private String id;

    // ── 基本元信息 ──────────────────────────────────────────────────

    /** 技能名称（管理页面显示用） */
    private String name;

    /** 技能展示名称 */
    private String displayName;

    /** 技能简介 */
    private String introduction;

    /** 技能功能分类 */
    private SkillCategory category;

    /** 可见性 */
    private SkillVisibility visibility;

    /**
     * 是否为系统内置技能。
     *
     * <p>true 时技能由平台预置，对所有 agent 默认可用，无需手动绑定。
     * 与 {@link #category} 正交：内置技能同样可以拥有任意功能分类（如 CODER、DATA_ANALYSIS）。
     */
    private boolean builtin;

    /** 标签列表 */
    private String tags;

    // ── 创建者 ─────────────────────────────────────────────────────

    /** 创建者类型 */
    private CreatorType creatorType;

    /** 创建者用户 ID */
    private Long creatorId;

    /** 创建者用户名 */
    private String creatorName;

    // ── 状态与版本指针 ───────────────────────────────────────────────

    /** 当前状态 */
    private SkillStatus status;

    /** 已发布的版本 ID（指向 skill_version.id） */
    private Long publishedVersionId;

    // ── 时间戳 ─────────────────────────────────────────────────────

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 删除时间（软删除标记） */
    private LocalDateTime deletedAt;
}