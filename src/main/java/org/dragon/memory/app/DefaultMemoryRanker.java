package org.dragon.memory.app;

import org.dragon.memory.core.MemoryEntry;
import org.dragon.memory.core.MemoryRanker;
import org.dragon.memory.core.MemorySearchResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
        // query 为空时视为全量返回，不做文本过滤
        boolean emptyQuery = query == null || query.isBlank();
        String lowerQuery = emptyQuery ? "" : query.toLowerCase();

        for (MemoryEntry entry : candidates) {
            double score;
            String reason;
            if (emptyQuery) {
                // 无查询词时所有条目以默认分数入选
                score = 0.5;
                reason = "全量召回";
            } else {
                score = calculateRelevanceScore(entry, lowerQuery);
                if (score == 0) {
                    continue;
                }
                reason = getMatchReason(entry, lowerQuery);
            }
            results.add(MemorySearchResult.builder()
                    .memory(entry)
                    .score(score)
                    .reason(reason)
                    .build());
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

        String title = entry.getTitle();
        String description = entry.getDescription();
        String content = entry.getContent();

        // 标题匹配权重最高
        if (title != null && title.toLowerCase().contains(lowerQuery)) {
            score += 0.6;
        }

        // 描述匹配权重次之
        if (description != null && description.toLowerCase().contains(lowerQuery)) {
            score += 0.3;
        }

        // 内容匹配权重最低
        if (content != null && content.toLowerCase().contains(lowerQuery)) {
            score += 0.1;
        }

        return score;
    }

    /**
     * 获取匹配原因
     */
    private String getMatchReason(MemoryEntry entry, String lowerQuery) {
        String title = entry.getTitle();
        String description = entry.getDescription();
        String content = entry.getContent();

        if (title != null && title.toLowerCase().contains(lowerQuery)) {
            return "标题匹配";
        } else if (description != null && description.toLowerCase().contains(lowerQuery)) {
            return "描述匹配";
        } else if (content != null && content.toLowerCase().contains(lowerQuery)) {
            return "内容匹配";
        } else {
            return "相关匹配";
        }
    }
}