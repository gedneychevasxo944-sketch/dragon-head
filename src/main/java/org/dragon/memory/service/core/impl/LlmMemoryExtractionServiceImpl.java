package org.dragon.memory.service.core.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.dragon.agent.llm.LLMRequest;
import org.dragon.agent.llm.LLMResponse;
import org.dragon.agent.llm.caller.LLMCaller;
import org.dragon.agent.llm.caller.LLMCallerSelector;
import org.dragon.config.PromptKeys;
import org.dragon.config.service.ConfigApplication;
import org.dragon.memory.constants.MemoryScope;
import org.dragon.memory.constants.MemoryType;
import org.dragon.memory.entity.MemoryEntry;
import org.dragon.memory.entity.MemoryId;
import org.dragon.memory.entity.SessionSnapshot;
import org.dragon.memory.service.core.MemoryExtractionService;
import org.dragon.memory.service.core.MemoryValidationPolicy;
import org.dragon.memory.storage.repo.CharacterMemoryRepository;
import org.dragon.memory.storage.repo.WorkspaceMemoryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 基于 LLM 的记忆提取服务实现
 * 通过 LLMCaller 调用大模型，从会话快照与事件流中识别值得长期保存的记忆候选，
 * 校验通过后按 MemoryType 路由到 CHARACTER / WORKSPACE 作用域并入库
 *
 * @author binarytom
 * @version 1.0
 */
@Slf4j
@Service
public class LlmMemoryExtractionServiceImpl implements MemoryExtractionService {

    private final CharacterMemoryRepository characterMemoryRepository;
    private final WorkspaceMemoryRepository workspaceMemoryRepository;
    private final MemoryValidationPolicy memoryValidationPolicy;
    private final LLMCallerSelector llmCallerSelector;
    private final ConfigApplication configApplication;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlmMemoryExtractionServiceImpl(CharacterMemoryRepository characterMemoryRepository,
                                          WorkspaceMemoryRepository workspaceMemoryRepository,
                                          MemoryValidationPolicy memoryValidationPolicy,
                                          LLMCallerSelector llmCallerSelector,
                                          ConfigApplication configApplication) {
        this.characterMemoryRepository = characterMemoryRepository;
        this.workspaceMemoryRepository = workspaceMemoryRepository;
        this.memoryValidationPolicy = memoryValidationPolicy;
        this.llmCallerSelector = llmCallerSelector;
        this.configApplication = configApplication;
    }

    @Override
    public List<MemoryEntry> extract(SessionSnapshot snapshot, List<String> events) {
        if (snapshot == null) {
            return List.of();
        }

        String systemPrompt = configApplication.getGlobalPrompt(PromptKeys.MEMORY_EXTRACT, null);
        if (systemPrompt == null || systemPrompt.isBlank()) {
            log.warn("[LlmMemoryExtractionService] memory.extract prompt 未配置，返回空候选");
            return List.of();
        }

        String userMessage = buildUserMessage(snapshot, events);
        LLMRequest request = LLMRequest.builder()
                .systemPrompt(systemPrompt)
                .messages(List.of(
                        LLMRequest.LLMMessage.builder()
                                .role(LLMRequest.LLMMessage.Role.USER)
                                .content(userMessage)
                                .build()))
                .build();

        try {
            LLMCaller llmCaller = llmCallerSelector.getDefault();
            log.info("[LlmMemoryExtractionService] 调用 LLM 提取记忆候选，模型: {}", llmCaller.getClass().getSimpleName());
            LLMResponse response = llmCaller.call(request);
            String content = response != null ? response.getContent() : null;
            if (content == null || content.isBlank()) {
                log.warn("[LlmMemoryExtractionService] LLM 返回内容为空");
                return List.of();
            }
            return parseCandidates(content);
        } catch (Exception e) {
            log.warn("[LlmMemoryExtractionService] LLM 提取记忆候选失败: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public List<MemoryEntry> promote(String sessionId, SessionSnapshot snapshot, List<String> events) {
        List<MemoryEntry> candidates = extract(snapshot, events);
        List<MemoryEntry> promotedEntries = new ArrayList<>();

        for (MemoryEntry candidate : candidates) {
            // 1. 校验
            MemoryValidationPolicy.ValidationResult validationResult = memoryValidationPolicy.validate(candidate);
            if (!validationResult.isValid()) {
                log.debug("[LlmMemoryExtractionService] 候选记忆校验未通过，已跳过: {}", validationResult.getErrorMessage());
                continue;
            }

            // 2. 路由作用域；SESSION 类型不提升到长期记忆
            MemoryScope scope = routeScope(candidate.getType());
            if (scope == MemoryScope.SESSION) {
                continue;
            }
            candidate.setScope(scope);
            candidate.setOwnerId(scope == MemoryScope.WORKSPACE ? snapshot.getWorkspaceId() : snapshot.getCharacterId());

            // 3. 入库
            if (scope == MemoryScope.CHARACTER && snapshot.getCharacterId() != null) {
                promotedEntries.add(characterMemoryRepository.create(snapshot.getCharacterId(), candidate));
            } else if (scope == MemoryScope.WORKSPACE && snapshot.getWorkspaceId() != null) {
                promotedEntries.add(workspaceMemoryRepository.create(snapshot.getWorkspaceId(), candidate));
            }
        }

        return promotedEntries;
    }

    /**
     * 按 MemoryType 路由作用域
     * CHARACTER_PROFILE / FEEDBACK → CHARACTER
     * PROJECT / REFERENCE / WORKSPACE_DECISION → WORKSPACE
     * SESSION_SUMMARY → SESSION（不提升）
     */
    public static MemoryScope routeScope(MemoryType type) {
        if (type == null) {
            return MemoryScope.SESSION;
        }
        return switch (type) {
            case CHARACTER_PROFILE, FEEDBACK -> MemoryScope.CHARACTER;
            case PROJECT, REFERENCE, WORKSPACE_DECISION -> MemoryScope.WORKSPACE;
            case SESSION_SUMMARY -> MemoryScope.SESSION;
        };
    }

    /**
     * 构造 LLM 用户消息：把 snapshot 和 events 序列化为结构化上下文
     */
    private String buildUserMessage(SessionSnapshot snapshot, List<String> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Session Snapshot\n");
        sb.append("sessionId: ").append(nullable(snapshot.getSessionId())).append('\n');
        sb.append("workspaceId: ").append(nullable(snapshot.getWorkspaceId())).append('\n');
        sb.append("characterId: ").append(nullable(snapshot.getCharacterId())).append('\n');
        sb.append("summary: ").append(nullable(snapshot.getSummary())).append('\n');
        sb.append("currentGoal: ").append(nullable(snapshot.getCurrentGoal())).append('\n');

        sb.append("recentDecisions:\n");
        if (snapshot.getRecentDecisions() != null) {
            for (String d : snapshot.getRecentDecisions()) {
                sb.append("  - ").append(d).append('\n');
            }
        }

        sb.append("unresolvedQuestions:\n");
        if (snapshot.getUnresolvedQuestions() != null) {
            for (String q : snapshot.getUnresolvedQuestions()) {
                sb.append("  - ").append(q).append('\n');
            }
        }

        sb.append("\n## Events\n");
        if (events != null) {
            for (int i = 0; i < events.size(); i++) {
                sb.append(i + 1).append(". ").append(events.get(i)).append('\n');
            }
        }

        sb.append("\n请严格按 system prompt 约束输出 JSON。");
        return sb.toString();
    }

    private String nullable(String v) {
        return v == null ? "" : v;
    }

    /**
     * 解析 LLM 返回的 JSON，提取候选条目
     * 兼容 LLM 带 Markdown 代码块包裹或前后含解释文字的情况
     */
    private List<MemoryEntry> parseCandidates(String llmContent) {
        String json = stripToJsonObject(llmContent);
        if (json == null) {
            log.warn("[LlmMemoryExtractionService] 无法从 LLM 返回中提取 JSON: {}", truncate(llmContent));
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode arr = root.get("candidates");
            if (arr == null || !arr.isArray()) {
                return List.of();
            }

            Instant now = Instant.now();
            List<MemoryEntry> result = new ArrayList<>();
            for (JsonNode node : arr) {
                MemoryEntry entry = toMemoryEntry(node, now);
                if (entry != null) {
                    result.add(entry);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("[LlmMemoryExtractionService] 解析 LLM JSON 失败: {}", e.getMessage());
            return List.of();
        }
    }

    private MemoryEntry toMemoryEntry(JsonNode node, Instant now) {
        String title = textOrNull(node, "title");
        String description = textOrNull(node, "description");
        String content = textOrNull(node, "content");
        String typeText = textOrNull(node, "type");

        // content 为空则此条记忆无保存价值
        if (content == null || content.isBlank()) {
            return null;
        }

        MemoryType type = parseType(typeText);
        if (title == null || title.isBlank()) {
            title = type.name();
        }

        return MemoryEntry.builder()
                .id(MemoryId.fromContent(content))
                .title(title)
                .description(description != null ? description : title)
                .content(content)
                .type(type)
                .fileName(type.name().toLowerCase(Locale.ROOT) + "_" + MemoryId.fromContent(content).getValue() + ".md")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private MemoryType parseType(String typeText) {
        if (typeText == null || typeText.isBlank()) {
            return MemoryType.SESSION_SUMMARY;
        }
        try {
            return MemoryType.valueOf(typeText.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return MemoryType.SESSION_SUMMARY;
        }
    }

    /**
     * 从 LLM 返回文本中截取最外层 JSON 对象
     */
    private String stripToJsonObject(String text) {
        if (text == null) {
            return null;
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return text.substring(start, end + 1);
    }

    private String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }

}
