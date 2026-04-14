package org.dragon.tool.store;

import io.ebean.Database;
import org.dragon.datasource.entity.ToolExecutionRecordEntity;
import org.dragon.tool.domain.ToolExecutionRecordDO;
import org.dragon.tool.enums.ExecutionStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlToolExecutionRecordStore — 基于 Ebean ORM 的 MySQL 存储实现。
 *
 * <p>对于 {@link #appendEvent} 和 {@link #updateProgress} 两个方法，
 * 由于当前 Entity 采用扁平化结构（events/progress 未持久化到 DB），
 * 这两个方法在 MySQL 实现中为空操作（no-op），事件/进度信息在内存中维护。
 * 如需持久化事件流，可扩展 Entity 添加 JSON 字段并更新 migration。
 */
@Component
public class MySqlToolExecutionRecordStore implements ToolExecutionRecordStore {

    private final Database mysqlDb;

    public MySqlToolExecutionRecordStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(ToolExecutionRecordDO record) {
        ToolExecutionRecordEntity entity = ToolExecutionRecordEntity.fromDomain(record);
        mysqlDb.save(entity);
    }

    @Override
    public void update(ToolExecutionRecordDO record) {
        ToolExecutionRecordEntity entity = ToolExecutionRecordEntity.fromDomain(record);
        mysqlDb.update(entity);
    }

    @Override
    public void updateStatus(String executionId, ExecutionStatus status) {
        mysqlDb.sqlUpdate(
                "UPDATE tool_execution_record SET status = :status WHERE execution_id = :id"
        )
                .setParameter("status", status.name())
                .setParameter("id", executionId)
                .execute();
    }

    @Override
    public void appendEvent(String executionId, ToolExecutionRecordDO.ExecutionEvent event) {
        // events 字段未持久化到 DB，当前为 no-op。
        // 如需持久化，扩展 Entity 增加 events JSON 字段并使用 JSON_ARRAY_APPEND。
    }

    @Override
    public void updateProgress(String executionId, ToolExecutionRecordDO.ProgressInfo progress) {
        // progress 字段未持久化到 DB，当前为 no-op。
        // 如需持久化，扩展 Entity 增加 progress JSON 字段。
    }

    @Override
    public Optional<ToolExecutionRecordDO> findById(String executionId) {
        ToolExecutionRecordEntity entity = mysqlDb.find(ToolExecutionRecordEntity.class, executionId);
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public Optional<ToolExecutionRecordDO> findByToolUseId(String toolUseId) {
        ToolExecutionRecordEntity entity = mysqlDb.find(ToolExecutionRecordEntity.class)
                .where()
                .eq("toolUseId", toolUseId)
                .findOne();
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public List<ToolExecutionRecordDO> findBySessionId(String sessionId) {
        return mysqlDb.find(ToolExecutionRecordEntity.class)
                .where()
                .eq("sessionId", sessionId)
                .orderBy("startedAt desc")
                .findList()
                .stream()
                .map(ToolExecutionRecordEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ToolExecutionRecordDO> findBySessionId(String sessionId, int offset, int limit) {
        return mysqlDb.find(ToolExecutionRecordEntity.class)
                .where()
                .eq("sessionId", sessionId)
                .orderBy("startedAt desc")
                .setFirstRow(offset)
                .setMaxRows(limit)
                .findList()
                .stream()
                .map(ToolExecutionRecordEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int deleteBySessionId(String sessionId) {
        return mysqlDb.find(ToolExecutionRecordEntity.class)
                .where()
                .eq("sessionId", sessionId)
                .delete();
    }

    @Override
    public int deleteByTenantId(String tenantId) {
        return mysqlDb.find(ToolExecutionRecordEntity.class)
                .where()
                .eq("tenantId", tenantId)
                .delete();
    }

    @Override
    public int cleanupExpired(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        return mysqlDb.sqlUpdate(
                "DELETE FROM tool_execution_record WHERE created_at < :cutoff"
        )
                .setParameter("cutoff", cutoff)
                .execute();
    }
}

