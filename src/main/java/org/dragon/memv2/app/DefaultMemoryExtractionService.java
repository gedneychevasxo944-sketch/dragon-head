package org.dragon.memv2.app;

import org.dragon.memv2.core.MemoryEntry;
import org.dragon.memv2.core.SessionSnapshot;
import org.dragon.memv2.core.MemoryExtractionService;
import org.dragon.memv2.core.MemoryScope;
import org.dragon.memv2.core.MemoryType;
import org.dragon.memv2.storage.repo.CharacterMemoryRepository;
import org.dragon.memv2.storage.repo.WorkspaceMemoryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    public DefaultMemoryExtractionService(CharacterMemoryRepository characterMemoryRepository,
                                         WorkspaceMemoryRepository workspaceMemoryRepository) {
        this.characterMemoryRepository = characterMemoryRepository;
        this.workspaceMemoryRepository = workspaceMemoryRepository;
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
                    .scope(determineScope(snapshot))
                    .ownerId(determineOwnerId(snapshot))
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
                    .scope(determineScope(snapshot))
                    .ownerId(determineOwnerId(snapshot))
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
                        .scope(determineScope(snapshot))
                        .ownerId(determineOwnerId(snapshot))
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
            if (candidate.getScope() == MemoryScope.CHARACTER && snapshot.getCharacterId() != null) {
                promotedEntries.add(characterMemoryRepository.create(snapshot.getCharacterId(), candidate));
            } else if (candidate.getScope() == MemoryScope.WORKSPACE && snapshot.getWorkspaceId() != null) {
                promotedEntries.add(workspaceMemoryRepository.create(snapshot.getWorkspaceId(), candidate));
            }
        }

        return promotedEntries;
    }

    /**
     * 生成记忆ID
     */
    private String generateMemoryId(String content) {
        return content.toLowerCase().replaceAll("[^a-zA-Z0-9_]", "_").substring(0, Math.min(content.length(), 30));
    }

    /**
     * 确定记忆作用域
     */
    private MemoryScope determineScope(SessionSnapshot snapshot) {
        // 如果有工作空间ID，则默认存储在工作空间
        if (snapshot.getWorkspaceId() != null) {
            return MemoryScope.WORKSPACE;
        }
        return MemoryScope.CHARACTER;
    }

    /**
     * 确定记忆所有者ID
     */
    private String determineOwnerId(SessionSnapshot snapshot) {
        if (snapshot.getWorkspaceId() != null) {
            return snapshot.getWorkspaceId();
        }
        return snapshot.getCharacterId();
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
