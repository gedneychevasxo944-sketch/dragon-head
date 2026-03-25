package org.dragon.sandbox.domain;

/**
 * Sandbox 状态枚举。
 *
 * @since 1.0
 */
public enum SandboxState {

    /** 初始化中 */
    INITIALIZING,

    /** 就绪，可接受执行任务 */
    READY,

    /** 执行中 */
    EXECUTING,

    /** 已销毁 */
    DESTROYED
}