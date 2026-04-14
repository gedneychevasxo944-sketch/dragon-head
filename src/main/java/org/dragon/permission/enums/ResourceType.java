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
    MEMORY_CHUNK,
    CONFIG,
    MODEL,
    TRAIT,
    TEMPLATE,
    COMMONSENSE,
    /**
     * 标签
     */
    TAG,
    /**
     * 表示所有资源类型，对应数据库中的 '*'
     */
    WILDCARD
}
