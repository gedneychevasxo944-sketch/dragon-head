package org.dragon.memv2.app;

import org.dragon.memv2.core.SessionCompressionService;
import org.dragon.memv2.core.SessionMemoryService;
import org.dragon.memv2.core.SessionSnapshot;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 会话记忆压缩服务默认实现
 * 负责压缩和归档会话记忆，减少存储占用
 *
 * @author binarytom
 * @version 1.0
 */
@Service
public class DefaultSessionCompressionService implements SessionCompressionService {
    private final SessionMemoryService sessionMemoryService;

    public DefaultSessionCompressionService(SessionMemoryService sessionMemoryService) {
        this.sessionMemoryService = sessionMemoryService;
    }

    @Override
    public void compressSession(String sessionId) {
        if (!shouldCompress(sessionId)) {
            return;
        }

        // 这里可以添加压缩逻辑，比如：
        // 1. 压缩会话快照
        // 2. 清理过期的事件记录
        // 3. 归档到长期存储
    }

    @Override
    public void compressSessions(List<String> sessionIds) {
        for (String sessionId : sessionIds) {
            if (shouldCompress(sessionId)) {
                compressSession(sessionId);
            }
        }
    }

    @Override
    public boolean shouldCompress(String sessionId) {
        SessionSnapshot snapshot = sessionMemoryService.get(sessionId);
        if (snapshot == null) {
            return false;
        }

        // 可以添加压缩条件，比如：
        // 1. 会话持续时间超过一定阈值
        // 2. 事件记录数量超过一定阈值
        // 3. 会话大小超过一定阈值
        return true; // 目前默认都应该压缩
    }

    @Override
    public long getCompressedSize(String sessionId) {
        // 这里可以返回压缩后的大小估计
        return 0;
    }
}
