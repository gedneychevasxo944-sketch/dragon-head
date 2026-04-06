package org.dragon.skill.dto;

import org.dragon.skill.domain.StorageInfoVO;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.SkillVersionStatus;
import org.dragon.skill.enums.SkillVisibility;
import org.dragon.skill.enums.StorageType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Skill 详情视图对象。
 *
 * <p>用于详情页接口（{@code GET /api/skills/{skillId}}）及
 * 版本详情接口（{@code GET /api/skills/{skillId}/versions/{version}}）。
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillDetailVO {

    // ── 技能元信息 ───────────────────────────────────────────────

    /** 技能 ID */
    private String id;

    private String name;

    private String description;

    private SkillCategory category;

    private SkillVisibility visibility;

    /** 标签列表 */
    private java.util.List<String> tags;

    private SkillStatus status;

    // ── 版本信息 ─────────────────────────────────────────────────

    /** 版本号 */
    private Integer version;

    /** 版本状态 */
    private SkillVersionStatus versionStatus;

    /** 是否为最新版本 */
    private Boolean isLatest;

    /** SKILL.md 正文 */
    private String content;

    /** SKILL.md 原始 frontmatter */
    private String frontmatter;

    // ── 解析后的运行时配置 ────────────────────────────────────────

    private SkillRuntimeConfigVO runtimeConfig;

    // ── 存储信息 ─────────────────────────────────────────────────

    private StorageType storageType;

    private StorageInfoVO storageInfo;

    // ── 创建/编辑信息 ────────────────────────────────────────────

    private Long creatorId;
    private String creatorName;
    private LocalDateTime createdAt;

    private Long editorId;
    private String editorName;
    private LocalDateTime publishedAt;
}