package org.dragon.skill.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.skill.enums.ExecutionContext;

import java.time.LocalDateTime;

/**
 * SkillUsageDO — Skill 调用记录领域对象。
 *
 * <p>对应 {@code skill_usage_log} 表，由 {@link org.dragon.datasource.entity.SkillUsageEntity} 转换而来。
 * 用于 {@link org.dragon.skill.store.SkillUsageStore} 和
 * {@link org.dragon.skill.service.SkillUsageService} 之间的数据传递。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillUsageDO {

    /** 物理自增主键（写入时为 null，回填后非 null） */
    private Long id;

    // ── Skill 信息 ───────────────────────────────────────────────────

    /** 技能业务 UUID */
    private String skillId;

    /** 技能名称（冗余，便于聚合排序） */
    private String skillName;

    /** 本次执行命中的版本号 */
    private Integer skillVersion;

    // ── 调用来源 ─────────────────────────────────────────────────────

    /** 执行者 Character ID */
    private String characterId;

    /** 所在 Workspace ID（独立模式为 null） */
    private String workspaceId;

    /** Agent 实例 ID（UUID） */
    private String agentId;

    /** 会话 Key */
    private String sessionKey;

    // ── 执行参数 ─────────────────────────────────────────────────────

    /** 执行模式：inline / fork */
    private ExecutionContext executionContext;

    /** 本次调用传入的参数文本（可为 null） */
    private String args;

    // ── 执行结果 ─────────────────────────────────────────────────────

    /** 是否执行成功 */
    private Boolean success;

    /** 失败时的错误信息 */
    private String errorMessage;

    /** 执行耗时（毫秒），null 表示未统计 */
    private Long durationMs;

    // ── 时间 ─────────────────────────────────────────────────────────

    /** 调用时间 */
    private LocalDateTime invokedAt;
}

