package org.dragon.tool.domain;

import lombok.Builder;
import lombok.Data;
import org.dragon.tool.enums.ExecutionStatus;
import org.dragon.tool.enums.ToolStorageType;
import org.dragon.tool.runtime.PermissionResult;
import org.dragon.tool.runtime.ValidationResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 工具执行记录。
 *
 * <p>完整记录一次工具执行的所有信息，包括：
 * <ul>
 *   <li>执行元数据（ID、工具名、时间、状态等）</li>
 *   <li>输入参数</li>
 *   <li>权限检查结果</li>
 *   <li>执行过程日志</li>
 *   <li>输出结果（或结果存储元信息）</li>
 * </ul>
 *
 * <h3>生命周期：</h3>
 * <pre>
 * 1. 创建记录 (status=RUNNING)
 * 2. 参数校验 → 记录校验结果
 * 3. 权限检查 → 记录权限结果
 * 4. 执行工具 → 记录执行日志
 * 5. 处理结果 → 如果结果大，存储到 ToolResultStorage
 * 6. 更新状态 (status=SUCCESS/FAILED/...)
 * </pre>
 */
@Data
@Builder(toBuilder = true)
public class ToolExecutionRecordDO {

    // ── 执行元数据 ───────────────────────────────────────────────────────

    /**
     * 执行记录 ID（主键）。
     */
    private final String executionId;

    /**
     * 工具调用 ID（Cla API 中的 tool_use_id）。
     */
    private final String toolUseId;

    /**
     * 工具名称。
     */
    private final String toolName;

    /**
     * 租户 ID。
     */
    private final String tenantId;

    /**
     * 会话 ID。
     */
    private final String sessionId;

    /**
     * 代理 ID（如果是子代理）。
     */
    private final String agentId;

    /**
     * 父执行 ID（如果是嵌套调用）。
     */
    private final String parentExecutionId;

    /**
     * 关联的消息 ID。
     */
    private final String messageId;

    // ── 时间信息 ─────────────────────────────────────────────────────────

    /**
     * 执行开始时间。
     */
    private final Instant startTime;

    /**
     * 执行结束时间。
     */
    private final Instant endTime;

    /**
     * 执行耗时（毫秒）。
     */
    private final Long durationMs;

    // ── 状态信息 ─────────────────────────────────────────────────────────

    /**
     * 执行状态。
     */
    @Builder.Default
    private ExecutionStatus status = ExecutionStatus.RUNNING;

    /**
     * 重试次数。
     */
    @Builder.Default
    private int retryCount = 0;

    // ── 输入参数 ─────────────────────────────────────────────────────────

    /**
     * 原始输入参数（JSON 格式）。
     */
    private final Map<String, Object> input;

    /**
     * 脱敏后的输入参数（用于日志和审计）。
     */
    private final Map<String, Object> inputSanitized;

    /**
     * 参数校验结果。
     */
    private final ValidationResult validationResult;

    // ── 权限检查 ─────────────────────────────────────────────────────────

    /**
     * 权限检查结果。
     */
    private final PermissionResult permissionResult;

    /**
     * 权限决策来源（config / user_permanent / user_temporary / hook）。
     */
    private final String permissionSource;

    // ── 执行过程 ─────────────────────────────────────────────────────────

    /**
     * 执行事件日志。
     */
    private final List<ExecutionEvent> events;

    /**
     * 执行进度信息。
     */
    private final ProgressInfo progress;

    // ── 输出结果 ─────────────────────────────────────────────────────────

    /**
     * 执行结果（小结果直接存储）。
     */
    private final Object result;

    /**
     * 结果存储元信息（大结果的存储引用）。
     */
    private final ResultStorageMeta resultStorageMeta;

    /**
     * 错误信息（如果失败）。
     */
    private final ErrorInfo error;

    /**
     * 结果是否被持久化到外部存储。
     */
    public boolean isResultPersisted() {
        return resultStorageMeta != null;
    }

    // ── 内部类型 ─────────────────────────────────────────────────────────

    /**
     * 执行事件。
     */
    @Data
    @Builder
    public static class ExecutionEvent {
        private final Instant timestamp;
        private final String type;      // start, progress, checkpoint, error, end
        private final String message;
        private final Map<String, Object> data;
    }

    /**
     * 进度信息。
     */
    @Data
    @Builder
    public static class ProgressInfo {
        private final int current;
        private final int total;
        private final String stage;
        private final String message;
        private final Double percentage;
    }

    /**
     * 结果存储元信息。
     */
    @Data
    @Builder
    public static class ResultStorageMeta {
        /**
         * 存储后端类型（{@link ToolStorageType#LOCAL} 或 {@link ToolStorageType#S3}），
         * 决定 {@link #storagePath} 的解析方式。
         */
        private final ToolStorageType storageType;

        /**
         * 存储路径（本地文件为绝对路径，S3 为 {@code s3://bucket/key} 格式）。
         */
        private final String storagePath;

        /**
         * 内容字符数。
         */
        private final long contentSize;

        /**
         * 内容类型（如 {@code text/plain}）。
         */
        private final String contentType;

        /**
         * 预览内容（前 2000 字符）。
         */
        private final String preview;

        /**
         * 是否有更多内容被截断。
         */
        private final boolean hasMore;
    }

    /**
     * 错误信息。
     */
    @Data
    @Builder
    public static class ErrorInfo {
        private final String type;          // validation, permission, execution, timeout
        private final String code;          // 错误码
        private final String message;       // 错误消息
        private final String stackTrace;    // 堆栈跟踪（可选）
        private final boolean retryable;    // 是否可重试
    }

    // ── 便捷方法 ─────────────────────────────────────────────────────────

    /**
     * 创建初始记录（状态为 RUNNING）。
     */
    public static ToolExecutionRecordDO create(String executionId,
                                               String toolUseId,
                                               String toolName,
                                               String tenantId,
                                               String sessionId,
                                               Map<String, Object> input) {
        return ToolExecutionRecordDO.builder()
                .executionId(executionId)
                .toolUseId(toolUseId)
                .toolName(toolName)
                .tenantId(tenantId)
                .sessionId(sessionId)
                .input(input)
                .startTime(Instant.now())
                .status(ExecutionStatus.RUNNING)
                .events(List.of())
                .build();
    }

    /**
     * 标记执行成功。
     */
    public ToolExecutionRecordDO markSuccess(Object result, ResultStorageMeta storageMeta) {
        return ToolExecutionRecordDO.builder()
                .executionId(executionId)
                .toolUseId(toolUseId)
                .toolName(toolName)
                .tenantId(tenantId)
                .sessionId(sessionId)
                .agentId(agentId)
                .parentExecutionId(parentExecutionId)
                .messageId(messageId)
                .startTime(startTime)
                .endTime(Instant.now())
                .durationMs(java.time.Duration.between(startTime, Instant.now()).toMillis())
                .status(ExecutionStatus.SUCCESS)
                .retryCount(retryCount)
                .input(input)
                .inputSanitized(inputSanitized)
                .validationResult(validationResult)
                .permissionResult(permissionResult)
                .permissionSource(permissionSource)
                .events(events)
                .progress(progress)
                .result(result)
                .resultStorageMeta(storageMeta)
                .build();
    }

    /**
     * 标记执行失败。
     */
    public ToolExecutionRecordDO markFailed(ErrorInfo error) {
        return ToolExecutionRecordDO.builder()
                .executionId(executionId)
                .toolUseId(toolUseId)
                .toolName(toolName)
                .tenantId(tenantId)
                .sessionId(sessionId)
                .agentId(agentId)
                .parentExecutionId(parentExecutionId)
                .messageId(messageId)
                .startTime(startTime)
                .endTime(Instant.now())
                .durationMs(java.time.Duration.between(startTime, Instant.now()).toMillis())
                .status(error.isRetryable() ? ExecutionStatus.FAILED : ExecutionStatus.FAILED)
                .retryCount(retryCount)
                .input(input)
                .inputSanitized(inputSanitized)
                .validationResult(validationResult)
                .permissionResult(permissionResult)
                .permissionSource(permissionSource)
                .events(events)
                .progress(progress)
                .error(error)
                .build();
    }
}
