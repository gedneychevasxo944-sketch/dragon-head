package org.dragon.memv2.app;

import org.dragon.memv2.core.MemoryDedupPolicy;
import org.dragon.memv2.core.MemoryEntry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 记忆去重策略实现类
 * 负责处理记忆的去重逻辑
 *
 * @author binarytom
 * @version 1.0
 */
@Service
public class DefaultMemoryDedupPolicy implements MemoryDedupPolicy {
    private static final double SIMILARITY_THRESHOLD = 0.7;

    @Override
    public Optional<MemoryEntry> findDuplicate(MemoryEntry candidate, List<MemoryEntry> existing) {
        for (MemoryEntry entry : existing) {
            double similarity = calculateSimilarity(entry, candidate);
            if (similarity >= SIMILARITY_THRESHOLD) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    @Override
    public MemoryEntry merge(MemoryEntry existing, MemoryEntry candidate) {
        // 合并标题
        String mergedTitle = existing.getTitle();
        if (!mergedTitle.equals(candidate.getTitle())) {
            mergedTitle = existing.getTitle() + " | " + candidate.getTitle();
        }

        // 合并描述
        String mergedDescription = existing.getDescription();
        if (!mergedDescription.equals(candidate.getDescription())) {
            mergedDescription = existing.getDescription() + "\n" + candidate.getDescription();
        }

        // 合并内容
        String mergedContent = existing.getContent() + "\n\n" + candidate.getContent();

        // 使用较新的时间戳
        Instant updatedAt = existing.getUpdatedAt().isAfter(candidate.getUpdatedAt())
                ? existing.getUpdatedAt()
                : candidate.getUpdatedAt();

        // 保持类型和作用域不变
        return MemoryEntry.builder()
                .id(existing.getId())
                .title(mergedTitle)
                .description(mergedDescription)
                .content(mergedContent)
                .type(existing.getType())
                .scope(existing.getScope())
                .ownerId(existing.getOwnerId())
                .fileName(existing.getFileName())
                .filePath(existing.getFilePath())
                .createdAt(existing.getCreatedAt())
                .updatedAt(updatedAt)
                .tags(existing.getTags())
                .build();
    }

    /**
     * 计算两个记忆条目之间的相似度
     */
    private double calculateSimilarity(MemoryEntry entry1, MemoryEntry entry2) {
        double titleSimilarity = calculateStringSimilarity(entry1.getTitle(), entry2.getTitle());
        double descriptionSimilarity = calculateStringSimilarity(entry1.getDescription(), entry2.getDescription());
        double contentSimilarity = calculateStringSimilarity(entry1.getContent(), entry2.getContent());

        // 权重分配：标题占40%，描述占30%，内容占30%
        return titleSimilarity * 0.4 + descriptionSimilarity * 0.3 + contentSimilarity * 0.3;
    }

    /**
     * 计算两个字符串之间的相似度（使用Levenshtein距离）
     */
    private double calculateStringSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }

        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) {
            return 1.0;
        }

        int distance = levenshteinDistance(s1.toLowerCase(), s2.toLowerCase());
        return 1.0 - (double) distance / maxLength;
    }

    /**
     * 计算Levenshtein距离
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }

        return dp[s1.length()][s2.length()];
    }
}
