package org.dragon.memory.service.core.impl;

import org.dragon.memory.entity.MemoryEntry;
import org.dragon.memory.entity.MemoryQuery;
import org.dragon.memory.entity.MemorySearchResult;
import org.dragon.memory.service.core.MemoryRecallService;
import org.dragon.memory.service.core.MemoryRanker;
import org.dragon.memory.entity.SessionSnapshot;
import org.dragon.memory.entity.MemoryId;
import org.dragon.memory.constants.MemoryType;
import org.dragon.memory.constants.MemoryScope;
import org.dragon.memory.storage.repo.CharacterMemoryRepository;
import org.dragon.memory.storage.repo.WorkspaceMemoryRepository;
import org.dragon.memory.storage.repo.SessionMemoryRepository;
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
public class MemoryRecallServiceImpl implements MemoryRecallService {
    private final CharacterMemoryRepository characterMemoryRepository;
    private final WorkspaceMemoryRepository workspaceMemoryRepository;
    private final SessionMemoryRepository sessionMemoryRepository;
    private final MemoryRanker memoryRanker;

    public MemoryRecallServiceImpl(CharacterMemoryRepository characterMemoryRepository,
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
        if (snapshotOpt.isEmpty()) {
            return new ArrayList<>();
        }
        SessionSnapshot snapshot = snapshotOpt.get();
        List<MemoryEntry> sessionEntries = collectSessionEntries(sessionId, snapshot);
        return searchEntries(sessionEntries, query, limit);
    }

    @Override
    public List<MemorySearchResult> recallComposite(MemoryQuery query) {
        List<MemoryEntry> allEntries = new ArrayList<>();

        // 1. 先拿 session summary（最近上下文优先）
        if (query.getSessionId() != null) {
            Optional<SessionSnapshot> snapshotOpt = sessionMemoryRepository.get(query.getSessionId());
            snapshotOpt.ifPresent(snapshot ->
                    allEntries.addAll(collectSessionEntries(query.getSessionId(), snapshot)));
        }

        // 2. 再召回 character memory
        if (query.getCharacterId() != null) {
            allEntries.addAll(characterMemoryRepository.list(query.getCharacterId()));
        }

        // 3. 再召回 workspace memory
        if (query.getWorkspaceId() != null) {
            allEntries.addAll(workspaceMemoryRepository.list(query.getWorkspaceId()));
        }

        // 4. 合并排序控量
        List<MemorySearchResult> results = memoryRanker.rank(query.getText(), allEntries, query.getLimit());
        return applyFilters(results, query);
    }

    /**
     * 将 SessionSnapshot 拆解为多条结构化 MemoryEntry
     */
    private List<MemoryEntry> collectSessionEntries(String sessionId, SessionSnapshot snapshot) {
        List<MemoryEntry> entries = new ArrayList<>();

        // 会话摘要
        if (snapshot.getSummary() != null && !snapshot.getSummary().isBlank()) {
            entries.add(MemoryEntry.builder()
                    .id(MemoryId.generate())
                    .title("会话摘要")
                    .content(snapshot.getSummary())
                    .type(MemoryType.SESSION_SUMMARY)
                    .scope(MemoryScope.SESSION)
                    .ownerId(sessionId)
                    .updatedAt(snapshot.getUpdatedAt())
                    .build());
        }

        // 最近决策
        if (snapshot.getRecentDecisions() != null) {
            for (String decision : snapshot.getRecentDecisions()) {
                entries.add(MemoryEntry.builder()
                        .id(MemoryId.generate())
                        .title("最近决策")
                        .content(decision)
                        .type(MemoryType.WORKSPACE_DECISION)
                        .scope(MemoryScope.SESSION)
                        .ownerId(sessionId)
                        .updatedAt(snapshot.getUpdatedAt())
                        .build());
            }
        }

        // 未解决问题
        if (snapshot.getUnresolvedQuestions() != null) {
            for (String question : snapshot.getUnresolvedQuestions()) {
                entries.add(MemoryEntry.builder()
                        .id(MemoryId.generate())
                        .title("未解决问题")
                        .content(question)
                        .type(MemoryType.SESSION_SUMMARY)
                        .scope(MemoryScope.SESSION)
                        .ownerId(sessionId)
                        .updatedAt(snapshot.getUpdatedAt())
                        .build());
            }
        }

        return entries;
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
     * 在给定的记忆条目中搜索与查询匹配的结果
     */
    private List<MemorySearchResult> searchEntries(List<MemoryEntry> entries, String query, int limit) {
        return memoryRanker.rank(query, entries, limit);
    }
}