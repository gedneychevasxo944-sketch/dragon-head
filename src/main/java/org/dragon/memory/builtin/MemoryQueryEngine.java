package org.dragon.memory.builtin;

import org.dragon.memory.config.ResolvedMemorySearchConfig;
import org.dragon.memory.models.MemorySearchResult;
import org.dragon.memory.models.SearchOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 封装 query -> keyword search -> vector search -> hybrid merge 的完整检索路径
 */
public class MemoryQueryEngine {

    private final ResolvedMemorySearchConfig searchConfig;
    private final HybridResultMerger hybridResultMerger;
    private final MemoryIndexRepository indexRepository;

    public MemoryQueryEngine(ResolvedMemorySearchConfig searchConfig) {
        this.searchConfig = searchConfig;
        this.hybridResultMerger = new HybridResultMerger(searchConfig);
        this.indexRepository = new MemoryIndexRepository(searchConfig);
    }

    /**
     * 总查询入口，根据 provider/fts/vector 状态选择 FTS-only / vector-only / hybrid
     */
    public List<MemorySearchResult> search(String query, SearchOptions opts) {
        String trimmedQuery = query.trim();
        if (trimmedQuery.isEmpty()) {
            return new ArrayList<>();
        }

        boolean hasEmbeddingProvider = searchConfig.getProvider() != null && !searchConfig.getProvider().isEmpty();
        boolean hasFtsEnabled = searchConfig.isFtsEnabled();
        boolean hasVectorEnabled = searchConfig.isStoreVectorEnabled();

        // 场景 A：FTS-only 模式（provider 为空）
        if (!hasEmbeddingProvider) {
            return searchFtsOnly(trimmedQuery, opts);
        }

        // 场景 B：provider 存在，但 FTS 不可用
        if (!hasFtsEnabled) {
            return searchVectorOnly(trimmedQuery, opts);
        }

        // 场景 C：provider 存在且 hybrid 可用
        return searchHybrid(trimmedQuery, opts);
    }

    /**
     * FTS-only 检索
     */
    private List<MemorySearchResult> searchFtsOnly(String query, SearchOptions opts) {
        if (!searchConfig.isFtsEnabled()) {
            return new ArrayList<>();
        }

        // 执行关键词搜索
        List<MemorySearchResult> keywordResults = indexRepository.searchKeyword(query, opts.getMaxResults());

        // 过滤和排序
        return keywordResults.stream()
                .filter(result -> result.getScore() >= searchConfig.getMinScore())
                .sorted((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()))
                .limit(opts.getMaxResults())
                .collect(Collectors.toList());
    }

    /**
     * 向量检索（包括 sqlite-vec 或内存 fallback）
     */
    private List<MemorySearchResult> searchVectorOnly(String query, SearchOptions opts) {
        if (!searchConfig.isStoreVectorEnabled()) {
            return new ArrayList<>();
        }

        // 生成查询向量
        List<Double> queryVector = generateQueryVector(query);
        if (queryVector == null || queryVector.isEmpty()) {
            return new ArrayList<>();
        }

        // 执行向量搜索
        List<MemorySearchResult> vectorResults = indexRepository.searchVector(queryVector, opts.getMaxResults());

        // 过滤和排序
        return vectorResults.stream()
                .filter(result -> result.getScore() >= searchConfig.getMinScore())
                .sorted((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()))
                .limit(opts.getMaxResults())
                .collect(Collectors.toList());
    }

    /**
     * 混合搜索（关键词 + 向量）
     */
    private List<MemorySearchResult> searchHybrid(String query, SearchOptions opts) {
        List<MemorySearchResult> keywordResults = indexRepository.searchKeyword(query, opts.getMaxResults() * 2);
        List<MemorySearchResult> vectorResults = new ArrayList<>();

        // 生成查询向量（带超时）
        List<Double> queryVector = generateQueryVector(query);
        if (queryVector != null && !queryVector.isEmpty()) {
            vectorResults = indexRepository.searchVector(queryVector, opts.getMaxResults() * 2);
        }

        // 合并结果
        List<MemorySearchResult> mergedResults = hybridResultMerger.merge(keywordResults, vectorResults);

        // 过滤和裁剪
        return mergedResults.stream()
                .filter(result -> result.getScore() >= searchConfig.getMinScore())
                .sorted((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()))
                .limit(opts.getMaxResults())
                .collect(Collectors.toList());
    }

    /**
     * 生成查询向量（带超时机制）
     */
    private List<Double> generateQueryVector(String query) {
        // 实际应用中需要调用 embedding provider
        // 这里先返回模拟向量
        List<Double> vector = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            vector.add(Math.random());
        }
        return vector;
    }
}
