package org.dragon.memory.service.meta.impl;

import lombok.extern.slf4j.Slf4j;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.api.controller.dto.memory.BatchDeleteChunksRequest;
import org.dragon.api.controller.dto.memory.BatchOperationResultDTO;
import org.dragon.api.controller.dto.memory.BatchUpdateIndexStatusRequest;
import org.dragon.api.controller.dto.memory.CreateChunkRequest;
import org.dragon.api.controller.dto.memory.MemoryChunkDTO;
import org.dragon.api.controller.dto.memory.UpdateChunkRequest;
import org.dragon.asset.tag.dto.AssetTagDTO;
import org.dragon.asset.tag.service.AssetTagService;
import org.dragon.datasource.entity.MemoryChunkEntity;
import org.dragon.memory.service.meta.MemoryMetaChunkService;
import org.dragon.memory.store.MemoryChunkStore;
import org.dragon.permission.enums.ResourceType;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private final AssetTagService assetTagService;

    public MemoryMetaChunkServiceImpl(StoreFactory storeFactory, AssetTagService assetTagService) {
        this.chunkStore = storeFactory.get(MemoryChunkStore.class);
        this.assetTagService = assetTagService;
    }

    @Override
    public PageResponse<MemoryChunkDTO> getChunks(String sourceId, String syncStatus, String indexedStatus, String tags, String search, int page, int pageSize) {
        List<MemoryChunkEntity> all = chunkStore.findByCondition(sourceId, syncStatus, indexedStatus, search);

        // 按标签过滤（标签存储在 asset_tag 表，非实体字段）
        List<MemoryChunkEntity> filtered = all;
        if (tags != null && !tags.isBlank()) {
            // 先批量加载所有相关标签
            List<String> chunkIds = all.stream().map(MemoryChunkEntity::getId).toList();
            Map<String, List<AssetTagDTO>> tagsMap = assetTagService.getTagsForAssets(ResourceType.MEMORY_CHUNK, chunkIds);
            filtered = all.stream()
                    .filter(e -> {
                        List<AssetTagDTO> chunkTags = tagsMap.getOrDefault(e.getId(), Collections.emptyList());
                        return chunkTags.stream().anyMatch(t -> t.getName().contains(tags));
                    })
                    .collect(Collectors.toList());
        }

        int total = filtered.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<MemoryChunkDTO> pageList = (fromIndex < toIndex ? filtered.subList(fromIndex, toIndex) : Collections.<MemoryChunkEntity>emptyList())
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
        log.info("[MemoryMetaChunkServiceImpl] Creating chunk for sourceId={}", request.getSourceId());

        MemoryChunkEntity entity = MemoryChunkEntity.builder()
                .id(UUID.randomUUID().toString())
                .sourceId(request.getSourceId())
                .title(request.getTitle())
                .content(request.getContent())
                .summary(request.getSummary())
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

        // 标签通过 asset_tag 表管理
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            assetTagService.tagAssets(ResourceType.MEMORY_CHUNK, entity.getId(), request.getTags());
        }

        log.info("[MemoryMetaChunkServiceImpl] Chunk created: {}", entity.getId());
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
            // 标签通过 asset_tag 表管理
            if (request.getTags() != null) {
                // 获取现有标签
                List<AssetTagDTO> existingTags = assetTagService.getTagsForAsset(ResourceType.MEMORY_CHUNK, chunkId);
                // 移除不在新列表中的标签
                for (AssetTagDTO existingTag : existingTags) {
                    if (!request.getTags().contains(existingTag.getName())) {
                        assetTagService.untagAsset(ResourceType.MEMORY_CHUNK, chunkId, existingTag.getName());
                    }
                }
                // 添加新标签
                for (String tagName : request.getTags()) {
                    if (existingTags.stream().noneMatch(t -> t.getName().equals(tagName))) {
                        assetTagService.tagAsset(ResourceType.MEMORY_CHUNK, chunkId, tagName);
                    }
                }
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
        log.info("[MemoryMetaChunkServiceImpl] Deleting chunk: {}", chunkId);
        return chunkStore.deleteById(chunkId);
    }

    @Override
    public BatchOperationResultDTO batchDeleteChunks(BatchDeleteChunksRequest request) {
        log.info("[MemoryMetaChunkServiceImpl] Batch deleting {} chunks", request.getChunkIds().size());
        chunkStore.deleteBatch(request.getChunkIds());
        return BatchOperationResultDTO.builder()
                .success(true)
                .deletedCount(request.getChunkIds().size())
                .build();
    }

    @Override
    public BatchOperationResultDTO batchUpdateIndexStatus(BatchUpdateIndexStatusRequest request) {
        log.info("[MemoryMetaChunkServiceImpl] Batch updating index status for {} chunks to {}",
                request.getChunkIds().size(), request.getIndexedStatus());
        chunkStore.updateIndexStatusBatch(request.getChunkIds(), request.getIndexedStatus());
        return BatchOperationResultDTO.builder()
                .success(true)
                .updatedCount(request.getChunkIds().size())
                .build();
    }

    @Override
    public String syncChunk(String chunkId) {
        log.info("[MemoryMetaChunkServiceImpl] Syncing chunk: {}", chunkId);
        return chunkStore.findById(chunkId).map(entity -> {
            chunkStore.updateSyncStatus(chunkId, "syncing");
            // 更新为已同步，并记录同步时间
            entity.setSyncStatus("synced");
            entity.setLastSyncAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
            chunkStore.save(entity);
            log.info("[MemoryMetaChunkServiceImpl] Chunk synced: {}", chunkId);
            return "同步成功";
        }).orElseGet(() -> {
            log.warn("[MemoryMetaChunkServiceImpl] Chunk not found for sync: {}", chunkId);
            return "片段不存在";
        });
    }

    // ---- 私有辅助方法 ----

    private MemoryChunkDTO entityToDto(MemoryChunkEntity entity) {
        // 从 asset_tag 表获取标签
        List<String> tagNames = assetTagService.getTagsForAsset(ResourceType.MEMORY_CHUNK, entity.getId())
                .stream()
                .map(AssetTagDTO::getName)
                .collect(Collectors.toList());

        return MemoryChunkDTO.builder()
                .id(entity.getId())
                .sourceId(entity.getSourceId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .summary(entity.getSummary())
                .tags(tagNames)
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
        return java.util.Arrays.stream(inner.split(","))
                .map(s -> s.trim().replaceAll("^\"|\"$", "").replace("\\\"", "\""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}