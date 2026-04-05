package org.dragon.skill.domain;

import org.dragon.skill.enums.CreatorType;
import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.enums.PersistMode;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillEffort;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.SkillVisibility;
import org.dragon.skill.enums.StorageType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * skills 表领域对象（Service / Store 层流转）。
 *
 * <p>设计说明：
 * <ul>
 *   <li>每次更新技能时 INSERT 一条新记录，{@code skillId} 不变，{@code version} +1。</li>
 *   <li>{@code id} 为物理自增主键；{@code skillId + version} 联合唯一。</li>
*   <li>{@code aliases}、{@code allowedTools} 存为 JSON 字符串，
*       在 SkillDefinition 层完成反序列化。</li>
 * </ul>
 */
@Data
public class SkillDO {

    /** 物理自增主键 */
    private Long id;

    /** 技能业务标识（UUID），同一技能所有版本相同 */
    private String skillId;

    // ── 基本元信息 ──────────────────────────────────────────────────

    /** 技能调用名称，格式：字母/数字/中划线/下划线/中文，长度 2-100 */
    private String name;

    /** 展示名称（可含中文等） */
    private String displayName;

    /** 技能描述 */
    private String description;

    /** SKILL.md 正文内容（frontmatter 之后的部分） */
    private String content;

    /** 别名列表，JSON 数组字符串，如 ["sk","my-skill"] */
    private String aliases;

    /** 使用场景说明 */
    private String whenToUse;

    /** 参数提示 */
    private String argumentHint;

    /** 允许的工具列表，JSON 数组字符串 */
    private String allowedTools;

    /** 指定模型（覆盖默认模型），可为 null */
    private String model;

    /** 是否禁用模型自动调用：0=否，1=是 */
    private Integer disableModelInvocation;

    /** 用户是否可调用：0=否，1=是 */
    private Integer userInvocable;

    /** 执行上下文：inline / fork */
    private ExecutionContext executionContext;

    /** 努力程度 */
    private SkillEffort effort;

    // ── 分类与可见性 ─────────────────────────────────────────────────

    /** 技能分类 */
    private SkillCategory category;

    /** 可见性 */
    private SkillVisibility visibility;

    // ── 创建者与编辑者 ───────────────────────────────────────────────

    /** 创建者类型 */
    private CreatorType creatorType;

    /** 原始创建者用户 ID（所有版本填写首次创建者） */
    private Long creatorId;

    /** 原始创建者用户名 */
    private String creatorName;

    /** 本次版本编辑者用户 ID */
    private Long editorId;

    /** 本次版本编辑者用户名 */
    private String editorName;

    // ── 状态与版本 ───────────────────────────────────────────────────

    /** 状态 */
    private SkillStatus status;

    /** 版本号，从 1 开始，每次发布新版本 +1 */
    private Integer version;

    // ── 存储信息 ─────────────────────────────────────────────────────

    /** 存储类型 */
    private StorageType storageType;

    /** 存储信息，StorageInfoVO 的 JSON 序列化字符串 */
    private String storageInfo;

    // ── 时间戳 ───────────────────────────────────────────────────────

    /** 本条记录（版本）的创建时间 */
    private LocalDateTime createdAt;

    /** 发布时间（status 变为 active 时填写） */
    private LocalDateTime publishedAt;

    // ── 运行时行为扩展 ────────────────────────────────────────────────

    /**
     * 调用后是否持续留存在上下文：0=否（用完即走），1=是（每轮对话持续注入约束规则）。
     */
    private Integer persist;

    /** 留存模式（persist=1 时生效） */
    private PersistMode persistMode;

    // ── 标签 ──────────────────────────────────────────────────────────

    /** 标签列表，JSON 数组字符串，如 ["数据分析","API","工具类"] */
    private String tags;
}

