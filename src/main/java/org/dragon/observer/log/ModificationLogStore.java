package org.dragon.observer.log;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ModificationLog 修改日志存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface ModificationLogStore {

    /**
     * 保存修改日志
     *
     * @param log 修改日志
     * @return 保存后的日志
     */
    ModificationLog save(ModificationLog log);

    /**
     * 根据 ID 查询
     *
     * @param id 日志 ID
     * @return Optional 日志
     */
    Optional<ModificationLog> findById(String id);

    /**
     * 根据目标查询
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @return 日志列表
     */
    List<ModificationLog> findByTarget(ModificationLog.TargetType targetType, String targetId);

    /**
     * 根据目标查询最近的 N 条
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @param limit      数量限制
     * @return 日志列表
     */
    List<ModificationLog> findRecentByTarget(ModificationLog.TargetType targetType, String targetId, int limit);

    /**
     * 根据时间范围查询
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 日志列表
     */
    List<ModificationLog> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据操作者查询
     *
     * @param operator 操作者
     * @return 日志列表
     */
    List<ModificationLog> findByOperator(String operator);

    /**
     * 根据触发源查询
     *
     * @param triggerSource 触发源
     * @return 日志列表
     */
    List<ModificationLog> findByTriggerSource(ModificationLog.TriggerSource triggerSource);

    /**
     * 删除日志
     *
     * @param id 日志 ID
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
     * 清除所有日志
     */
    void clear();
}
