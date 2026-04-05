package org.dragon.skill.dto;

import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.enums.PersistMode;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillEffort;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.SkillVisibility;
import org.dragon.skill.enums.StorageType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Skill 详情视图对象（单条记录完整信息）。
 *
 * <p>用于详情页接口（{@code GET /api/skills/{skillId}}）及
 * 版本详情接口（{@code GET /api/skills/{skillId}/versions/{version}}）。
 *
 * <p>其中 {@code content} 字段（SKILL.md 正文）仅在明确需要时返回，
 * 列表页接口使用 {@link SkillSummaryVO} 避免大字段传输。
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillDetailVO {

    // ── 标识 ─────────────────────────────────────────────────────────

    /** 物理主键 */
    private Long id;

    /** 技能业务 UUID */
    private String skillId;

    /** 当前查询的版本号 */
    private Integer version;

    /** 是否为该 skillId 下的最新版本 */
    private Boolean isLatest;

    // ── 基本信息 ─────────────────────────────────────────────────────

    private String name;
    private String displayName;
    private String description;
    private String whenToUse;
    private String argumentHint;

    /** SKILL.md 正文（仅详情接口返回，列表接口不含此字段） */
    private String content;

    private List<String> aliases;
    private List<String> allowedTools;

    // ── 执行配置 ─────────────────────────────────────────────────────

    private String model;
    private Boolean disableModelInvocation;
    private Boolean userInvocable;
    private ExecutionContext executionContext;
    private SkillEffort effort;

    /** 是否持续留存上下文 */
    private Boolean persist;
    private PersistMode persistMode;

    // ── 标签 ────────────────────────────────────────────────────────

    /** 标签列表，用于技能分类/场景归纳 */
    private List<String> tags;

    // ── 分类与可见性 ─────────────────────────────────────────────────

    private SkillCategory category;
    private SkillVisibility visibility;
    private SkillStatus status;

    // ── 存储信息 ─────────────────────────────────────────────────────

    private StorageType storageType;

    // ── 创建者 / 编辑者 ──────────────────────────────────────────────

    private Long creatorId;
    private String creatorName;
    private Long editorId;
    private String editorName;

    // ── 时间戳 ───────────────────────────────────────────────────────

    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;

    // ── 使用统计（按需填充）──────────────────────────────────────────

    /** 总调用次数（来自 skill_usage_logs 聚合，可选） */
    private Long totalUsageCount;

    /** 最近一次调用时间（可选） */
    private LocalDateTime lastInvokedAt;
}

