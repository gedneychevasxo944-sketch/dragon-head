package org.dragon.memory.service.core.impl;

import org.dragon.agent.llm.LLMRequest;
import org.dragon.agent.llm.LLMResponse;
import org.dragon.agent.llm.caller.LLMCaller;
import org.dragon.agent.llm.caller.LLMCallerSelector;
import org.dragon.agent.model.ModelInstance;
import org.dragon.config.PromptKeys;
import org.dragon.config.service.ConfigApplication;
import org.dragon.memory.entity.MemoryEntry;
import org.dragon.memory.entity.MemoryQuery;
import org.dragon.memory.entity.MemorySearchResult;
import org.dragon.memory.service.core.MemoryRecallService;
import org.dragon.memory.entity.SessionSnapshot;
import org.dragon.memory.entity.MemoryId;
import org.dragon.memory.constants.MemoryType;
import org.dragon.memory.constants.MemoryScope;
import org.dragon.memory.storage.repo.CharacterMemoryRepository;
import org.dragon.memory.storage.repo.WorkspaceMemoryRepository;
import org.dragon.memory.storage.repo.SessionMemoryRepository;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 记忆检索服务实现类
 * 负责从不同作用域（角色、工作空间、会话）中检索和召回相关记忆
 *
 * @author binarytom
 * @version 1.0
 */
@Slf4j
@Service
public class MemoryRecallServiceImpl implements MemoryRecallService {
    private final CharacterMemoryRepository characterMemoryRepository;
    private final WorkspaceMemoryRepository workspaceMemoryRepository;
    private final SessionMemoryRepository sessionMemoryRepository;
    private final LLMCallerSelector llmCallerSelector;
    private final ConfigApplication configApplication;

    public MemoryRecallServiceImpl(CharacterMemoryRepository characterMemoryRepository,
                                   WorkspaceMemoryRepository workspaceMemoryRepository,
                                   SessionMemoryRepository sessionMemoryRepository,
                                   LLMCallerSelector llmCallerSelector,
                                   ConfigApplication configApplication) {
        this.characterMemoryRepository = characterMemoryRepository;
        this.workspaceMemoryRepository = workspaceMemoryRepository;
        this.sessionMemoryRepository = sessionMemoryRepository;
        this.llmCallerSelector = llmCallerSelector;
        this.configApplication = configApplication;
    }

    @Override
    public List<MemorySearchResult> recallCharacter(String characterId, String query, int limit) {
        log.info("[MemoryRecallServiceImpl] Recalling character memories: {}", characterId);
        List<MemoryEntry> allEntries = characterMemoryRepository.list(characterId);
        return searchRelevantEntries(allEntries, query, limit);
    }

    @Override
    public List<MemorySearchResult> recallWorkspace(String workspaceId, String query, int limit) {
        log.info("[MemoryRecallServiceImpl] Recalling workspace memories: {}", workspaceId);
        List<MemoryEntry> allEntries = workspaceMemoryRepository.list(workspaceId);
        return searchRelevantEntries(allEntries, query, limit);
    }

    @Override
    public List<MemorySearchResult> recallSession(String sessionId, String query, int limit) {
        log.info("[MemoryRecallServiceImpl] Recalling session memories: {}", sessionId);
        Optional<SessionSnapshot> snapshotOpt = sessionMemoryRepository.get(sessionId);
        if (snapshotOpt.isEmpty()) {
            return new ArrayList<>();
        }
        SessionSnapshot snapshot = snapshotOpt.get();
        List<MemoryEntry> sessionEntries = collectSessionEntries(sessionId, snapshot);
        return searchRelevantEntries(sessionEntries, query, limit);
    }

    @Override
    public List<MemorySearchResult> recallComposite(MemoryQuery query) {
        log.info("[MemoryRecallServiceImpl] Recalling composite memories: workspaceId={}, characterId={}, sessionId={}", query.getWorkspaceId(), query.getCharacterId(), query.getSessionId());
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
        log.info("[MemoryRecallServiceImpl] Total entries collected: {}", allEntries.size());

        // 4. LLM 筛选后应用过滤规则
        List<MemorySearchResult> results = searchRelevantEntries(allEntries, query.getText(), query.getLimit());
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
     *
     * <p>query 非空时调用 LLM（SELECT_MEMORIES_SYSTEM_PROMPT）筛选相关条目，以模型返回结果为准。
     * LLM 调用失败时降级为全量返回（截取到 limit）。query 为空时直接全量返回。
     *
     * @param entries 候选记忆条目
     * @param query   查询文本
     * @param limit   返回数量上限
     * @return LLM 选中的结果列表
     */
    private List<MemorySearchResult> searchRelevantEntries(List<MemoryEntry> entries, String query, int limit) {
        if (entries.isEmpty()) {
            return List.of();
        }

        if (query == null || query.isBlank()) {
            log.info("[MemoryRecallServiceImpl] Query is blank, cutting to limit: {}", limit);
            // 无查询词时全量返回，截取到 limit
            return toResults(limit > 0 ? entries.stream().limit(limit).collect(Collectors.toList()) : entries);
        }

        List<MemoryEntry> llmSelected = selectByLLM(entries, query);
        if (llmSelected == null) {
            // LLM 调用异常，降级为全量截取
            log.warn("[MemoryRecallServiceImpl] LLM 筛选失败，降级为全量返回");
            return toResults(limit > 0 ? entries.stream().limit(limit).collect(Collectors.toList()) : entries);
        }

        List<MemoryEntry> limited = (limit > 0 && llmSelected.size() > limit)
                ? llmSelected.subList(0, limit)
                : llmSelected;
        return toResults(limited);
    }

    /**
     * 将 MemoryEntry 列表直接包装为 MemorySearchResult，不附加评分
     */
    private List<MemorySearchResult> toResults(List<MemoryEntry> entries) {
        return entries.stream()
                .map(e -> MemorySearchResult.builder().memory(e).build())
                .collect(Collectors.toList());
    }

    /**
     * 调用 LLM 从候选条目中筛选与 query 相关的记忆
     *
     * <p>User message 格式：
     * <pre>
     * User query: {query}
     *
     * Available memory files:
     * 1. filename: {fileName}  description: {description}
     * 2. ...
     * </pre>
     *
     * LLM 应返回一个 filename 列表（每行一个，或逗号分隔）。
     * 返回 null 表示 LLM 调用失败，调用方应降级处理。
     *
     * @param candidates 候选条目
     * @param query      用户查询文本
     * @return LLM 筛选后的条目子集，或 null（调用失败）
     */
    private List<MemoryEntry> selectByLLM(List<MemoryEntry> candidates, String query) {
        // 构建候选列表描述，每条记忆以 fileName（或 title 作为后备标识）呈现
        StringBuilder userMessage = new StringBuilder();
        userMessage.append("User query: ").append(query).append("\n\n");
        userMessage.append("Available memory files:\n");

        for (int i = 0; i < candidates.size(); i++) {
            MemoryEntry entry = candidates.get(i);
            // fileName 优先作为唯一标识；无 fileName 时用 title 代替
            String identifier = (entry.getFileName() != null && !entry.getFileName().isBlank())
                    ? entry.getFileName()
                    : entry.getTitle();
            String description = entry.getDescription() != null ? entry.getDescription() : entry.getTitle();
            userMessage.append(i + 1).append(". filename: ").append(identifier)
                    .append("  description: ").append(description).append("\n");
        }

        String systemPrompt = configApplication.getGlobalPrompt(PromptKeys.MEMORY_RECALL_SELECT, null);
        LLMRequest request = LLMRequest.builder()
                .systemPrompt(systemPrompt)
                .messages(List.of(
                        LLMRequest.LLMMessage.builder()
                                .role(LLMRequest.LLMMessage.Role.USER)
                                .content(userMessage.toString())
                                .build()))
                .build();

        try {
//            LLMCaller llmCaller = llmCallerSelector.getDefault();
            LLMCaller llmCaller = llmCallerSelector.selectByProvider(ModelInstance.ModelProvider.MINIMAX);
            log.info("[MemoryRecallServiceImpl] 调用 LLM 筛选记忆，模型: {}", llmCaller.getClass());
            LLMResponse response = llmCaller.call(request);
            String content = response != null ? response.getContent() : null;
            if (content == null || content.isBlank()) {
                return List.of();
            }
            return filterByLLMResponse(candidates, content);
        } catch (Exception e) {
            log.warn("[MemoryRecallServiceImpl] LLM 记忆筛选调用异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 根据 LLM 返回的 filename 列表过滤候选条目
     *
     * <p>LLM 可能以换行、逗号、序号等多种格式返回 filename，这里做宽松匹配：
     * 只要候选条目的 identifier（fileName 或 title）出现在响应文本中即保留。
     *
     * @param candidates  候选条目
     * @param llmResponse LLM 返回的原始文本
     * @return 被 LLM 选中的条目子集
     */
    private List<MemoryEntry> filterByLLMResponse(List<MemoryEntry> candidates, String llmResponse) {
        // 将响应文本按常见分隔符拆分，构建命中集合
        Set<String> mentioned = new HashSet<>();
        for (String token : llmResponse.split("[,\n\\[\\]\"'\\s]+")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                mentioned.add(trimmed.toLowerCase());
            }
        }
        log.info("[MemoryRecallServiceImpl] LLM 筛选记忆，命中列表: {}", mentioned);

        return candidates.stream()
                .filter(entry -> {
                    String identifier = (entry.getFileName() != null && !entry.getFileName().isBlank())
                            ? entry.getFileName()
                            : entry.getTitle();
                    if (identifier == null) {
                        return false;
                    }
                    // 宽松匹配：identifier 出现在任意 token 中，或任意 token 是 identifier 的子串
                    String lowerIdentifier = identifier.toLowerCase();
                    return mentioned.stream().anyMatch(token ->
                            token.contains(lowerIdentifier) || lowerIdentifier.contains(token));
                })
                .collect(Collectors.toList());
    }

}