package org.dragon.memory.service.core.impl;

import org.dragon.memory.entity.MemoryEntry;
import org.dragon.memory.entity.SessionSnapshot;
import org.dragon.memory.service.core.MemoryExtractionService;
import org.dragon.memory.constants.MemoryScope;
import org.dragon.memory.constants.MemoryType;
import org.dragon.memory.service.core.MemoryRoutingPolicy;
import org.dragon.memory.entity.MemoryId;
import org.dragon.memory.service.core.MemoryValidationPolicy;
import org.dragon.memory.service.core.MemoryDedupPolicy;
import org.dragon.memory.storage.repo.CharacterMemoryRepository;
import org.dragon.memory.storage.repo.WorkspaceMemoryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 记忆提取服务实现类
 * 负责从会话记忆中提取可长期保存的记忆候选，并进行固化处理
 *
 * @author binarytom
 * @version 1.0
 */
@Service
public class MemoryExtractionServiceImpl implements MemoryExtractionService {
    private final CharacterMemoryRepository characterMemoryRepository;
    private final WorkspaceMemoryRepository workspaceMemoryRepository;
    private final MemoryRoutingPolicy memoryRoutingPolicy;
    private final MemoryValidationPolicy memoryValidationPolicy;
    private final MemoryDedupPolicy memoryDedupPolicy;

    public MemoryExtractionServiceImpl(CharacterMemoryRepository characterMemoryRepository,
                                       WorkspaceMemoryRepository workspaceMemoryRepository,
                                       MemoryRoutingPolicy memoryRoutingPolicy,
                                       MemoryValidationPolicy memoryValidationPolicy,
                                       MemoryDedupPolicy memoryDedupPolicy) {
        this.characterMemoryRepository = characterMemoryRepository;
        this.workspaceMemoryRepository = workspaceMemoryRepository;
        this.memoryRoutingPolicy = memoryRoutingPolicy;
        this.memoryValidationPolicy = memoryValidationPolicy;
        this.memoryDedupPolicy = memoryDedupPolicy;
    }

    @Override
    public List<MemoryEntry> extract(SessionSnapshot snapshot, List<String> events) {
        List<MemoryEntry> candidates = new ArrayList<>();

        // 从会话快照中提取记忆候选
        if (snapshot.getSummary() != null && !snapshot.getSummary().isEmpty()) {
            MemoryEntry entry = MemoryEntry.builder()
                    .id(generateMemoryId(snapshot.getSummary()))
                    .title("会话摘要")
                    .description(snapshot.getSummary())
                    .content(snapshot.getSummary())
                    .type(MemoryType.SESSION_SUMMARY)
                    .fileName("session_summary.md")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            candidates.add(entry);
        }

        // 从决策中提取记忆候选
        for (int i = 0; i < snapshot.getRecentDecisions().size(); i++) {
            String decision = snapshot.getRecentDecisions().get(i);
            MemoryEntry entry = MemoryEntry.builder()
                    .id(generateMemoryId(decision))
                    .title("决策记录")
                    .description(decision)
                    .content(decision)
                    .type(MemoryType.WORKSPACE_DECISION)
                    .fileName("decision_" + MemoryId.fromContent(decision).getValue() + ".md")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            candidates.add(entry);
        }

        // 从事件中提取记忆候选
        for (String event : events) {
            // 从 JSON 格式事件中提取可读内容用于关键词判断
            String eventText = extractEventText(event);
            if (isSignificantEvent(eventText)) {
                MemoryEntry entry = MemoryEntry.builder()
                        .id(generateMemoryId(event))
                        .title("重要事件")
                        .description(eventText)
                        .content(eventText)
                        .type(MemoryType.PROJECT)
                        .fileName("event_" + MemoryId.fromContent(event).getValue() + ".md")
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                candidates.add(entry);
            }
        }

        return candidates;
    }

    @Override
    public List<MemoryEntry> promote(String sessionId, SessionSnapshot snapshot, List<String> events) {
        List<MemoryEntry> candidates = extract(snapshot, events);
        List<MemoryEntry> promotedEntries = new ArrayList<>();

        for (MemoryEntry candidate : candidates) {
            // 1. 校验记忆条目
            MemoryValidationPolicy.ValidationResult validationResult = memoryValidationPolicy.validate(candidate);
            if (!validationResult.isValid()) {
                continue; // 跳过无效的记忆条目
            }

            // 2. 确定记忆作用域
            MemoryScope scope = memoryRoutingPolicy.route(candidate, snapshot);
            candidate.setScope(scope);
            candidate.setOwnerId(scope == MemoryScope.WORKSPACE ? snapshot.getWorkspaceId() : snapshot.getCharacterId());

            // 3. 检查是否应该提升到长期记忆
            if (!memoryRoutingPolicy.shouldPromote(candidate, snapshot)) {
                continue; // 跳过不应提升的记忆条目
            }

            // 4. 去重逻辑
            List<MemoryEntry> existingMemories = getExistingMemories(snapshot, scope);
            Optional<MemoryEntry> duplicateEntry = memoryDedupPolicy.findDuplicate(candidate, existingMemories);
            if (duplicateEntry.isPresent()) {
                // 如果找到重复条目，合并后更新
                MemoryEntry mergedEntry = memoryDedupPolicy.merge(duplicateEntry.get(), candidate);
                if (scope == MemoryScope.CHARACTER && snapshot.getCharacterId() != null) {
                    characterMemoryRepository.update(snapshot.getCharacterId(), mergedEntry);
                    promotedEntries.add(mergedEntry);
                } else if (scope == MemoryScope.WORKSPACE && snapshot.getWorkspaceId() != null) {
                    workspaceMemoryRepository.update(snapshot.getWorkspaceId(), mergedEntry);
                    promotedEntries.add(mergedEntry);
                }
            } else {
                // 如果没有找到重复条目，创建新条目
                if (scope == MemoryScope.CHARACTER && snapshot.getCharacterId() != null) {
                    promotedEntries.add(characterMemoryRepository.create(snapshot.getCharacterId(), candidate));
                } else if (scope == MemoryScope.WORKSPACE && snapshot.getWorkspaceId() != null) {
                    promotedEntries.add(workspaceMemoryRepository.create(snapshot.getWorkspaceId(), candidate));
                }
            }
        }

        return promotedEntries;
    }

    /**
     * 获取已存在的记忆列表
     */
    private List<MemoryEntry> getExistingMemories(SessionSnapshot snapshot, MemoryScope scope) {
        if (scope == MemoryScope.CHARACTER && snapshot.getCharacterId() != null) {
            return characterMemoryRepository.list(snapshot.getCharacterId());
        } else if (scope == MemoryScope.WORKSPACE && snapshot.getWorkspaceId() != null) {
            return workspaceMemoryRepository.list(snapshot.getWorkspaceId());
        }
        return List.of();
    }

    /**
     * 生成记忆ID
     */
    private MemoryId generateMemoryId(String content) {
        return MemoryId.fromContent(content);
    }


    /**
     * 从事件字符串中提取可读文本。
     * appendEntry() 写入的事件为 JSON 格式：{"type":"...","title":"...","content":"..."}
     * 普通字符串事件直接返回原文。
     */
    private String extractEventText(String event) {
        if (event == null || !event.trim().startsWith("{")) {
            return event;
        }
        // 简单提取 content 字段值，避免引入 JSON 库依赖
        String text = extractJsonField(event, "content");
        if (text == null || text.isBlank()) {
            text = extractJsonField(event, "title");
        }
        return text != null ? text : event;
    }

    /**
     * 从 JSON 字符串中粗提取指定字段的字符串值（仅用于单层简单 JSON）
     */
    private String extractJsonField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start < 0) {
            return null;
        }
        start += key.length();
        int end = json.indexOf("\"", start);
        if (end < 0) {
            return null;
        }
        return json.substring(start, end).replace("\\\"", "\"");
    }

    /**
     * 判断事件文本是否重要（作用于可读文本，非原始 JSON）
     */
    private boolean isSignificantEvent(String eventText) {
        if (eventText == null || eventText.isBlank()) {
            return false;
        }
        String lower = eventText.toLowerCase();
        return lower.contains("决定") || lower.contains("需要") || lower.contains("必须") ||
                lower.contains("建议") || lower.contains("计划") || lower.contains("目标");
    }
}