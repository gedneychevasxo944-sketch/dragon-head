package org.dragon.datasource.entity;

import io.ebean.annotation.WhenCreated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.tool.domain.ToolExecutionRecordDO;
import org.dragon.tool.enums.ExecutionStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * ToolExecutionRecordEntity — 映射 tool_execution_record 表（工具执行记录）。
 *
 * <p>采用"扁平化关键字段 + JSON 存储复杂子对象"策略，
 * 适合在不影响查询效率的前提下保留完整的执行上下文。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tool_execution_record")
public class ToolExecutionRecordEntity {

    // ── 标识 ──────────────────────────────────────────────────────────

    @Id
    @Column(name = "execution_id", length = 64)
    private String executionId;

    /** 工具调用 ID（Claude API tool_use_id） */
    @Column(name = "tool_use_id", length = 128)
    private String toolUseId;

    /** 工具名称 */
    @Column(name = "tool_name", nullable = false, length = 128)
    private String toolName;

    // ── 上下文 ──────────────────────────────────────────────────────────

    /** 租户 ID */
    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    /** 会话 ID */
    @Column(name = "session_id", length = 128)
    private String sessionId;

    /** 代理 ID */
    @Column(name = "agent_id", length = 64)
    private String agentId;

    /** 父执行 ID（嵌套调用时有值） */
    @Column(name = "parent_execution_id", length = 64)
    private String parentExecutionId;

    /** 关联消息 ID */
    @Column(name = "message_id", length = 128)
    private String messageId;

    // ── 状态 ──────────────────────────────────────────────────────────

    /** 执行状态 */
    @Column(nullable = false, length = 20)
    private String status;

    // ── 输入/输出 ─────────────────────────────────────────────────────

    /** 输入参数（JSON） */
    @Column(name = "input_params", columnDefinition = "JSON")
    private String inputParams;

    /** 执行结果摘要（大结果仅存摘要） */
    @Column(name = "output_summary", columnDefinition = "TEXT")
    private String outputSummary;

    /** 结果存储类型 */
    @Column(name = "storage_type", length = 20)
    private String storageType;

    /** 结果存储 key */
    @Column(name = "storage_key", length = 512)
    private String storageKey;

    /** 错误信息 */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // ── 时间 ──────────────────────────────────────────────────────────

    /** 执行耗时（毫秒） */
    @Column(name = "duration_ms")
    private Long durationMs;

    /** 开始时间 */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** 结束时间 */
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    /** 记录创建时间 */
    @Column(name = "created_at")
    @WhenCreated
    private LocalDateTime createdAt;

    // ── 转换方法 ──────────────────────────────────────────────────────

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * 转换为 ToolExecutionRecordDO（仅还原可直接映射的核心字段）。
     */
    public ToolExecutionRecordDO toDomain() {
        return ToolExecutionRecordDO.builder()
                .executionId(this.executionId)
                .toolUseId(this.toolUseId)
                .toolName(this.toolName)
                .tenantId(this.tenantId)
                .sessionId(this.sessionId)
                .agentId(this.agentId)
                .parentExecutionId(this.parentExecutionId)
                .messageId(this.messageId)
                .status(this.status != null ? ExecutionStatus.valueOf(this.status) : ExecutionStatus.RUNNING)
                .durationMs(this.durationMs)
                .startTime(toInstant(this.startedAt))
                .endTime(toInstant(this.finishedAt))
                .build();
    }

    /**
     * 从 ToolExecutionRecordDO 创建 Entity（仅存储可持久化的核心字段）。
     */
    public static ToolExecutionRecordEntity fromDomain(ToolExecutionRecordDO domain) {
        // 提取输入参数的 JSON 字符串
        String inputJson = null;
        if (domain.getInput() != null) {
            try {
                inputJson = MAPPER.writeValueAsString(domain.getInput());
            } catch (Exception ignored) {
            }
        }

        // 提取错误信息
        String errorMsg = null;
        if (domain.getError() != null) {
            errorMsg = domain.getError().getMessage();
        }

        // 提取结果存储信息
        String storageTypeStr = null;
        String storageKeyStr = null;
        String outputSummary = null;
        if (domain.getResultStorageMeta() != null) {
            var meta = domain.getResultStorageMeta();
            storageTypeStr = meta.getStorageType() != null ? meta.getStorageType().name() : null;
            storageKeyStr = meta.getStoragePath();
            outputSummary = meta.getPreview();
        }

        return ToolExecutionRecordEntity.builder()
                .executionId(domain.getExecutionId())
                .toolUseId(domain.getToolUseId())
                .toolName(domain.getToolName())
                .tenantId(domain.getTenantId())
                .sessionId(domain.getSessionId())
                .agentId(domain.getAgentId())
                .parentExecutionId(domain.getParentExecutionId())
                .messageId(domain.getMessageId())
                .status(domain.getStatus() != null ? domain.getStatus().name() : ExecutionStatus.RUNNING.name())
                .inputParams(inputJson)
                .outputSummary(outputSummary)
                .storageType(storageTypeStr)
                .storageKey(storageKeyStr)
                .errorMessage(errorMsg)
                .durationMs(domain.getDurationMs())
                .startedAt(toLocalDateTime(domain.getStartTime()))
                .finishedAt(toLocalDateTime(domain.getEndTime()))
                .build();
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) return null;
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private static Instant toInstant(LocalDateTime ldt) {
        if (ldt == null) return null;
        return ldt.atZone(ZoneId.systemDefault()).toInstant();
    }
}

