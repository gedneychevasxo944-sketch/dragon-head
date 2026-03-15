package org.dragon.schedule.entity;

/**
 * Cron 任务状态枚举
 */
public enum CronStatus {
    /**
     * 启用状态 - 正常调度
     */
    ENABLED,

    /**
     * 禁用状态 - 不调度
     */
    DISABLED,

    /**
     * 暂停状态 - 暂时停止调度，可恢复
     */
    PAUSED
}
