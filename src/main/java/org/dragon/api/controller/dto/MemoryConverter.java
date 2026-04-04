package org.dragon.api.controller.dto;

import org.dragon.memory.core.MemoryEntry;
import org.dragon.memory.core.MemoryQuery;
import org.dragon.memory.core.MemoryScope;
import org.dragon.memory.core.MemorySearchResult;
import org.dragon.memory.core.MemoryType;
import org.dragon.memory.core.SessionSnapshot;
import org.dragon.memory.core.MemoryId;

import java.util.stream.Collectors;

/**
 * 记忆模块 DTO 转换器
 * 用于在领域对象和 DTO 之间进行转换
 *
 * @author binarytom
 * @version 1.0
 */
public class MemoryConverter {

    /**
     * 将 MemoryEntry 转换为 MemoryEntryDTO
     *
     * @param entry 领域对象
     * @return DTO 对象
     */
    public static MemoryEntryDTO toDto(MemoryEntry entry) {
        if (entry == null) {
            return null;
        }

        return MemoryEntryDTO.builder()
                .id(entry.getId() != null ? entry.getId().getValue() : null)
                .title(entry.getTitle())
                .description(entry.getDescription())
                .type(entry.getType() != null ? entry.getType().name() : null)
                .scope(entry.getScope() != null ? entry.getScope().name() : null)
                .ownerId(entry.getOwnerId())
                .fileName(entry.getFileName())
                .filePath(entry.getFilePath())
                .content(entry.getContent())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .tags(entry.getTags())
                .build();
    }

    /**
     * 将 MemoryEntryDTO 转换为 MemoryEntry
     *
     * @param dto DTO 对象
     * @return 领域对象
     */
    public static MemoryEntry toEntity(MemoryEntryDTO dto) {
        if (dto == null) {
            return null;
        }

        return MemoryEntry.builder()
                .id(dto.getId() != null ? MemoryId.of(dto.getId()) : null)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .type(dto.getType() != null ? MemoryType.valueOf(dto.getType()) : null)
                .scope(dto.getScope() != null ? MemoryScope.valueOf(dto.getScope()) : null)
                .ownerId(dto.getOwnerId())
                .fileName(dto.getFileName())
                .filePath(dto.getFilePath())
                .content(dto.getContent())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .tags(dto.getTags())
                .build();
    }

    /**
     * 将 MemoryQuery 转换为 MemoryQueryDTO
     *
     * @param query 领域对象
     * @return DTO 对象
     */
    public static MemoryQueryDTO toDto(MemoryQuery query) {
        if (query == null) {
            return null;
        }

        return MemoryQueryDTO.builder()
                .text(query.getText())
                .workspaceId(query.getWorkspaceId())
                .characterId(query.getCharacterId())
                .sessionId(query.getSessionId())
                .scopes(query.getScopes().stream()
                        .map(MemoryScope::name)
                        .collect(Collectors.toSet()))
                .types(query.getTypes().stream()
                        .map(MemoryType::name)
                        .collect(Collectors.toSet()))
                .limit(query.getLimit())
                .build();
    }

    /**
     * 将 MemoryQueryDTO 转换为 MemoryQuery
     *
     * @param dto DTO 对象
     * @return 领域对象
     */
    public static MemoryQuery toEntity(MemoryQueryDTO dto) {
        if (dto == null) {
            return null;
        }

        return MemoryQuery.builder()
                .text(dto.getText())
                .workspaceId(dto.getWorkspaceId())
                .characterId(dto.getCharacterId())
                .sessionId(dto.getSessionId())
                .scopes(dto.getScopes().stream()
                        .map(MemoryScope::valueOf)
                        .collect(Collectors.toSet()))
                .types(dto.getTypes().stream()
                        .map(MemoryType::valueOf)
                        .collect(Collectors.toSet()))
                .limit(dto.getLimit() > 0 ? dto.getLimit() : 5)
                .build();
    }

    /**
     * 将 MemorySearchResult 转换为 MemorySearchResultDTO
     *
     * @param result 领域对象
     * @return DTO 对象
     */
    public static MemorySearchResultDTO toDto(MemorySearchResult result) {
        if (result == null) {
            return null;
        }

        return MemorySearchResultDTO.builder()
                .memory(toDto(result.getMemory()))
                .score(result.getScore())
                .reason(result.getReason())
                .build();
    }

    /**
     * 将 MemorySearchResultDTO 转换为 MemorySearchResult
     *
     * @param dto DTO 对象
     * @return 领域对象
     */
    public static MemorySearchResult toEntity(MemorySearchResultDTO dto) {
        if (dto == null) {
            return null;
        }

        return MemorySearchResult.builder()
                .memory(toEntity(dto.getMemory()))
                .score(dto.getScore())
                .reason(dto.getReason())
                .build();
    }

    /**
     * 将 SessionSnapshot 转换为 SessionSnapshotDTO
     *
     * @param snapshot 领域对象
     * @return DTO 对象
     */
    public static SessionSnapshotDTO toDto(SessionSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }

        return SessionSnapshotDTO.builder()
                .sessionId(snapshot.getSessionId())
                .characterId(snapshot.getCharacterId())
                .workspaceId(snapshot.getWorkspaceId())
                .summary(snapshot.getSummary())
                .currentGoal(snapshot.getCurrentGoal())
                .recentDecisions(snapshot.getRecentDecisions())
                .unresolvedQuestions(snapshot.getUnresolvedQuestions())
                .content(snapshot.getContent())
                .updatedAt(snapshot.getUpdatedAt())
                .build();
    }

    /**
     * 将 SessionSnapshotDTO 转换为 SessionSnapshot
     *
     * @param dto DTO 对象
     * @return 领域对象
     */
    public static SessionSnapshot toEntity(SessionSnapshotDTO dto) {
        if (dto == null) {
            return null;
        }

        return SessionSnapshot.builder()
                .sessionId(dto.getSessionId())
                .characterId(dto.getCharacterId())
                .workspaceId(dto.getWorkspaceId())
                .summary(dto.getSummary())
                .currentGoal(dto.getCurrentGoal())
                .recentDecisions(dto.getRecentDecisions())
                .unresolvedQuestions(dto.getUnresolvedQuestions())
                .content(dto.getContent())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
