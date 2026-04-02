package org.dragon.memv2.core;

/**
 * 记忆路由策略接口
 * 负责判断候选记忆应该存储到哪个作用域（CHARACTER / WORKSPACE / SESSION）
 *
 * @author binarytom
 * @version 1.0
 */
public interface MemoryRoutingPolicy {
    /**
     * 路由决策
     *
     * @param candidate 候选记忆条目
     * @param snapshot 会话快照
     * @return 记忆作用域
     */
    MemoryScope route(MemoryEntry candidate, SessionSnapshot snapshot);

    /**
     * 判断是否应该将记忆提升到长期记忆
     *
     * @param candidate 候选记忆条目
     * @param snapshot 会话快照
     * @return 是否应该提升到长期记忆
     */
    default boolean shouldPromote(MemoryEntry candidate, SessionSnapshot snapshot) {
        return route(candidate, snapshot) != MemoryScope.SESSION;
    }
}
