package org.dragon.datasource.entity;

import org.dragon.skill.enums.CreatorType;
import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.enums.PersistMode;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillEffort;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.SkillVisibility;
import org.dragon.skill.enums.StorageType;
import io.ebean.annotation.DbJson;
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
import java.util.List;

/**
 * SkillEntity — 映射 skills 表。
 *
 * <p>版本设计说明：
 * <ul>
 *   <li>{@code id} 为物理自增主键，全局唯一。</li>
 *   <li>{@code skillId} 为业务 UUID，同一技能所有版本共享同一值。</li>
 *   <li>每次更新技能时 INSERT 新记录，{@code skillId} 不变，{@code version} +1。</li>
 *   <li>查询最新版本：{@code WHERE skill_id=? ORDER BY version DESC LIMIT 1}。</li>
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── 业务标识 ─────────────────────────────────────────────────────

    /** 技能业务 UUID，同一技能所有版本相同 */
    @Column(name = "skill_id", nullable = false, length = 64)
    private String skillId;

    // ── 基本元信息 ───────────────────────────────────────────────────

    /** 技能调用名称（目录名），字母/数字/中划线/下划线/中文，长度 2-100 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 展示名称（frontmatter 中 name 字段，给用户看的名字） */
    @Column(name = "display_name", length = 200)
    private String displayName;

    /** 技能描述 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** SKILL.md 正文内容（frontmatter 之后的部分，即 prompt 模板） */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 别名列表，如 ["sk","my-skill"] */
    @Column(columnDefinition = "JSON")
    @DbJson
    private List<String> aliases;

    /** 使用场景说明（when_to_use），告诉模型何时自动调用此 skill */
    @Column(name = "when_to_use", columnDefinition = "TEXT")
    private String whenToUse;

    /** 参数提示（argument-hint），如 "[scope] <branch>" */
    @Column(name = "argument_hint", length = 200)
    private String argumentHint;

    /** 允许的工具列表，如 ["Bash(gh:*)", "Read", "Grep"] */
    @Column(name = "allowed_tools", columnDefinition = "JSON")
    @DbJson
    private List<String> allowedTools;

    /** 指定模型（覆盖默认模型），null 表示使用系统默认 */
    @Column(length = 100)
    private String model;

    /** 是否禁用模型自动调用（disable-model-invocation）：0=否，1=是 */
    @Column(name = "disable_model_invocation")
    private Integer disableModelInvocation;

    /** 用户是否可通过 /name 手动调用（user-invocable）：0=否，1=是 */
    @Column(name = "user_invocable")
    private Integer userInvocable;

    /** 执行上下文（context）：inline（默认）/ fork（独立 sub-agent） */
    @Column(name = "execution_context", length = 20)
    private ExecutionContext executionContext;

    /** 努力程度（effort） */
    @Column(length = 20)
    private SkillEffort effort;

    // ── 分类与可见性 ─────────────────────────────────────────────────

    /** 技能分类 */
    @Column(length = 50)
    private SkillCategory category;

    /** 可见性 */
    @Column(length = 20)
    private SkillVisibility visibility;

    // ── 创建者与编辑者 ───────────────────────────────────────────────

    /** 创建者类型 */
    @Column(name = "creator_type", length = 20)
    private CreatorType creatorType;

    /** 原始创建者用户 ID（所有版本固定填写首次创建者） */
    @Column(name = "creator_id")
    private Long creatorId;

    /** 原始创建者用户名 */
    @Column(name = "creator_name", length = 100)
    private String creatorName;

    /** 本次版本编辑者用户 ID */
    @Column(name = "editor_id")
    private Long editorId;

    /** 本次版本编辑者用户名 */
    @Column(name = "editor_name", length = 100)
    private String editorName;

    // ── 状态与版本 ───────────────────────────────────────────────────

    /** 状态 */
    @Column(nullable = false, length = 20)
    private SkillStatus status;

    /** 版本号，从 1 开始，每次 INSERT 新版本 +1 */
    @Column(nullable = false)
    private Integer version;

    // ── 存储信息 ─────────────────────────────────────────────────────

    /** 存储类型 */
    @Column(name = "storage_type", length = 20)
    private StorageType storageType;

    /** 存储详情，StorageInfoVO 的 JSON 序列化 */
    @Column(name = "storage_info", columnDefinition = "JSON")
    private String storageInfo;

    // ── 时间戳 ──────────────────────────────────────────────────────

    /** 本条版本记录的创建时间 */
    @Column(name = "created_at")
    @WhenCreated
    private LocalDateTime createdAt;

    /** 发布时间（status 变为 active 时填写） */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // ── 运行时行为扩展 ────────────────────────────────────────────────

    /**
     * 调用后是否持续留存在上下文：0=否（用完即走），1=是（每轮持续注入约束规则）。
     */
    @Column
    private Integer persist;

    /** 留存模式（persist=1 时生效） */
    @Column(name = "persist_mode", length = 20)
    private PersistMode persistMode;

    // ── 转换方法 ─────────────────────────────────────────────────────

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * 转换为 SkillDO（Service 层使用的领域对象）。
     * aliases / allowedTools 在 SkillDO 中存为 JSON 字符串，与 Entity 的 List 互转。
     */
    public org.dragon.skill.domain.SkillDO toDomain() {
        org.dragon.skill.domain.SkillDO domain = new org.dragon.skill.domain.SkillDO();
        domain.setId(this.id);
        domain.setSkillId(this.skillId);
        domain.setName(this.name);
        domain.setDisplayName(this.displayName);
        domain.setDescription(this.description);
        domain.setContent(this.content);
        domain.setAliases(toJsonString(this.aliases));
        domain.setWhenToUse(this.whenToUse);
        domain.setArgumentHint(this.argumentHint);
        domain.setAllowedTools(toJsonString(this.allowedTools));
        domain.setModel(this.model);
        domain.setDisableModelInvocation(this.disableModelInvocation);
        domain.setUserInvocable(this.userInvocable);
        domain.setExecutionContext(this.executionContext);
        domain.setEffort(this.effort);
        domain.setCategory(this.category);
        domain.setVisibility(this.visibility);
        domain.setCreatorType(this.creatorType);
        domain.setCreatorId(this.creatorId);
        domain.setCreatorName(this.creatorName);
        domain.setEditorId(this.editorId);
        domain.setEditorName(this.editorName);
        domain.setStatus(this.status);
        domain.setVersion(this.version);
        domain.setStorageType(this.storageType);
        domain.setStorageInfo(this.storageInfo);
        domain.setCreatedAt(this.createdAt);
        domain.setPublishedAt(this.publishedAt);
        domain.setPersist(this.persist);
        domain.setPersistMode(this.persistMode);
        return domain;
    }

    /**
     * 从 SkillDO 创建 Entity。
     */
    public static SkillEntity fromDomain(org.dragon.skill.domain.SkillDO domain) {
        return SkillEntity.builder()
                .id(domain.getId())
                .skillId(domain.getSkillId())
                .name(domain.getName())
                .displayName(domain.getDisplayName())
                .description(domain.getDescription())
                .content(domain.getContent())
                .aliases(fromJsonString(domain.getAliases()))
                .whenToUse(domain.getWhenToUse())
                .argumentHint(domain.getArgumentHint())
                .allowedTools(fromJsonString(domain.getAllowedTools()))
                .model(domain.getModel())
                .disableModelInvocation(domain.getDisableModelInvocation())
                .userInvocable(domain.getUserInvocable())
                .executionContext(domain.getExecutionContext())
                .effort(domain.getEffort())
                .category(domain.getCategory())
                .visibility(domain.getVisibility())
                .creatorType(domain.getCreatorType())
                .creatorId(domain.getCreatorId())
                .creatorName(domain.getCreatorName())
                .editorId(domain.getEditorId())
                .editorName(domain.getEditorName())
                .status(domain.getStatus())
                .version(domain.getVersion())
                .storageType(domain.getStorageType())
                .storageInfo(domain.getStorageInfo())
                .createdAt(domain.getCreatedAt())
                .publishedAt(domain.getPublishedAt())
                .persist(domain.getPersist())
                .persistMode(domain.getPersistMode())
                .build();
    }

    // ── 私有工具 ─────────────────────────────────────────────────────

    /** List<String> → JSON 字符串，null/空列表返回 null */
    private static String toJsonString(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }

    /** JSON 字符串 → List<String>，null/空串返回 null */
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