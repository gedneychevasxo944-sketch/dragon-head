package org.dragon.impression.enums;

/**
 * ImpressionType 印象类型
 * 定义印象源和目标实体的组合类型
 */
public enum ImpressionType {
    /**
     * Character 对 Character 的印象
     */
    CHARACTER_CHARACTER,

    /**
     * Character 对 Workspace 的印象
     */
    CHARACTER_WORKSPACE,

    /**
     * Workspace 对 Character 的印象
     */
    WORKSPACE_CHARACTER,

    /**
     * Workspace 对 Workspace 的印象
     */
    WORKSPACE_WORKSPACE
}
