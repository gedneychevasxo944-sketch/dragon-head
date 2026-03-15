package org.dragon.schedule.entity;

/**
 * 任务执行状态枚举
 */
public enum ExecutionStatus {
    /**
     * 运行中
     */
    RUNNING,

    /**
     * 执行成功
     */
    SUCCESS,

    /**
     * 执行失败
     */
    FAILED,

    /**
     * 执行超时
     */
    TIMEOUT,

    /**
     * 执行取消
     */
    CANCELLED
}
