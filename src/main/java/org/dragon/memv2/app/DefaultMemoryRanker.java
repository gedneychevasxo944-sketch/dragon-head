package org.dragon.memv2.app;

import org.dragon.memv2.core.MemoryEntry;
import org.dragon.memv2.core.MemoryRanker;
import org.dragon.memv2.core.MemorySearchResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 记忆排序服务实现类
 * 负责对记忆检索结果进行排序
 *
 * @author binarytom
 * @version 1.0
 */
@Service
public class DefaultMemoryRanker implements MemoryRanker {
    @Override
    public List<MemorySearchResult> rank(String query, List<MemoryEntry> candidates, int limit) {
        List<MemorySearchResult> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (MemoryEntry entry : candidates) {
            double score = calculateRelevanceScore(entry, lowerQuery);
            if (score > 0) {
                results.add(MemorySearchResult.builder()
                        .memory(entry)
                        .score(score)
                        .reason(getMatchReason(entry, lowerQuery))
                        .build());
            }
        }

        // 按分数降序排序
        results.sort(Comparator.comparingDouble(MemorySearchResult::getScore).reversed());

        // 限制结果数量
        if (limit > 0 && results.size() > limit) {
            return results.subList(0, limit);
        }

        return results;
    }

    /**
     * 计算记忆条目与查询的相关度分数
     */
    private double calculateRelevanceScore(MemoryEntry entry, String lowerQuery) {
        double score = 0.0;
        String lowerTitle = entry.getTitle().toLowerCase();
        String lowerContent = entry.getContent().toLowerCase();
        String lowerDescription = entry.getDescription().toLowerCase();

        // 标题匹配权重最高
        if (lowerTitle.contains(lowerQuery)) {
            score += 0.6;
        }

        // 描述匹配权重次之
        if (lowerDescription.contains(lowerQuery)) {
            score += 0.3;
        }

        // 内容匹配权重最低
        if (lowerContent.contains(lowerQuery)) {
            score += 0.1;
        }

        return score;
    }

    /**
     * 获取匹配原因
     */
    private String getMatchReason(MemoryEntry entry, String lowerQuery) {
        String lowerTitle = entry.getTitle().toLowerCase();
        String lowerContent = entry.getContent().toLowerCase();
        String lowerDescription = entry.getDescription().toLowerCase();

        if (lowerTitle.contains(lowerQuery)) {
            return "标题匹配";
        } else if (lowerDescription.contains(lowerQuery)) {
            return "描述匹配";
        } else if (lowerContent.contains(lowerQuery)) {
            return "内容匹配";
        } else {
            return "相关匹配";
        }
    }
}
