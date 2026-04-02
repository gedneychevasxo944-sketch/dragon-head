package org.dragon.memory.memv2.core;

/**
 * 记忆作用域枚举
 *
 * @author wyj
 * @version 1.0
 */
public enum MemoryScope {
    /**
     * 角色长期记忆
     */
    CHARACTER,

    /**
     * 工作空间共享记忆
     */
    WORKSPACE,

    /**
     * 会话短期记忆
     */
    SESSION
}
