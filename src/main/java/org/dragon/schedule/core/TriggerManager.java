package org.dragon.schedule.core;

import org.dragon.schedule.entity.CronDefinition;

/**
 * 触发器管理器接口
 * 负责管理 Cron 触发器的生命周期
 */
public interface TriggerManager {

    /**
     * 为 Cron 定义创建触发器
     *
     * @param definition Cron 定义
     */
    void createTrigger(CronDefinition definition);

    /**
     * 移除触发器
     *
     * @param cronId Cron ID
     */
    void removeTrigger(String cronId);

    /**
     * 更新触发器
     *
     * @param definition Cron 定义
     */
    void updateTrigger(CronDefinition definition);

    /**
     * 暂停触发器
     *
     * @param cronId Cron ID
     */
    void pauseTrigger(String cronId);

    /**
     * 恢复触发器
     *
     * @param cronId Cron ID
     */
    void resumeTrigger(String cronId);

    /**
     * 立即触发
     *
     * @param cronId Cron ID
     */
    void triggerNow(String cronId);

    /**
     * 检查触发器是否存在
     *
     * @param cronId Cron ID
     * @return boolean
     */
    boolean hasTrigger(String cronId);

    /**
     * 获取下次触发时间
     *
     * @param cronId Cron ID
     * @return 下次触发时间戳，如果没有则返回 null
     */
    Long getNextFireTime(String cronId);

    /**
     * 获取上次触发时间
     *
     * @param cronId Cron ID
     * @return 上次触发时间戳，如果没有则返回 null
     */
    Long getPreviousFireTime(String cronId);
}
