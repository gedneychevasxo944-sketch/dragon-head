package org.dragon.observer.actionlog;

/**
 * Observer 动作类型枚举
 * 定义所有需要上报的动作类型
 *
 * @author wyj
 * @version 1.0
 */
public enum ActionType {
    // ==================== Workspace 生命周期 ====================
    /** 创建工作空间 */
    WORKSPACE_CREATE,
    /** 删除工作空间 */
    WORKSPACE_DELETE,
    /** 激活工作空间 */
    WORKSPACE_ACTIVATE,
    /** 停用工作空间 */
    WORKSPACE_DEACTIVATE,
    /** 归档工作空间 */
    WORKSPACE_ARCHIVE,

    // ==================== 雇佣管理 ====================
    /** 雇佣 Character */
    HIRE,
    /** 解雇 Character */
    FIRE,
    /** 更新职责 */
    UPDATE_DUTY,
    /** 自动分配 */
    AUTO_ASSIGN,

    // ==================== 任务生命周期 ====================
    /** 创建任务 */
    TASK_CREATE,
    /** 开始执行任务 */
    TASK_START,
    /** 任务完成 */
    TASK_COMPLETE,
    /** 任务失败 */
    TASK_FAIL,
    /** 取消任务 */
    TASK_CANCEL,

    // ==================== 任务编排 ====================
    /** 任务分解 */
    TASK_DECOMPOSE,
    /** 成员选择 */
    MEMBER_SELECT,
    /** 子任务执行 */
    SUBTASK_EXECUTE,

    // ==================== LLM 调用 ====================
    /** LLM 调用开始 */
    LLM_CALL_START,
    /** LLM 调用完成 */
    LLM_CALL_COMPLETE,
    /** LLM 调用失败 */
    LLM_CALL_FAIL,

    // ==================== Character 执行 ====================
    /** Character 运行开始 */
    CHARACTER_RUN_START,
    /** Character 运行完成 */
    CHARACTER_RUN_COMPLETE,
    /** Character 运行失败 */
    CHARACTER_RUN_FAIL,

    // ==================== Observer ====================
    /** 触发评价 */
    EVALUATION_TRIGGER,
    /** 触发优化 */
    OPTIMIZATION_TRIGGER,
    /** 执行优化 */
    OPTIMIZATION_EXECUTE,

    // ==================== Schedule ====================
    /** 定时任务触发 */
    SCHEDULE_TRIGGER,
    /** 定时任务执行 */
    SCHEDULE_EXECUTE
}
