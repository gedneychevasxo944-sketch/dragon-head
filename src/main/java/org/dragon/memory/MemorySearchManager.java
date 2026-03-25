package org.dragon.memory;

import org.dragon.memory.config.ResolvedMemoryBackendConfig;
import org.dragon.memory.config.ResolvedMemorySearchConfig;
import org.dragon.memory.models.*;

public interface MemorySearchManager {

    /**
     * 搜索记忆
     * @param query 搜索查询
     * @param opts 搜索选项
     * @return 搜索结果列表
     */
    MemorySearchResult search(String query, SearchOptions opts);

    /**
     * 读取记忆文件
     * @param request 读取文件请求
     * @return 读取结果
     */
    ReadFileResult readFile(ReadFileRequest request);

    /**
     * 获取当前状态
     * @return 内存提供器状态
     */
    MemoryProviderStatus status();

    /**
     * 同步索引
     * @param request 同步请求
     * @return 同步进度更新
     */
    MemorySyncProgressUpdate sync(SyncRequest request);

    /**
     * 探测 embedding 可用性
     * @return embedding 探测结果
     */
    MemoryEmbeddingProbeResult probeEmbeddingAvailability();

    /**
     * 探测向量检索可用性
     * @return 向量检索探测结果
     */
    boolean probeVectorAvailability();

    /**
     * 关闭资源
     */
    void close();
}
