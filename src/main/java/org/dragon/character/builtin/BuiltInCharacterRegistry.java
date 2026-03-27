package org.dragon.character.builtin;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.dragon.character.config.BuiltInCharacterDefinition;

/**
 * Built-in Character 静态注册表
 * 定义所有内置 character 的配置
 *
 * @author wyj
 * @version 1.0
 */
public class BuiltInCharacterRegistry {

    private static final Map<String, BuiltInCharacterDefinition> DEFINITIONS = Map.of(
        "hr", new BuiltInCharacterDefinition(
            "hr",
            "hr_",
            "HR Manager",
            "负责 Workspace 的人力资源管理，包括招聘、解雇、职责分配等",
            List.of("hire_character", "fire_character", "assign_duty", "list_candidates", "evaluate_character"),
            List.of("hr.hire.decision", "hr.hire.select", "hr.fire.decision", "hr.duty.generate")
        ),

        "member_selector", new BuiltInCharacterDefinition(
            "member_selector",
            "member_selector_",
            "Member Selector",
            "负责从 Workspace 中已雇佣的 Character 中选择最合适的执行者来完成特定任务",
            List.of("list_workspace_members", "get_member_profile", "select_member"),
            List.of("memberSelector.select")
        ),

        "project_manager", new BuiltInCharacterDefinition(
            "project_manager",
            "project_manager_",
            "Project Manager",
            "负责将复杂任务拆解为可执行的子任务，并管理任务执行进度",
            List.of("decompose_task", "assign_subtask", "get_task_status"),
            List.of("projectManager.decompose", "projectManager.continuationDecision")
        ),

        "prompt_writer", new BuiltInCharacterDefinition(
            "prompt_writer",
            "prompt_writer_",
            "Prompt Writer",
            "负责将 prompt 模板与动态数据拼接成完整的 prompt",
            List.of("get_workspace_common_sense"),
            List.of("react.execute", "react.taskDecompose")
        ),

        "commonsense_writer", new BuiltInCharacterDefinition(
            "commonsense_writer",
            "commonsense_writer_",
            "Common Sense Writer",
            "负责将 CommonSense 常识转换成 prompt 格式",
            List.of(),
            List.of()
        ),

        "material_summary", new BuiltInCharacterDefinition(
            "material_summary",
            "material_summary_",
            "Material Summary",
            "负责为物料生成摘要",
            List.of(),
            List.of()
        ),

        "observer_advisor", new BuiltInCharacterDefinition(
            "observer_advisor",
            "observer_advisor_",
            "Observer Advisor",
            "提供系统级的优化建议，支持 global/workspace/character 三种作用域",
            List.of("explore_observation_needs", "get_character_state", "get_workspace_state", "get_recent_tasks", "get_evaluation_records"),
            List.of("observer.suggestion", "observer.personalityEnhancement")
        )
    );

    private BuiltInCharacterRegistry() {}

    /**
     * 根据 type 获取 character 定义
     *
     * @param type character 类型
     * @return 定义，如果不存在返回 empty
     */
    public static Optional<BuiltInCharacterDefinition> getDefinition(String type) {
        return Optional.ofNullable(DEFINITIONS.get(type));
    }

    /**
     * 检查是否支持指定类型
     *
     * @param type character 类型
     * @return 是否支持
     */
    public static boolean isSupported(String type) {
        return DEFINITIONS.containsKey(type);
    }

    /**
     * 获取所有支持的类型
     *
     * @return 类型列表
     */
    public static List<String> getSupportedTypes() {
        return List.copyOf(DEFINITIONS.keySet());
    }
}
