package org.dragon.memory;

import org.dragon.memory.models.*;

import java.util.List;

/**
 * 统一记忆搜索管理接口，不暴露 SQLite、FTS、向量、watcher、provider 细节。
 */
public interface MemorySearchManager {

    /**
     * 统一检索入口
     * @param query 搜索查询
     * @param opts 搜索选项
     * @return 搜索结果列表
     */
    List<MemorySearchResult> search(String query, SearchOptions opts);

    /**
     * 读取记忆文件片段
     * @param request 读取文件请求
     * @return 读取结果
     */
    ReadFileResult readFile(ReadFileRequest request);

    /**
     * 返回状态快照
     * @return 内存提供器状态
     */
    MemoryProviderStatus status();

    /**
     * 同步索引（single-flight 保证）
     * @param request 同步请求
     * @return 同步进度更新
     */
    MemorySyncProgressUpdate sync(SyncRequest request);

    /**
     * 探测 embedding 是否可用
     * @return embedding 探测结果
     */
    MemoryEmbeddingProbeResult probeEmbeddingAvailability();

    /**
     * 探测向量检索是否可用
     * @return 向量检索探测结果
     */
    boolean probeVectorAvailability();

    /**
     * 会话预热（仅首次 session start 异步触发）
     * @param sessionKey 会话密钥
     */
    void warmSession(String sessionKey);

    /**
     * 关闭资源
     */
    void close();
}
