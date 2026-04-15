package org.dragon.character.enums;

import java.util.List;

/**
 * BuiltinType Built-in Character 类型枚举
 *
 * <p>定义所有内置 Character 的类型、元数据和关联资源。
 *
 * @author yijunw
 * @version 1.0
 */
public enum BuiltinType {

    /**
     * HR Manager - 负责人力资源管理
     */
    HR("hr", "HR Manager", "负责 Workspace 的人力资源管理，包括招聘、解雇、职责分配等",
       List.of("hire_character", "fire_character", "assign_duty", "list_candidates", "evaluate_character"),
       List.of("hr.hire.decision", "hr.hire.select", "hr.fire.decision", "hr.duty.generate"),
       "hr_trait", "hr"),

    /**
     * Member Selector - 负责选择最合适的执行者
     */
    MEMBER_SELECTOR("member_selector", "Member Selector", "负责从 Workspace 中已雇佣的 Character 中选择最合适的执行者来完成特定任务",
       List.of("list_workspace_members", "get_member_profile", "select_member"),
       List.of("memberSelector.select"),
       "member_selector_trait", "member_selection"),

    /**
     * Project Manager - 负责任务拆解和进度管理
     */
    PROJECT_MANAGER("project_manager", "Project Manager", "负责将复杂任务拆解为可执行的子任务，并管理任务执行进度",
       List.of("decompose_task", "assign_subtask", "get_task_status"),
       List.of("projectManager.decompose", "projectManager.continuationDecision"),
       "project_manager_trait", "project_management"),

    /**
     * Prompt Writer - 负责 prompt 模板与动态数据拼接
     */
    PROMPT_WRITER("prompt_writer", "Prompt Writer", "负责将 prompt 模板与动态数据拼接成完整的 prompt",
       List.of("get_workspace_common_sense"),
       List.of("react.execute", "react.taskDecompose"),
       "prompt_writer_trait", "prompt_generation"),

    /**
     * Common Sense Writer - 负责常识转换为 prompt 格式
     */
    COMMONSENSE_WRITER("commonsense_writer", "Common Sense Writer", "负责将 CommonSense 常识转换成 prompt 格式",
       List.of(),
       List.of(),
       "commonsense_writer_trait", "commonsense_generation"),

    /**
     * Material Summary - 负责物料生成摘要
     */
    MATERIAL_SUMMARY("material_summary", "Material Summary", "负责为物料生成摘要",
       List.of(),
       List.of(),
       "material_summary_trait", "material_summary"),

    /**
     * Observer Advisor (Workspace) - 提供 Workspace 级别的优化建议
     */
    OBSERVER_ADVISOR_WORKSPACE("observer_advisor_workspace", "Observer Advisor", "提供 Workspace 级别的系统优化建议",
       List.of("explore_observation_needs", "get_character_state", "get_workspace_state", "get_recent_tasks", "get_evaluation_records"),
       List.of("observer.suggestion", "observer.personalityEnhancement"),
       "observer_advisor_workspace_trait", "observer_workspace"),

    /**
     * Observer Advisor (Character) - 提供 Character 级别的优化建议
     */
    OBSERVER_ADVISOR_CHARACTER("observer_advisor_character", "Observer Advisor", "提供 Character 级别的系统优化建议",
       List.of("explore_observation_needs", "get_character_state", "get_workspace_state", "get_recent_tasks", "get_evaluation_records"),
       List.of("observer.suggestion", "observer.personalityEnhancement"),
       "observer_advisor_character_trait", "observer_character");

    private final String id;
    private final String name;
    private final String description;
    private final List<String> toolNames;
    private final List<String> promptKeys;
    private final String traitId;
    private final String scope;

    BuiltinType(String id, String name, String description,
                List<String> toolNames, List<String> promptKeys, String traitId, String scope) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.toolNames = toolNames;
        this.promptKeys = promptKeys;
        this.traitId = traitId;
        this.scope = scope;
    }

    /**
     * 获取 Character ID
     */
    public String getId() {
        return id;
    }

    /**
     * 获取名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取允许使用的工具名称列表
     */
    public List<String> getToolNames() {
        return toolNames;
    }

    /**
     * 获取使用的 prompt key 列表
     */
    public List<String> getPromptKeys() {
        return promptKeys;
    }

    /**
     * 获取关联的 Trait ID
     */
    public String getTraitId() {
        return traitId;
    }

    /**
     * 获取 Scope 标识
     */
    public String getScope() {
        return scope;
    }
}
