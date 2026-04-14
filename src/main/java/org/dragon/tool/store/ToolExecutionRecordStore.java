package org.dragon.tool.store;

import org.dragon.tool.domain.ToolExecutionRecordDO;
import org.dragon.tool.enums.ExecutionStatus;

import java.util.List;
import java.util.Optional;

/**
 * ToolExecutionRecordStore — tool_execution_record 表的存储抽象接口。
 *
 * <p>对齐 {@code ToolStore} / {@code ToolVersionStore} 的设计风格：
 * <ul>
 *   <li>写操作返回 {@code void}，异常通过 unchecked exception 向上传播</li>
 *   <li>单条查询返回 {@code Optional}，列表查询返回 {@code List}（不返回 null）</li>
 *   <li>接口只声明业务必要的方法，统计/分页等扩展方法按需添加</li>
 * </ul>
 */
public interface ToolExecutionRecordStore {

    // ── 写操作 ────────────────────────────────────────────────────────

    /**
     * 保存执行记录（INSERT）。
     *
     * @param record 执行记录
     */
    void save(ToolExecutionRecordDO record);

    /**
     * 更新执行记录（UPDATE by executionId）。
     *
     * @param record 执行记录（executionId 不可为 null）
     */
    void update(ToolExecutionRecordDO record);

    /**
     * 更新执行状态。
     *
     * @param executionId 执行 ID
     * @param status      新状态
     */
    void updateStatus(String executionId, ExecutionStatus status);

    /**
     * 追加执行事件。
     *
     * @param executionId 执行 ID
     * @param event       执行事件
     */
    void appendEvent(String executionId, ToolExecutionRecordDO.ExecutionEvent event);

    /**
     * 更新进度信息。
     *
     * @param executionId 执行 ID
     * @param progress    进度信息
     */
    void updateProgress(String executionId, ToolExecutionRecordDO.ProgressInfo progress);

    // ── 读操作 ────────────────────────────────────────────────────────

    /**
     * 按 executionId 查询执行记录。
     *
     * @param executionId 执行 ID
     * @return Optional 包装的执行记录
     */
    Optional<ToolExecutionRecordDO> findById(String executionId);

    /**
     * 按 toolUseId 查询执行记录。
     *
     * @param toolUseId 工具调用 ID（LLM 侧生成的唯一 ID）
     * @return Optional 包装的执行记录
     */
    Optional<ToolExecutionRecordDO> findByToolUseId(String toolUseId);

    /**
     * 查询会话的所有执行记录，按开始时间降序。
     *
     * @param sessionId 会话 ID
     * @return 执行记录列表，不存在时返回空列表
     */
    List<ToolExecutionRecordDO> findBySessionId(String sessionId);

    /**
     * 查询会话的执行记录（带分页）。
     *
     * @param sessionId 会话 ID
     * @param offset    偏移量
     * @param limit     限制数量
     * @return 执行记录列表，不存在时返回空列表
     */
    List<ToolExecutionRecordDO> findBySessionId(String sessionId, int offset, int limit);

    // ── 清理操作 ──────────────────────────────────────────────────────

    /**
     * 批量删除会话的所有执行记录。
     *
     * @param sessionId 会话 ID
     * @return 删除的记录数
     */
    int deleteBySessionId(String sessionId);

    /**
     * 批量删除租户的所有执行记录。
     *
     * @param tenantId 租户 ID
     * @return 删除的记录数
     */
    int deleteByTenantId(String tenantId);

    /**
     * 清理过期执行记录。
     *
     * @param retentionDays 保留天数
     * @return 删除的记录数
     */
    int cleanupExpired(int retentionDays);
}

