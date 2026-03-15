package org.dragon.schedule.core;

import org.dragon.schedule.entity.CronDefinition;
import org.dragon.schedule.entity.CronStatus;

import java.util.List;
import java.util.Optional;

/**
 * Cron 调度器核心接口
 * 对应架构文档中的 CronScheduler
 */
public interface CronScheduler {

    /**
     * 启动调度器
     */
    void start();

    /**
     * 停止调度器
     */
    void stop();

    /**
     * 注册 Cron 任务
     *
     * @param definition Cron 定义
     * @return Cron ID
     */
    String registerCron(CronDefinition definition);

    /**
     * 取消注册 Cron 任务
     *
     * @param cronId Cron ID
     */
    void unregisterCron(String cronId);

    /**
     * 更新 Cron 任务
     *
     * @param definition Cron 定义
     */
    void updateCron(CronDefinition definition);

    /**
     * 暂停 Cron 任务
     *
     * @param cronId Cron ID
     */
    void pauseCron(String cronId);

    /**
     * 恢复 Cron 任务
     *
     * @param cronId Cron ID
     */
    void resumeCron(String cronId);

    /**
     * 立即触发执行
     *
     * @param cronId Cron ID
     */
    void triggerNow(String cronId);

    /**
     * 获取 Cron 定义
     *
     * @param cronId Cron ID
     * @return Optional<CronDefinition>
     */
    Optional<CronDefinition> getCron(String cronId);

    /**
     * 列出所有 Cron 任务
     *
     * @return List<CronDefinition>
     */
    List<CronDefinition> listCrons();

    /**
     * 按状态列出 Cron 任务
     *
     * @param status Cron 状态
     * @return List<CronDefinition>
     */
    List<CronDefinition> listCronsByStatus(CronStatus status);

    /**
     * 检查调度器是否运行中
     *
     * @return boolean
     */
    boolean isRunning();
}
