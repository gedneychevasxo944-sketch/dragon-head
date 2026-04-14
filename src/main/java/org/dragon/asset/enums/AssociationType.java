package org.dragon.asset.enums;

/**
 * 资产关联类型
 */
public enum AssociationType {
    /**
     * Character 被添加到 Workspace
     */
    CHARACTER_WORKSPACE,

    /**
     * Memory 片段挂载到 Character
     */
    MEMORY_CHARACTER,

    /**
     * Memory 挂载到 Workspace
     */
    MEMORY_WORKSPACE,

    /**
     * Tool 被 Skill 引用
     */
    TOOL_SKILL,

    /**
     * Tool 关联到 Character
     */
    TOOL_CHARACTER,

    /**
     * Tool 关联到 Workspace
     */
    TOOL_WORKSPACE,

    /**
     * Observer 挂载到 Workspace
     */
    OBSERVER_WORKSPACE,

    /**
     * Skill 关联到 Workspace
     */
    SKILL_WORKSPACE,

    /**
     * Skill 关联到 Character
     */
    SKILL_CHARACTER,

    /**
     * Trait 关联到 Character
     */
    TRAIT_CHARACTER
}
