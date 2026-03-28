package org.dragon.schedule.store;

import org.dragon.schedule.entity.ExecutionHistory;
import org.dragon.schedule.entity.ExecutionStatus;
import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * 执行历史存储接口
 */
public interface ExecutionHistoryStore extends Store {

    /**
     * 保存执行历史
     *
     * @param history 执行历史
     */
    void save(ExecutionHistory history);

    /**
     * 更新执行历史
     *
     * @param history 执行历史
     */
    void update(ExecutionHistory history);

    /**
     * 根据执行 ID 查找
     *
     * @param executionId 执行 ID
     * @return Optional<ExecutionHistory>
     */
    Optional<ExecutionHistory> findByExecutionId(String executionId);

    /**
     * 根据 Cron ID 查找执行历史
     *
     * @param cronId Cron ID
     * @param limit 限制条数
     * @return List<ExecutionHistory>
     */
    List<ExecutionHistory> findByCronId(String cronId, int limit);

    /**
     * 根据状态查找
     *
     * @param status 执行状态
     * @param limit 限制条数
     * @return List<ExecutionHistory>
     */
    List<ExecutionHistory> findByStatus(ExecutionStatus status, int limit);

    /**
     * 查找正在执行的任务
     *
     * @return List<ExecutionHistory>
     */
    List<ExecutionHistory> findRunningJobs();

    /**
     * 根据执行节点查找
     *
     * @param nodeId 节点 ID
     * @return List<ExecutionHistory>
     */
    List<ExecutionHistory> findByExecuteNode(String nodeId);

    /**
     * 删除历史记录
     *
     * @param executionId 执行 ID
     */
    void delete(String executionId);

    /**
     * 批量删除历史记录
     *
     * @param beforeTime 删除此时间之前的历史
     * @return 删除的记录数
     */
    int deleteBefore(long beforeTime);

    /**
     * 计数
     *
     * @return long
     */
    long count();

    /**
     * 根据 Cron ID 计数
     *
     * @param cronId Cron ID
     * @return long
     */
    long countByCronId(String cronId);
}
