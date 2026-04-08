package org.dragon.skill.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Skill 绑定详情视图对象（用于列表查询返回）。
 *
 * <p>聚合了绑定记录本身和 skill 基本信息，供前端展示绑定列表使用。
 */
@Data
public class SkillBindingVO {

    /** 绑定记录物理主键 */
    private Long bindingId;

    /** 绑定类型：character / workspace / character_workspace */
    private String bindingType;

    /** Character 主键（bindingType 含 character 时有值） */
    private String characterId;

    /** Workspace 主键（bindingType 含 workspace 时有值） */
    private String workspaceId;

    // ── Skill 信息（来自 skills 表） ──────────────────────────────────

    /** 技能唯一标识（UUID） */
    private String skillId;

    /** 技能调用名称 */
    private String skillName;

    /** 技能展示名称 */
    private String skillDisplayName;

    /** 技能描述 */
    private String skillDescription;

    /** 技能当前状态 */
    private String skillStatus;

    // ── 版本策略 ─────────────────────────────────────────────────────

    /** 版本类型：latest / fixed */
    private String versionType;

    /**
     * 版本号展示。
     * <ul>
     *   <li>versionType = 'latest'：返回当前最新 active 版本号</li>
     *   <li>versionType = 'fixed'：返回 fixedVersion</li>
     * </ul>
     */
    private Integer displayVersion;

    // ── 时间 ─────────────────────────────────────────────────────────

    /** 绑定创建时间 */
    private LocalDateTime createdAt;
}

