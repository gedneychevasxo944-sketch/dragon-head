package org.dragon.memv2.app;

import org.dragon.memv2.core.MemoryEntry;
import org.dragon.memv2.core.MemoryQuery;
import org.dragon.memv2.core.MemorySearchResult;
import org.dragon.memv2.core.MemoryRecallService;
import org.dragon.memv2.storage.repo.CharacterMemoryRepository;
import org.dragon.memv2.storage.repo.WorkspaceMemoryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 记忆检索服务实现类
 * 负责从不同作用域（角色、工作空间、会话）中检索和召回相关记忆
 *
 * @author binarytom
 * @version 1.0
 */
@Service
public class DefaultMemoryRecallService implements MemoryRecallService {
    private final CharacterMemoryRepository characterMemoryRepository;
    private final WorkspaceMemoryRepository workspaceMemoryRepository;

    public DefaultMemoryRecallService(CharacterMemoryRepository characterMemoryRepository,
                                      WorkspaceMemoryRepository workspaceMemoryRepository) {
        this.characterMemoryRepository = characterMemoryRepository;
        this.workspaceMemoryRepository = workspaceMemoryRepository;
    }

    @Override
    public List<MemorySearchResult> recallCharacter(String characterId, String query, int limit) {
        List<MemoryEntry> allEntries = characterMemoryRepository.list(characterId);
        return searchEntries(allEntries, query, limit);
    }

    @Override
    public List<MemorySearchResult> recallWorkspace(String workspaceId, String query, int limit) {
        List<MemoryEntry> allEntries = workspaceMemoryRepository.list(workspaceId);
        return searchEntries(allEntries, query, limit);
    }

    @Override
    public List<MemorySearchResult> recallComposite(MemoryQuery query) {
        List<MemorySearchResult> results = new ArrayList<>();

        if (query.getCharacterId() != null) {
            results.addAll(recallCharacter(query.getCharacterId(), query.getText(), query.getLimit()));
        }

        if (query.getWorkspaceId() != null) {
            results.addAll(recallWorkspace(query.getWorkspaceId(), query.getText(), query.getLimit()));
        }

        // 统一过滤规则
        List<MemorySearchResult> filteredResults = applyFilters(results, query);

        // 统一排序规则
        List<MemorySearchResult> sortedResults = applySorting(filteredResults);

        // 限制结果数量
        return sortedResults.stream()
                .limit(query.getLimit())
                .collect(Collectors.toList());
    }

    /**
     * 应用过滤规则
     */
    private List<MemorySearchResult> applyFilters(List<MemorySearchResult> results, MemoryQuery query) {
        // 类型过滤
        if (!query.getTypes().isEmpty()) {
            results = results.stream()
                    .filter(result -> query.getTypes().contains(result.getMemory().getType()))
                    .collect(Collectors.toList());
        }

        // 作用域过滤
        if (!query.getScopes().isEmpty()) {
            results = results.stream()
                    .filter(result -> query.getScopes().contains(result.getMemory().getScope()))
                    .collect(Collectors.toList());
        }

        return results;
    }

    /**
     * 应用排序规则
     */
    private List<MemorySearchResult> applySorting(List<MemorySearchResult> results) {
        return results.stream()
                .sorted((a, b) -> {
                    // 按分数降序排列
                    int scoreComparison = Double.compare(b.getScore(), a.getScore());
                    if (scoreComparison != 0) {
                        return scoreComparison;
                    }

                    // 分数相同按更新时间降序排列
                    return b.getMemory().getUpdatedAt().compareTo(a.getMemory().getUpdatedAt());
                })
                .collect(Collectors.toList());
    }

    /**
     * 在给定的记忆条目中搜索与查询匹配的结果
     */
    private List<MemorySearchResult> searchEntries(List<MemoryEntry> entries, String query, int limit) {
        List<MemorySearchResult> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (MemoryEntry entry : entries) {
            double score = calculateRelevanceScore(entry, lowerQuery);
            if (score > 0) {
                results.add(MemorySearchResult.builder()
                        .memory(entry)
                        .score(score)
                        .reason(getMatchReason(entry, lowerQuery))
                        .build());
            }
        }

        // 按分数排序并限制结果数量
        return results.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(limit)
                .collect(Collectors.toList());
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
