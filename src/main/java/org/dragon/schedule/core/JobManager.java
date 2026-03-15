package org.dragon.schedule.core;

import org.dragon.schedule.entity.CronDefinition;
import org.dragon.schedule.entity.ExecutionHistory;
import org.dragon.schedule.entity.JobContext;

import java.util.List;

/**
 * 任务管理器接口
 * 负责任务执行的管理和监控
 */
public interface JobManager {

    /**
     * 执行任务
     *
     * @param definition Cron 定义
     * @param context 执行上下文
     * @return ExecutionHistory 执行历史
     */
    ExecutionHistory executeJob(CronDefinition definition, JobContext context);

    /**
     * 异步执行任务
     *
     * @param definition Cron 定义
     * @param context 执行上下文
     */
    void executeJobAsync(CronDefinition definition, JobContext context);

    /**
     * 取消正在执行的任务
     *
     * @param executionId 执行 ID
     * @return 是否成功取消
     */
    boolean cancelJob(String executionId);

    /**
     * 检查任务是否正在执行
     *
     * @param executionId 执行 ID
     * @return boolean
     */
    boolean isJobRunning(String executionId);

    /**
     * 获取正在执行的任务列表
     *
     * @return List<ExecutionHistory>
     */
    List<ExecutionHistory> getRunningJobs();

    /**
     * 获取执行历史
     *
     * @param executionId 执行 ID
     * @return ExecutionHistory
     */
    ExecutionHistory getExecutionHistory(String executionId);

    /**
     * 获取 Cron 任务的执行历史
     *
     * @param cronId Cron ID
     * @param limit 限制条数
     * @return List<ExecutionHistory>
     */
    List<ExecutionHistory> getExecutionHistoryByCronId(String cronId, int limit);

    /**
     * 记录执行历史
     *
     * @param history 执行历史
     */
    void recordExecutionHistory(ExecutionHistory history);

    /**
     * 更新执行历史
     *
     * @param history 执行历史
     */
    void updateExecutionHistory(ExecutionHistory history);
}
