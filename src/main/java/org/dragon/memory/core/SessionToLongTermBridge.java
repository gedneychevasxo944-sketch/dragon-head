package org.dragon.memory.core;

import java.util.List;

/**
 * 会话记忆到长期记忆的转换桥接接口
 * 负责管理会话记忆到角色记忆或工作空间记忆的转换过程
 *
 * @author binarytom
 * @version 1.0
 */
public interface SessionToLongTermBridge {
    /**
     * 将会话记忆转换为长期记忆
     *
     * @param sessionId 会话ID
     * @return 转换后的长期记忆条目列表
     */
    List<MemoryEntry> convertSessionToLongTerm(String sessionId);

    /**
     * 批量转换会话记忆到长期记忆
     *
     * @param sessionIds 会话ID列表
     * @return 转换后的长期记忆条目列表
     */
    List<MemoryEntry> convertSessionsToLongTerm(List<String> sessionIds);

    /**
     * 检查会话记忆是否符合转换条件
     *
     * @param sessionId 会话ID
     * @return 是否符合转换条件
     */
    boolean shouldConvert(String sessionId);

    /**
     * 清理已转换的会话记忆
     *
     * @param sessionId 会话ID
     */
    void cleanupSessionMemory(String sessionId);
}
