package org.dragon.memory.service.meta.impl;

import lombok.extern.slf4j.Slf4j;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.api.controller.dto.memory.BatchDeleteChunksRequest;
import org.dragon.api.controller.dto.memory.BatchOperationResultDTO;
import org.dragon.api.controller.dto.memory.BatchUpdateIndexStatusRequest;
import org.dragon.api.controller.dto.memory.CreateChunkRequest;
import org.dragon.api.controller.dto.memory.MemoryChunkDTO;
import org.dragon.api.controller.dto.memory.UpdateChunkRequest;
import org.dragon.datasource.entity.MemoryChunkEntity;
import org.dragon.memory.service.meta.MemoryMetaChunkService;
import org.dragon.memory.store.MemoryChunkStore;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 记忆片段服务实现类
 *
 * @author binarytom
 * @version 1.0
 */
@Slf4j
@Service
public class MemoryMetaChunkServiceImpl implements MemoryMetaChunkService {

    private final MemoryChunkStore chunkStore;

    public MemoryMetaChunkServiceImpl(StoreFactory storeFactory) {
        this.chunkStore = storeFactory.get(MemoryChunkStore.class);
    }

    @Override
    public PageResponse<MemoryChunkDTO> getChunks(String sourceId, String syncStatus, String indexedStatus, String tags, String search, int page, int pageSize) {
        List<MemoryChunkEntity> all = chunkStore.findByCondition(sourceId, syncStatus, indexedStatus, tags, search);

        int total = all.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<MemoryChunkDTO> pageList = all.subList(fromIndex, toIndex)
                .stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());

        return PageResponse.<MemoryChunkDTO>builder()
                .list(pageList)
                .total((long) total)
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Override
    public MemoryChunkDTO getChunk(String chunkId) {
        return chunkStore.findById(chunkId)
                .map(this::entityToDto)
                .orElse(null);
    }

    @Override
    public MemoryChunkDTO createChunk(CreateChunkRequest request) {
        log.info("[DefaultMemoryChunkService] Creating chunk for sourceId={}", request.getSourceId());

        MemoryChunkEntity entity = MemoryChunkEntity.builder()
                .id(UUID.randomUUID().toString())
                .sourceId(request.getSourceId())
                .title(request.getTitle())
                .content(request.getContent())
                .summary(request.getSummary())
                .tags(serializeList(request.getTags()))
                .indexedStatus("pending")
                .filePath(request.getFilePath())
                .fileType(request.getFileType())
                .totalSize(request.getTotalSize())
                .syncStatus("pending")
                .healthStatus("unknown")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        chunkStore.save(entity);
        log.info("[DefaultMemoryChunkService] Chunk created: {}", entity.getId());
        return entityToDto(entity);
    }

    @Override
    public MemoryChunkDTO updateChunk(String chunkId, UpdateChunkRequest request) {
        return chunkStore.findById(chunkId).map(entity -> {
            if (request.getTitle() != null) {
                entity.setTitle(request.getTitle());
            }
            if (request.getContent() != null) {
                entity.setContent(request.getContent());
            }
            if (request.getSummary() != null) {
                entity.setSummary(request.getSummary());
            }
            if (request.getTags() != null) {
                entity.setTags(serializeList(request.getTags()));
            }
            if (request.getIndexedStatus() != null) {
                entity.setIndexedStatus(request.getIndexedStatus());
            }
            if (request.getFilePath() != null) {
                entity.setFilePath(request.getFilePath());
            }
            if (request.getFileType() != null) {
                entity.setFileType(request.getFileType());
            }
            if (request.getSyncStatus() != null) {
                entity.setSyncStatus(request.getSyncStatus());
            }
            if (request.getHealthStatus() != null) {
                entity.setHealthStatus(request.getHealthStatus());
            }
            entity.setUpdatedAt(Instant.now());
            chunkStore.save(entity);
            return entityToDto(entity);
        }).orElse(null);
    }

    @Override
    public boolean deleteChunk(String chunkId) {
        log.info("[DefaultMemoryChunkService] Deleting chunk: {}", chunkId);
        return chunkStore.deleteById(chunkId);
    }

    @Override
    public BatchOperationResultDTO batchDeleteChunks(BatchDeleteChunksRequest request) {
        log.info("[DefaultMemoryChunkService] Batch deleting {} chunks", request.getChunkIds().size());
        chunkStore.deleteBatch(request.getChunkIds());
        return BatchOperationResultDTO.builder()
                .success(true)
                .deletedCount(request.getChunkIds().size())
                .build();
    }

    @Override
    public BatchOperationResultDTO batchUpdateIndexStatus(BatchUpdateIndexStatusRequest request) {
        log.info("[DefaultMemoryChunkService] Batch updating index status for {} chunks to {}",
                request.getChunkIds().size(), request.getIndexedStatus());
        chunkStore.updateIndexStatusBatch(request.getChunkIds(), request.getIndexedStatus());
        return BatchOperationResultDTO.builder()
                .success(true)
                .updatedCount(request.getChunkIds().size())
                .build();
    }

    @Override
    public String syncChunk(String chunkId) {
        log.info("[DefaultMemoryChunkService] Syncing chunk: {}", chunkId);
        return chunkStore.findById(chunkId).map(entity -> {
            chunkStore.updateSyncStatus(chunkId, "syncing");
            // 更新为已同步，并记录同步时间
            entity.setSyncStatus("synced");
            entity.setLastSyncAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
            chunkStore.save(entity);
            log.info("[DefaultMemoryChunkService] Chunk synced: {}", chunkId);
            return "同步成功";
        }).orElseGet(() -> {
            log.warn("[DefaultMemoryChunkService] Chunk not found for sync: {}", chunkId);
            return "片段不存在";
        });
    }

    // ---- 私有辅助方法 ----

    private MemoryChunkDTO entityToDto(MemoryChunkEntity entity) {
        return MemoryChunkDTO.builder()
                .id(entity.getId())
                .sourceId(entity.getSourceId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .summary(entity.getSummary())
                .tags(deserializeList(entity.getTags()))
                .indexedStatus(entity.getIndexedStatus())
                .relations(deserializeList(entity.getRelations()))
                .filePath(entity.getFilePath())
                .fileType(entity.getFileType())
                .totalSize(entity.getTotalSize())
                .syncStatus(entity.getSyncStatus())
                .healthStatus(entity.getHealthStatus())
                .lastSyncAt(entity.getLastSyncAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * 将字符串列表序列化为 JSON 数组字符串
     *
     * @param list 字符串列表
     * @return JSON 数组字符串，如 ["tag1","tag2"]
     */
    private String serializeList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return "[" + list.stream()
                .map(s -> "\"" + s.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",")) + "]";
    }

    /**
     * 将 JSON 数组字符串反序列化为字符串列表
     *
     * @param json JSON 数组字符串
     * @return 字符串列表
     */
    private List<String> deserializeList(String json) {
        if (json == null || json.isBlank() || json.equals("null")) {
            return Collections.emptyList();
        }
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return Collections.emptyList();
        }
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        if (inner.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(inner.split(","))
                .map(s -> s.trim().replaceAll("^\"|\"$", "").replace("\\\"", "\""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}