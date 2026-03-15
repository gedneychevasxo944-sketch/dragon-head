package org.dragon.schedule.core;

import org.dragon.schedule.entity.CronDefinition;

import java.util.List;

/**
 * 调度引擎接口
 * 负责调度算法的核心实现
 */
public interface ScheduleEngine {

    /**
     * 启动调度引擎
     */
    void start();

    /**
     * 停止调度引擎
     */
    void stop();

    /**
     * 调度 Cron 任务
     *
     * @param definition Cron 定义
     */
    void schedule(CronDefinition definition);

    /**
     * 取消调度
     *
     * @param cronId Cron ID
     */
    void unschedule(String cronId);

    /**
     * 重新调度
     *
     * @param definition Cron 定义
     */
    void reschedule(CronDefinition definition);

    /**
     * 暂停调度
     *
     * @param cronId Cron ID
     */
    void pause(String cronId);

    /**
     * 恢复调度
     *
     * @param cronId Cron ID
     */
    void resume(String cronId);

    /**
     * 触发执行
     *
     * @param cronId Cron ID
     */
    void trigger(String cronId);

    /**
     * 检查是否已调度
     *
     * @param cronId Cron ID
     * @return boolean
     */
    boolean isScheduled(String cronId);

    /**
     * 获取所有已调度的 Cron ID
     *
     * @return List<String>
     */
    List<String> getScheduledCronIds();

    /**
     * 获取调度引擎状态
     *
     * @return ScheduleEngineState
     */
    ScheduleEngineState getState();

    /**
     * 调度引擎状态
     */
    enum ScheduleEngineState {
        CREATED,
        STARTING,
        RUNNING,
        STOPPING,
        STOPPED
    }
}
