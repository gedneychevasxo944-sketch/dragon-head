package org.dragon.memory.app;

import org.dragon.memory.core.MemoryEntry;
import org.dragon.memory.core.SessionToLongTermBridge;
import org.dragon.memory.core.SessionMemoryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 会话记忆到长期记忆的转换桥接默认实现
 * 负责管理会话记忆到角色记忆或工作空间记忆的转换过程
 *
 * @author binarytom
 * @version 1.0
 */
@Service
public class DefaultSessionToLongTermBridge implements SessionToLongTermBridge {
    private final SessionMemoryService sessionMemoryService;

    public DefaultSessionToLongTermBridge(SessionMemoryService sessionMemoryService) {
        this.sessionMemoryService = sessionMemoryService;
    }

    @Override
    public List<MemoryEntry> convertSessionToLongTerm(String sessionId) {
        if (!shouldConvert(sessionId)) {
            return List.of();
        }

        List<MemoryEntry> convertedEntries = sessionMemoryService.promote(sessionId);
        cleanupSessionMemory(sessionId);
        return convertedEntries;
    }

    @Override
    public List<MemoryEntry> convertSessionsToLongTerm(List<String> sessionIds) {
        List<MemoryEntry> allConvertedEntries = new ArrayList<>();
        for (String sessionId : sessionIds) {
            if (shouldConvert(sessionId)) {
                List<MemoryEntry> convertedEntries = sessionMemoryService.promote(sessionId);
                allConvertedEntries.addAll(convertedEntries);
                cleanupSessionMemory(sessionId);
            }
        }
        return allConvertedEntries;
    }

    @Override
    public boolean shouldConvert(String sessionId) {
        // 可以添加更多条件，比如会话持续时间、记忆数量等
        return true; // 目前默认都应该转换
    }

    @Override
    public void cleanupSessionMemory(String sessionId) {
        sessionMemoryService.close(sessionId);
    }
}
