package org.dragon.schedule.store;

import io.ebean.Database;
import org.dragon.datasource.entity.ExecutionHistoryEntity;
import org.dragon.schedule.entity.ExecutionHistory;
import org.dragon.schedule.entity.ExecutionStatus;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlExecutionHistoryStore 执行历史MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlExecutionHistoryStore implements ExecutionHistoryStore {

    private final Database mysqlDb;

    public MySqlExecutionHistoryStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(ExecutionHistory history) {
        ExecutionHistoryEntity entity = ExecutionHistoryEntity.fromExecutionHistory(history);
        // todo chenzhijie 临时先去掉，这个会一直写数据库，之后还原一下
//        mysqlDb.save(entity);
    }

    @Override
    public void update(ExecutionHistory history) {
        ExecutionHistoryEntity entity = ExecutionHistoryEntity.fromExecutionHistory(history);
        mysqlDb.update(entity);
    }

    @Override
    public Optional<ExecutionHistory> findByExecutionId(String executionId) {
        ExecutionHistoryEntity entity = mysqlDb.find(ExecutionHistoryEntity.class)
                .where()
                .eq("executionId", executionId)
                .findOne();
        return entity != null ? Optional.of(entity.toExecutionHistory()) : Optional.empty();
    }

    @Override
    public List<ExecutionHistory> findByCronId(String cronId, int limit) {
        return mysqlDb.find(ExecutionHistoryEntity.class)
                .where()
                .eq("cronId", cronId)
                .orderBy()
                .desc("triggerTime")
                .setMaxRows(limit)
                .findList()
                .stream()
                .map(ExecutionHistoryEntity::toExecutionHistory)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExecutionHistory> findByStatus(ExecutionStatus status, int limit) {
        return mysqlDb.find(ExecutionHistoryEntity.class)
                .where()
                .eq("status", status.name())
                .setMaxRows(limit)
                .findList()
                .stream()
                .map(ExecutionHistoryEntity::toExecutionHistory)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExecutionHistory> findRunningJobs() {
        return findByStatus(ExecutionStatus.RUNNING, Integer.MAX_VALUE);
    }

    @Override
    public List<ExecutionHistory> findByExecuteNode(String nodeId) {
        return mysqlDb.find(ExecutionHistoryEntity.class)
                .where()
                .eq("executeNode", nodeId)
                .findList()
                .stream()
                .map(ExecutionHistoryEntity::toExecutionHistory)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String executionId) {
        mysqlDb.find(ExecutionHistoryEntity.class)
                .where()
                .eq("executionId", executionId)
                .delete();
    }

    @Override
    public int deleteBefore(long beforeTime) {
        List<ExecutionHistoryEntity> toDelete = mysqlDb.find(ExecutionHistoryEntity.class)
                .where()
                .lt("triggerTime", beforeTime)
                .findList();

        for (ExecutionHistoryEntity entity : toDelete) {
            mysqlDb.delete(ExecutionHistoryEntity.class, entity.getId());
        }
        return toDelete.size();
    }

    @Override
    public long count() {
        return mysqlDb.find(ExecutionHistoryEntity.class).findCount();
    }

    @Override
    public long countByCronId(String cronId) {
        return mysqlDb.find(ExecutionHistoryEntity.class)
                .where()
                .eq("cronId", cronId)
                .findCount();
    }
}
