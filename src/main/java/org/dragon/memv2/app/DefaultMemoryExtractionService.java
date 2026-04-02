package org.dragon.memv2.app;

import org.dragon.memv2.core.MemoryEntry;
import org.dragon.memv2.core.SessionSnapshot;
import org.dragon.memv2.core.MemoryExtractionService;
import org.dragon.memv2.core.MemoryScope;
import org.dragon.memv2.core.MemoryType;
import org.dragon.memv2.core.MemoryRoutingPolicy;
import org.dragon.memv2.core.MemoryValidationPolicy;
import org.dragon.memv2.core.MemoryDedupPolicy;
import org.dragon.memv2.storage.repo.CharacterMemoryRepository;
import org.dragon.memv2.storage.repo.WorkspaceMemoryRepository;
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
public class DefaultMemoryExtractionService implements MemoryExtractionService {
    private final CharacterMemoryRepository characterMemoryRepository;
    private final WorkspaceMemoryRepository workspaceMemoryRepository;
    private final MemoryRoutingPolicy memoryRoutingPolicy;
    private final MemoryValidationPolicy memoryValidationPolicy;
    private final MemoryDedupPolicy memoryDedupPolicy;

    public DefaultMemoryExtractionService(CharacterMemoryRepository characterMemoryRepository,
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
        for (String decision : snapshot.getRecentDecisions()) {
            MemoryEntry entry = MemoryEntry.builder()
                    .id(generateMemoryId(decision))
                    .title("决策记录")
                    .description(decision)
                    .content(decision)
                    .type(MemoryType.WORKSPACE_DECISION)
                    .fileName("decision_" + System.currentTimeMillis() + ".md")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            candidates.add(entry);
        }

        // 从事件中提取记忆候选
        for (String event : events) {
            if (isSignificantEvent(event)) {
                MemoryEntry entry = MemoryEntry.builder()
                        .id(generateMemoryId(event))
                        .title("重要事件")
                        .description(event)
                        .content(event)
                        .type(MemoryType.PROJECT)
                        .fileName("event_" + System.currentTimeMillis() + ".md")
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
    private String generateMemoryId(String content) {
        return content.toLowerCase().replaceAll("[^a-zA-Z0-9_]", "_").substring(0, Math.min(content.length(), 30));
    }


    /**
     * 判断事件是否重要
     */
    private boolean isSignificantEvent(String event) {
        String lowerEvent = event.toLowerCase();
        return lowerEvent.contains("决定") || lowerEvent.contains("需要") || lowerEvent.contains("必须") ||
                lowerEvent.contains("建议") || lowerEvent.contains("计划") || lowerEvent.contains("目标");
    }
}
