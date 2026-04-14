package org.dragon.actionlog;

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

    // ==================== Plan 生命周期 ====================
    /** 生成优化计划 */
    PLAN_GENERATE,
    /** 复核优化计划 */
    PLAN_REVIEW,
    /** 审批优化计划 */
    PLAN_APPROVE,
    /** 拒绝优化计划 */
    PLAN_REJECT,
    /** 执行优化计划 */
    PLAN_EXECUTE,
    /** 回滚优化计划 */
    PLAN_ROLLBACK,

    // ==================== Schedule ====================
    /** 注册定时任务 */
    SCHEDULE_REGISTER,
    /** 定时任务触发 */
    SCHEDULE_TRIGGER,
    /** 定时任务执行 */
    SCHEDULE_EXECUTE,

    // ==================== Skill 生命周期 ====================
    /** 注册 Skill */
    SKILL_REGISTER,
    /** 更新 Skill */
    SKILL_UPDATE,
    /** 保存 Skill 草稿 */
    SKILL_SAVE_DRAFT,
    /** 发布 Skill */
    SKILL_PUBLISH,
    /** 下架 Skill */
    SKILL_DISABLE,
    /** 重新发布 Skill */
    SKILL_REPUBLISH,
    /** 删除 Skill */
    SKILL_DELETE,
    /** Skill 关联到 Character */
    SKILL_BIND_CHARACTER,
    /** Skill 关联到 Workspace */
    SKILL_BIND_WORKSPACE,
    /** Skill 解除关联 */
    SKILL_UNBIND,

    // ==================== Tool 生命周期 ====================
    /** 注册 Tool */
    TOOL_REGISTER,
    /** 更新 Tool */
    TOOL_UPDATE,
    /** 保存 Tool 草稿 */
    TOOL_SAVE_DRAFT,
    /** 发布 Tool */
    TOOL_PUBLISH,
    /** 禁用 Tool */
    TOOL_DISABLE,
    /** 启用 Tool */
    TOOL_ENABLE,
    /** 删除 Tool */
    TOOL_DELETE,
    /** Tool 绑定到 Workspace */
    TOOL_BIND_WORKSPACE,
    /** Tool 绑定到 Character */
    TOOL_BIND_CHARACTER,
    /** Tool 解除绑定 */
    TOOL_UNBIND
}
