package org.dragon.schedule.entity;

/**
 * 错过触发策略枚举
 */
public enum MisfirePolicy {
    /**
     * 忽略错过的触发，等待下一次正常调度
     */
    IGNORE,

    /**
     * 立即触发一次错过的任务
     */
    FIRE_NOW,

    /**
     * 智能策略：根据错过时间决定是否触发
     * 如果错过时间不超过阈值则立即触发，否则忽略
     */
    SMART
}
