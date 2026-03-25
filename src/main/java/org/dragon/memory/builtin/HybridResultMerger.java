package org.dragon.memory.builtin;

import org.dragon.memory.config.ResolvedMemorySearchConfig;
import org.dragon.memory.models.MemorySearchResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HybridResultMerger {

    private final ResolvedMemorySearchConfig searchConfig;

    public HybridResultMerger(ResolvedMemorySearchConfig searchConfig) {
        this.searchConfig = searchConfig;
    }

    /**
     * 合并关键词搜索和向量搜索结果
     */
    public List<MemorySearchResult> merge(List<MemorySearchResult> keywordResults, List<MemorySearchResult> vectorResults) {
        // 首先合并结果，去重并保留最高分
        Map<String, MemorySearchResult> mergedMap = new HashMap<>();

        // 添加关键词搜索结果
        for (MemorySearchResult result : keywordResults) {
            String key = result.getPath() + ":" + result.getStartLine() + "-" + result.getEndLine();
            if (!mergedMap.containsKey(key) || result.getScore() > mergedMap.get(key).getScore()) {
                mergedMap.put(key, result);
            }
        }

        // 添加向量搜索结果
        for (MemorySearchResult result : vectorResults) {
            String key = result.getPath() + ":" + result.getStartLine() + "-" + result.getEndLine();
            if (!mergedMap.containsKey(key) || result.getScore() > mergedMap.get(key).getScore()) {
                mergedMap.put(key, result);
            }
        }

        // 转换为列表并排序
        List<MemorySearchResult> mergedResults = new ArrayList<>(mergedMap.values());
        mergedResults = sortResults(mergedResults);

        // 应用 MMR 重排（如果启用）
        if (searchConfig.isQueryHybridMmr()) {
            mergedResults = applyMmr(mergedResults);
        }

        return mergedResults;
    }

    /**
     * 按分数降序排序结果
     */
    private List<MemorySearchResult> sortResults(List<MemorySearchResult> results) {
        return results.stream()
                .sorted((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()))
                .collect(Collectors.toList());
    }

    /**
     * 应用 MMR（Maximum Marginal Relevance）重排，以提高结果的多样性
     */
    private List<MemorySearchResult> applyMmr(List<MemorySearchResult> results) {
        List<MemorySearchResult> selected = new ArrayList<>();
        List<MemorySearchResult> candidates = new ArrayList<>(results);

        // 首先添加分数最高的结果
        if (!candidates.isEmpty()) {
            selected.add(candidates.remove(0));
        }

        // 继续添加其他结果，平衡相关性和多样性
        while (!candidates.isEmpty() && selected.size() < searchConfig.getQueryMaxResults()) {
            double bestScore = -1.0;
            int bestIndex = -1;

            for (int i = 0; i < candidates.size(); i++) {
                MemorySearchResult candidate = candidates.get(i);

                // 计算与查询的相关性分数
                double relevanceScore = candidate.getScore();

                // 计算与已选结果的多样性分数
                double diversityScore = 0.0;
                for (MemorySearchResult selectedResult : selected) {
                    diversityScore += computeSimilarity(candidate, selectedResult);
                }
                if (!selected.isEmpty()) {
                    diversityScore /= selected.size();
                }

                // 组合分数（0.5 是相关性权重，0.5 是多样性权重）
                double combinedScore = 0.5 * relevanceScore - 0.5 * diversityScore;

                if (combinedScore > bestScore) {
                    bestScore = combinedScore;
                    bestIndex = i;
                }
            }

            if (bestIndex != -1) {
                selected.add(candidates.remove(bestIndex));
            }
        }

        return selected;
    }

    /**
     * 计算两个结果之间的相似度（简单实现）
     */
    private double computeSimilarity(MemorySearchResult result1, MemorySearchResult result2) {
        // 简单实现：基于内容相似度
        String content1 = result1.getSnippet() != null ? result1.getSnippet() : "";
        String content2 = result2.getSnippet() != null ? result2.getSnippet() : "";

        // 计算 Jaccard 相似度
        if (content1.isEmpty() || content2.isEmpty()) {
            return 0.0;
        }

        int intersection = countCommonWords(content1, content2);
        int union = countUniqueWords(content1 + " " + content2);

        return (double) intersection / union;
    }

    private int countCommonWords(String str1, String str2) {
        String[] words1 = str1.toLowerCase().split("\\W+");
        String[] words2 = str2.toLowerCase().split("\\W+");

        int count = 0;
        for (String word1 : words1) {
            if (word1.isEmpty()) continue;
            for (String word2 : words2) {
                if (word2.isEmpty()) continue;
                if (word1.equals(word2)) {
                    count++;
                    break;
                }
            }
        }

        return count;
    }

    private int countUniqueWords(String str) {
        String[] words = str.toLowerCase().split("\\W+");
        Map<String, Boolean> uniqueWords = new HashMap<>();

        for (String word : words) {
            if (!word.isEmpty()) {
                uniqueWords.put(word, true);
            }
        }

        return uniqueWords.size();
    }

    /**
     * 应用时间衰减（如果启用）
     */
    private List<MemorySearchResult> applyTemporalDecay(List<MemorySearchResult> results) {
        // TODO: 实现时间衰减逻辑
        return results;
    }
}
