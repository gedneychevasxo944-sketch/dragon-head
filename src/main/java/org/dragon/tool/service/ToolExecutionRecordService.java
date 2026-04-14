package org.dragon.tool.service;

import lombok.extern.slf4j.Slf4j;
import org.dragon.tool.domain.ToolExecutionRecordDO;
import org.dragon.tool.enums.ExecutionStatus;
import org.dragon.tool.runtime.PermissionResult;
import org.dragon.tool.runtime.ValidationResult;
import org.dragon.tool.store.ToolExecutionRecordStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 工具执行记录服务。
 *
 * <p><b>职责单一</b>：仅负责执行记录的生命周期管理（写入 {@link ToolExecutionRecordStore}），
 * 不涉及大结果的外部存储。大结果落存由上层 {@code ToolExecutionService} 主链路负责，
 * 落存完成后将 {@link ToolExecutionRecordDO.ResultStorageMeta} 传入 {@link #completeWithResult}。
 *
 * <h3>执行记录生命周期：</h3>
 * <pre>
 * startExecution()
 *   → recordValidation()  （可选）
 *   → recordPermission()  （可选）
 *   → appendEvent()       （可选，多次）
 *   → completeWithResult() / completeWithError()
 * </pre>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * ToolExecutionRecord record = recordService.startExecution(
 *     "BashTool", tenantId, sessionId, toolUseId, input);
 *
 * try {
 *     // ... 执行工具 ...
 *     // 大结果落存由调用方处理，storageMeta 可为 null（小结果）
 *     recordService.completeWithResult(record.getExecutionId(), result, storageMeta);
 * } catch (Exception e) {
 *     recordService.completeWithError(record.getExecutionId(), e, isRetryable(e));
 * }
 * </pre>
 */
@Slf4j
@Service
public class ToolExecutionRecordService {

    private final ToolExecutionRecordStore store;

    // ── 构造函数 ─────────────────────────────────────────────────────────

    @Autowired
    public ToolExecutionRecordService(ToolExecutionRecordStore store) {
        this.store = store;
        log.info("[ToolExecutionRecord] 初始化完成");
    }

    // ── 执行生命周期 ─────────────────────────────────────────────────────

    /**
     * 开始执行，创建初始执行记录（状态 RUNNING）。
     *
     * @param toolName  工具名称
     * @param tenantId  租户 ID
     * @param sessionId 会话 ID
     * @param toolUseId 工具调用 ID
     * @param input     输入参数
     * @return 已持久化的执行记录
     */
    public ToolExecutionRecordDO startExecution(String toolName,
                                                String tenantId,
                                                String sessionId,
                                                String toolUseId,
                                                Map<String, Object> input) {
        String executionId = UUID.randomUUID().toString();

        ToolExecutionRecordDO record = ToolExecutionRecordDO.create(
                executionId,
                toolUseId,
                toolName,
                tenantId,
                sessionId,
                input
        );

        store.save(record);
        log.info("[ToolExecutionRecord] 开始执行: executionId={}, toolName={}, toolUseId={}",
                executionId, toolName, toolUseId);

        return record;
    }

    /**
     * 记录参数校验结果。
     */
    public void recordValidation(String executionId, ValidationResult validation) {
        Optional<ToolExecutionRecordDO> recordOpt = store.findById(executionId);
        if (recordOpt.isEmpty()) {
            log.warn("[ToolExecutionRecord] 未找到执行记录: executionId={}", executionId);
            return;
        }

        ToolExecutionRecordDO record = recordOpt.get();

        if (!validation.isValid()) {
            ToolExecutionRecordDO.ErrorInfo error = ToolExecutionRecordDO.ErrorInfo.builder()
                    .type("validation")
                    .code(validation.getErrorCode() != null ? String.valueOf(validation.getErrorCode()) : null)
                    .message(validation.getMessage())
                    .retryable(false)
                    .build();

            store.update(record.toBuilder()
                    .validationResult(validation)
                    .status(ExecutionStatus.VALIDATION_ERROR)
                    .endTime(Instant.now())
                    .error(error)
                    .build());

            log.warn("[ToolExecutionRecord] 校验失败: executionId={}, error={}",
                    executionId, validation.getMessage());
        } else {
            store.update(record.toBuilder()
                    .validationResult(validation)
                    .build());
        }
    }

    /**
     * 记录权限检查结果。
     */
    public void recordPermission(String executionId,
                                 PermissionResult permission,
                                 String source) {
        Optional<ToolExecutionRecordDO> recordOpt = store.findById(executionId);
        if (recordOpt.isEmpty()) {
            log.warn("[ToolExecutionRecord] 未找到执行记录: executionId={}", executionId);
            return;
        }

        ToolExecutionRecordDO record = recordOpt.get();

        if (permission.getBehavior() == PermissionResult.Behavior.DENY) {
            ToolExecutionRecordDO.ErrorInfo error = ToolExecutionRecordDO.ErrorInfo.builder()
                    .type("permission")
                    .message(permission.getMessage())
                    .retryable(false)
                    .build();

            store.update(record.toBuilder()
                    .permissionResult(permission)
                    .permissionSource(source)
                    .status(ExecutionStatus.PERMISSION_DENIED)
                    .endTime(Instant.now())
                    .error(error)
                    .build());

            log.warn("[ToolExecutionRecord] 权限拒绝: executionId={}, reason={}",
                    executionId, permission.getMessage());
        } else {
            store.update(record.toBuilder()
                    .permissionResult(permission)
                    .permissionSource(source)
                    .build());
        }
    }

    /**
     * 追加执行事件。
     */
    public void appendEvent(String executionId,
                            String type,
                            String message,
                            Map<String, Object> data) {
        ToolExecutionRecordDO.ExecutionEvent event = ToolExecutionRecordDO.ExecutionEvent.builder()
                .timestamp(Instant.now())
                .type(type)
                .message(message)
                .data(data)
                .build();

        store.appendEvent(executionId, event);
        log.debug("[ToolExecutionRecord] 追加事件: executionId={}, type={}", executionId, type);
    }

    /**
     * 更新执行进度。
     */
    public void updateProgress(String executionId,
                               int current,
                               int total,
                               String stage,
                               String message) {
        ToolExecutionRecordDO.ProgressInfo progress = ToolExecutionRecordDO.ProgressInfo.builder()
                .current(current)
                .total(total)
                .stage(stage)
                .message(message)
                .percentage(total > 0 ? (current * 100.0 / total) : null)
                .build();

        store.updateProgress(executionId, progress);
    }

    /**
     * 执行成功，写入执行结果元数据。
     *
     * <p>本方法<b>不负责大结果的外部存储</b>，仅将调用方已处理好的 {@code storageMeta}（可为 null）
     * 写入执行记录。大结果落存请在调用本方法前由上层完成。
     *
     * @param executionId 执行 ID
     * @param result      原始结果对象（保留供日志/调试使用）
     * @param storageMeta 大结果存储元数据；小结果或无外部存储时传 {@code null}
     */
    public void completeWithResult(String executionId,
                                   Object result,
                                   ToolExecutionRecordDO.ResultStorageMeta storageMeta) {
        Optional<ToolExecutionRecordDO> recordOpt = store.findById(executionId);
        if (recordOpt.isEmpty()) {
            log.warn("[ToolExecutionRecord] 未找到执行记录: executionId={}", executionId);
            return;
        }

        store.update(recordOpt.get().markSuccess(result, storageMeta));

        if (storageMeta != null) {
            log.info("[ToolExecutionRecord] 执行成功（结果已持久化）: executionId={}, storagePath={}",
                    executionId, storageMeta.getStoragePath());
        } else {
            log.info("[ToolExecutionRecord] 执行成功: executionId={}", executionId);
        }
    }

    /**
     * 执行失败。
     */
    public void completeWithError(String executionId,
                                  Throwable error,
                                  boolean retryable) {
        store.findById(executionId).ifPresentOrElse(
                record -> {
                    ToolExecutionRecordDO.ErrorInfo errorInfo = ToolExecutionRecordDO.ErrorInfo.builder()
                            .type("execution")
                            .code(error.getClass().getSimpleName())
                            .message(error.getMessage())
                            .retryable(retryable)
                            .build();

                    store.update(record.markFailed(errorInfo));
                    log.error("[ToolExecutionRecord] 执行失败: executionId={}, error={}",
                            executionId, error.getMessage());
                },
                () -> log.warn("[ToolExecutionRecord] 未找到执行记录: executionId={}", executionId)
        );
    }

    /**
     * 执行超时。
     */
    public void markTimeout(String executionId) {
        store.findById(executionId).ifPresentOrElse(
                record -> {
                    ToolExecutionRecordDO.ErrorInfo error = ToolExecutionRecordDO.ErrorInfo.builder()
                            .type("timeout")
                            .message("Execution timed out")
                            .retryable(true)
                            .build();

                    store.update(record.markFailed(error));
                    log.warn("[ToolExecutionRecord] 执行超时: executionId={}", executionId);
                },
                () -> log.warn("[ToolExecutionRecord] 未找到执行记录: executionId={}", executionId)
        );
    }

    /**
     * 用户取消。
     */
    public void markCancelled(String executionId) {
        store.findById(executionId).ifPresentOrElse(
                record -> {
                    ToolExecutionRecordDO.ErrorInfo error = ToolExecutionRecordDO.ErrorInfo.builder()
                            .type("cancelled")
                            .message("Execution cancelled by user")
                            .retryable(false)
                            .build();

                    store.updateStatus(executionId, ExecutionStatus.CANCELLED);
                    store.update(record.markFailed(error));
                    log.info("[ToolExecutionRecord] 执行取消: executionId={}", executionId);
                },
                () -> log.warn("[ToolExecutionRecord] 未找到执行记录: executionId={}", executionId)
        );
    }

    // ── 查询方法 ─────────────────────────────────────────────────────────

    /**
     * 查找执行记录。
     */
    public Optional<ToolExecutionRecordDO> findById(String executionId) {
        return store.findById(executionId);
    }

    /**
     * 查找会话的所有执行记录。
     */
    public List<ToolExecutionRecordDO> findBySessionId(String sessionId) {
        return store.findBySessionId(sessionId);
    }

    // ── 清理方法 ─────────────────────────────────────────────────────────

    /**
     * 清理会话的所有执行记录（仅清理 Store，外部存储由上层负责）。
     */
    public int cleanupSession(String sessionId) {
        int deleted = store.deleteBySessionId(sessionId);
        log.info("[ToolExecutionRecord] 清理会话: sessionId={}, deleted={}", sessionId, deleted);
        return deleted;
    }

    /**
     * 清理租户的所有执行记录（仅清理 Store，外部存储由上层负责）。
     */
    public int cleanupTenant(String tenantId) {
        int deleted = store.deleteByTenantId(tenantId);
        log.info("[ToolExecutionRecord] 清理租户: tenantId={}, deleted={}", tenantId, deleted);
        return deleted;
    }

    /**
     * 清理过期记录。
     */
    public int cleanupExpired(int retentionDays) {
        int deleted = store.cleanupExpired(retentionDays);
        log.info("[ToolExecutionRecord] 清理过期记录: retentionDays={}, deleted={}",
                retentionDays, deleted);
        return deleted;
    }
}
