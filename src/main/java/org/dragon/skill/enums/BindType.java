package org.dragon.skill.enums;

/**
 * Skill 绑定类型枚举。
 *
 * @since 1.0
 */
public enum BindType {

    /**
     * Workspace 级别绑定。
     */
    WORKSPACE,

    /**
     * Character 全局绑定（跨所有 workspace）。
     */
    CHARACTER,

    /**
     * Character + Workspace 组合绑定（仅在特定 workspace 生效）。
     */
    CHARACTER_WORKSPACE
}