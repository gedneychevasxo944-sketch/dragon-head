package org.dragon.datasource.entity;

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
import org.dragon.skill.domain.SkillUsageDO;
import org.dragon.skill.enums.ExecutionContext;

import java.time.LocalDateTime;

/**
 * SkillUsageEntity — 映射 skill_usage_log 表。
 *
 * <p>每次 Skill 执行时写入一条记录，兼顾以下用途：
 * <ul>
 *   <li>使用频率统计：按 skillId / skillName 聚合调用次数，用于排序推荐</li>
 *   <li>调用来源分析：characterId / workspaceId / agentId 可下钻到具体 Agent</li>
 *   <li>执行质量分析：durationMs / success / errorMessage 用于故障排查</li>
 *   <li>版本追踪：skillVersion 记录实际执行时命中的版本号</li>
 * </ul>
 *
 * <p>建表 SQL 参考：
 * <pre>
 * CREATE TABLE skill_usage_log (
 *   id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
 *   skill_id       VARCHAR(64)   NOT NULL COMMENT '技能业务 UUID',
 *   skill_name     VARCHAR(128)  NOT NULL COMMENT '技能名称（冗余，便于查询无需 JOIN）',
 *   skill_version  INT           NOT NULL COMMENT '本次执行命中的版本号',
 *   character_id   BIGINT        NOT NULL COMMENT '执行者 Character ID',
 *   workspace_id   BIGINT                 COMMENT '所在 Workspace ID，独立模式为 NULL',
 *   agent_id       VARCHAR(64)   NOT NULL COMMENT 'Agent 实例 ID（UUID）',
 *   session_key    VARCHAR(128)  NOT NULL COMMENT '会话 Key',
 *   execution_context VARCHAR(16) NOT NULL DEFAULT 'inline' COMMENT 'inline | fork',
 *   args           TEXT                   COMMENT '本次调用传入的参数',
 *   success        TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '是否执行成功：1=成功 0=失败',
 *   error_message  TEXT                   COMMENT '失败时的错误信息',
 *   duration_ms    BIGINT                 COMMENT '执行耗时（毫秒），NULL 表示未统计',
 *   invoked_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '调用时间（毫秒精度）',
 *   INDEX idx_skill_id   (skill_id),
 *   INDEX idx_character  (character_id, invoked_at),
 *   INDEX idx_workspace  (workspace_id, invoked_at),
 *   INDEX idx_invoked_at (invoked_at)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Skill 调用记录';
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "skill_usage_log")
public class SkillUsageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Skill 信息（冗余，避免 JOIN） ─────────────────────────────────

    /** 技能业务 UUID */
    @Column(name = "skill_id", nullable = false, length = 64)
    private String skillId;

    /** 技能名称（冗余，直接用于排序聚合） */
    @Column(name = "skill_name", nullable = false, length = 128)
    private String skillName;

    /** 本次执行命中的版本号 */
    @Column(name = "skill_version")
    private Integer skillVersion;

    // ── 调用来源 ─────────────────────────────────────────────────────

    /** 执行者 Character ID */
    @Column(name = "character_id")
    private String characterId;

    /** 所在 Workspace ID（独立模式为 null） */
    @Column(name = "workspace_id")
    private String workspaceId;

    /** Agent 实例 ID（UUID，fork 模式下对应子 Agent） */
    @Column(name = "agent_id", length = 64)
    private String agentId;

    /** 会话 Key */
    @Column(name = "session_key", length = 128)
    private String sessionKey;

    // ── 执行参数 ─────────────────────────────────────────────────────

    /** 执行模式：inline（直接展开） / fork（子 Agent） */
    @Column(name = "execution_context", length = 16)
    private String executionContext;

    /** 本次调用传入的参数文本（可为 null） */
    @Column(name = "args", columnDefinition = "TEXT")
    private String args;

    // ── 执行结果 ─────────────────────────────────────────────────────

    /** 是否执行成功：true=成功，false=失败 */
    @Column(name = "success", nullable = false)
    private Boolean success;

    /** 失败时的错误信息（成功时为 null） */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** 执行耗时（毫秒），null 表示未统计（如 fork 模式异步执行） */
    @Column(name = "duration_ms")
    private Long durationMs;

    // ── 时间 ─────────────────────────────────────────────────────────

    /** 调用时间（毫秒精度） */
    @Column(name = "invoked_at")
    @WhenCreated
    private LocalDateTime invokedAt;

    // ── 转换方法 ─────────────────────────────────────────────────────

    public SkillUsageDO toDomain() {
        return SkillUsageDO.builder()
                .id(this.id)
                .skillId(this.skillId)
                .skillName(this.skillName)
                .skillVersion(this.skillVersion)
                .characterId(this.characterId)
                .workspaceId(this.workspaceId)
                .agentId(this.agentId)
                .sessionKey(this.sessionKey)
                .executionContext(this.executionContext != null
                        ? ExecutionContext.fromValue(this.executionContext) : null)
                .args(this.args)
                .success(this.success)
                .errorMessage(this.errorMessage)
                .durationMs(this.durationMs)
                .invokedAt(this.invokedAt)
                .build();
    }

    public static SkillUsageEntity fromDomain(SkillUsageDO domain) {
        return SkillUsageEntity.builder()
                .id(domain.getId())
                .skillId(domain.getSkillId())
                .skillName(domain.getSkillName())
                .skillVersion(domain.getSkillVersion())
                .characterId(domain.getCharacterId())
                .workspaceId(domain.getWorkspaceId())
                .agentId(domain.getAgentId())
                .sessionKey(domain.getSessionKey())
                .executionContext(domain.getExecutionContext() != null
                        ? domain.getExecutionContext().getValue() : null)
                .args(domain.getArgs())
                .success(domain.getSuccess())
                .errorMessage(domain.getErrorMessage())
                .durationMs(domain.getDurationMs())
                .invokedAt(domain.getInvokedAt())
                .build();
    }
}