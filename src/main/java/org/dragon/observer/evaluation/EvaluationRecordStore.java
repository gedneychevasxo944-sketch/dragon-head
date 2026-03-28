package org.dragon.observer.evaluation;

import org.dragon.store.Store;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * EvaluationRecord 评价记录存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface EvaluationRecordStore extends Store {

    /**
     * 保存评价记录
     *
     * @param record 评价记录
     * @return 保存后的记录
     */
    EvaluationRecord save(EvaluationRecord record);

    /**
     * 根据 ID 查询
     *
     * @param id 记录 ID
     * @return Optional 记录
     */
    Optional<EvaluationRecord> findById(String id);

    /**
     * 根据目标查询
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @return 记录列表
     */
    List<EvaluationRecord> findByTarget(EvaluationRecord.TargetType targetType, String targetId);

    /**
     * 根据目标查询最近的 N 条记录
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @param limit      数量限制
     * @return 记录列表
     */
    List<EvaluationRecord> findRecentByTarget(EvaluationRecord.TargetType targetType, String targetId, int limit);

    /**
     * 根据时间范围查询
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 记录列表
     */
    List<EvaluationRecord> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据目标和时间范围查询
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @return 记录列表
     */
    List<EvaluationRecord> findByTargetAndTimeRange(
            EvaluationRecord.TargetType targetType, String targetId,
            LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据任务 ID 查询
     *
     * @param taskId 任务 ID
     * @return Optional 记录
     */
    Optional<EvaluationRecord> findByTaskId(String taskId);

    /**
     * 查询低于阈值的评价记录
     *
     * @param threshold 阈值
     * @return 记录列表
     */
    List<EvaluationRecord> findBelowThreshold(double threshold);

    /**
     * 删除记录
     *
     * @param id 记录 ID
     * @return 是否删除成功
     */
    boolean delete(String id);

    /**
     * 获取总数
     *
     * @return 总数
     */
    int count();

    /**
     * 清除所有记录
     */
    void clear();
}
