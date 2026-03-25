package org.dragon.observer.evaluation;

import io.ebean.Database;
import org.dragon.datasource.entity.EvaluationRecordEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlEvaluationRecordStore 评价记录MySQL存储实现
 */
@Component
public class MySqlEvaluationRecordStore implements EvaluationRecordStore {

    private final Database mysqlDb;

    public MySqlEvaluationRecordStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public EvaluationRecord save(EvaluationRecord record) {
        EvaluationRecordEntity entity = EvaluationRecordEntity.fromEvaluationRecord(record);
        mysqlDb.save(entity);
        return record;
    }

    @Override
    public Optional<EvaluationRecord> findById(String id) {
        EvaluationRecordEntity entity = mysqlDb.find(EvaluationRecordEntity.class, id);
        return entity != null ? Optional.of(entity.toEvaluationRecord()) : Optional.empty();
    }

    @Override
    public List<EvaluationRecord> findByTarget(EvaluationRecord.TargetType targetType, String targetId) {
        return mysqlDb.find(EvaluationRecordEntity.class)
                .where()
                .eq("targetType", targetType.name())
                .eq("targetId", targetId)
                .findList()
                .stream()
                .map(EvaluationRecordEntity::toEvaluationRecord)
                .collect(Collectors.toList());
    }

    @Override
    public List<EvaluationRecord> findRecentByTarget(EvaluationRecord.TargetType targetType, String targetId, int limit) {
        return mysqlDb.find(EvaluationRecordEntity.class)
                .where()
                .eq("targetType", targetType.name())
                .eq("targetId", targetId)
                .orderBy()
                .desc("timestamp")
                .setMaxRows(limit)
                .findList()
                .stream()
                .map(EvaluationRecordEntity::toEvaluationRecord)
                .collect(Collectors.toList());
    }

    @Override
    public List<EvaluationRecord> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return mysqlDb.find(EvaluationRecordEntity.class)
                .where()
                .ge("timestamp", startTime)
                .le("timestamp", endTime)
                .findList()
                .stream()
                .map(EvaluationRecordEntity::toEvaluationRecord)
                .collect(Collectors.toList());
    }

    @Override
    public List<EvaluationRecord> findByTargetAndTimeRange(
            EvaluationRecord.TargetType targetType, String targetId,
            LocalDateTime startTime, LocalDateTime endTime) {
        return mysqlDb.find(EvaluationRecordEntity.class)
                .where()
                .eq("targetType", targetType.name())
                .eq("targetId", targetId)
                .ge("timestamp", startTime)
                .le("timestamp", endTime)
                .findList()
                .stream()
                .map(EvaluationRecordEntity::toEvaluationRecord)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<EvaluationRecord> findByTaskId(String taskId) {
        EvaluationRecordEntity entity = mysqlDb.find(EvaluationRecordEntity.class)
                .where()
                .eq("taskId", taskId)
                .findOne();
        return entity != null ? Optional.of(entity.toEvaluationRecord()) : Optional.empty();
    }

    @Override
    public List<EvaluationRecord> findBelowThreshold(double threshold) {
        return mysqlDb.find(EvaluationRecordEntity.class)
                .findList()
                .stream()
                .map(EvaluationRecordEntity::toEvaluationRecord)
                .filter(r -> {
                    if (r.getOverallScore() == null) {
                        r.calculateOverallScore();
                    }
                    return r.getOverallScore() != null && r.getOverallScore() < threshold;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean delete(String id) {
        int rows = mysqlDb.find(EvaluationRecordEntity.class, id) != null ? 1 : 0;
        mysqlDb.delete(EvaluationRecordEntity.class, id);
        return rows > 0;
    }

    @Override
    public int count() {
        return (int) mysqlDb.find(EvaluationRecordEntity.class).findCount();
    }

    @Override
    public void clear() {
        mysqlDb.find(EvaluationRecordEntity.class).delete();
    }
}
