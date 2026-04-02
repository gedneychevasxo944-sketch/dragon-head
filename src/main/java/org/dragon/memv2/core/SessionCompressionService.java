package org.dragon.memv2.core;

/**
 * 会话记忆压缩服务接口
 * 负责压缩和归档会话记忆，减少存储占用
 *
 * @author binarytom
 * @version 1.0
 */
public interface SessionCompressionService {
    /**
     * 压缩指定会话的记忆
     *
     * @param sessionId 会话ID
     */
    void compressSession(String sessionId);

    /**
     * 批量压缩会话记忆
     *
     * @param sessionIds 会话ID列表
     */
    void compressSessions(java.util.List<String> sessionIds);

    /**
     * 检查会话记忆是否需要压缩
     *
     * @param sessionId 会话ID
     * @return 是否需要压缩
     */
    boolean shouldCompress(String sessionId);

    /**
     * 获取会话记忆压缩后的大小
     *
     * @param sessionId 会话ID
     * @return 压缩后的大小（字节）
     */
    long getCompressedSize(String sessionId);
}
