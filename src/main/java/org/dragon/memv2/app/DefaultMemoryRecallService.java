package org.dragon.memv2.app;

import org.dragon.memv2.core.MemoryEntry;
import org.dragon.memv2.core.MemoryQuery;
import org.dragon.memv2.core.MemorySearchResult;
import org.dragon.memv2.core.MemoryRecallService;
import org.dragon.memv2.core.MemoryRanker;
import org.dragon.memv2.core.SessionSnapshot;
import org.dragon.memv2.core.MemoryId;
import org.dragon.memv2.core.MemoryType;
import org.dragon.memv2.core.MemoryScope;
import org.dragon.memv2.storage.repo.CharacterMemoryRepository;
import org.dragon.memv2.storage.repo.WorkspaceMemoryRepository;
import org.dragon.memv2.storage.repo.SessionMemoryRepository;
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
    private final SessionMemoryRepository sessionMemoryRepository;
    private final MemoryRanker memoryRanker;

    public DefaultMemoryRecallService(CharacterMemoryRepository characterMemoryRepository,
                                      WorkspaceMemoryRepository workspaceMemoryRepository,
                                      SessionMemoryRepository sessionMemoryRepository,
                                      MemoryRanker memoryRanker) {
        this.characterMemoryRepository = characterMemoryRepository;
        this.workspaceMemoryRepository = workspaceMemoryRepository;
        this.sessionMemoryRepository = sessionMemoryRepository;
        this.memoryRanker = memoryRanker;
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
    public List<MemorySearchResult> recallSession(String sessionId, String query, int limit) {
        Optional<SessionSnapshot> snapshotOpt = sessionMemoryRepository.get(sessionId);
        if (snapshotOpt.isPresent()) {
            SessionSnapshot snapshot = snapshotOpt.get();
            // 这里可以根据需要将 SessionSnapshot 转换为 MemoryEntry 进行搜索
            // 目前简单实现：如果会话内容包含查询词，则返回该会话的记忆结果
            List<MemoryEntry> sessionEntries = new ArrayList<>();
            MemoryEntry entry = MemoryEntry.builder()
                    .id(MemoryId.generate())
                    .title("会话摘要")
                    .description(snapshot.getSummary())
                    .content(snapshot.getContent())
                    .type(MemoryType.SESSION_SUMMARY)
                    .scope(MemoryScope.SESSION)
                    .ownerId(sessionId)
                    .updatedAt(snapshot.getUpdatedAt())
                    .build();
            sessionEntries.add(entry);
            return searchEntries(sessionEntries, query, limit);
        }
        return new ArrayList<>();
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

        if (query.getSessionId() != null) {
            results.addAll(recallSession(query.getSessionId(), query.getText(), query.getLimit()));
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
        return memoryRanker.rank(query, entries, limit);
    }
}
