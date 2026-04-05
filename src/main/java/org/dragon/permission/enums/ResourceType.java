package org.dragon.permission.enums;

/**
 * 资源类型
 */
public enum ResourceType {
    WORKSPACE,
    CHARACTER,
    SKILL,
    TOOL,
    OBSERVER,
    MEMORY,
    CONFIG,
    MODEL,
    TRAIT,
    TEMPLATE,
    COMMONSENSE,
    /**
     * 表示所有资源类型，对应数据库中的 '*'
     */
    WILDCARD
}
