package org.dragon.actionlog;

import org.dragon.observer.log.ObserverActionLog;
import org.dragon.store.Store;

import java.util.List;

/**
 * Observer ActionLog 存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface ActionLogStore extends Store {

    /**
     * 保存日志
     *
     * @param log 日志
     */
    void save(ObserverActionLog log);

    /**
     * 根据目标类型和目标ID查询日志
     *
     * @param targetType 目标类型
     * @param targetId  目标ID
     * @return 日志列表
     */
    List<ObserverActionLog> findByTarget(String targetType, String targetId);

    /**
     * 根据动作类型查询日志
     *
     * @param actionType 动作类型
     * @return 日志列表
     */
    List<ObserverActionLog> findByActionType(ActionType actionType);

    /**
     * 根据目标类型、目标ID和动作类型查询日志
     *
     * @param targetType 目标类型
     * @param targetId  目标ID
     * @param actionType 动作类型
     * @return 日志列表
     */
    List<ObserverActionLog> findByTargetAndActionType(String targetType, String targetId, ActionType actionType);

    /**
     * 获取所有日志
     *
     * @return 日志列表
     */
    List<ObserverActionLog> findAll();

    /**
     * 删除指定日志
     *
     * @param id 日志ID
     */
    void delete(String id);

    /**
     * 清空所有日志
     */
    void clear();

    /**
     * 分页查询指定目标的日志，按时间降序。
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @param offset     偏移量
     * @param limit      每页条数
     * @return 日志列表
     */
    List<ObserverActionLog> findByTarget(String targetType, String targetId, int offset, int limit);

    /**
     * 统计指定目标的日志总数。
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @return 日志总数
     */
    int countByTarget(String targetType, String targetId);
}
