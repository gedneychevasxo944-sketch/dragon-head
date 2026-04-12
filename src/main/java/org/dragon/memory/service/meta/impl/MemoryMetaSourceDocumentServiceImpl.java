package org.dragon.memory.service.meta.impl;

import org.apache.commons.lang3.BooleanUtils;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.api.controller.dto.memory.SourceDocumentDTO;
import org.dragon.api.controller.dto.memory.CreateSourceRequest;
import org.dragon.api.controller.dto.memory.UpdateSourceRequest;
import org.dragon.datasource.entity.SourceDocumentEntity;
import org.dragon.memory.service.meta.MemoryMetaSourceDocumentService;
import org.dragon.memory.store.SourceDocumentStore;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 数据源服务实现类
 *
 * @author binarytom
 * @version 1.0
 */
@Service
public class MemoryMetaSourceDocumentServiceImpl implements MemoryMetaSourceDocumentService {

    private final SourceDocumentStore sourceDocumentStore;

    public MemoryMetaSourceDocumentServiceImpl(StoreFactory storeFactory) {
        this.sourceDocumentStore = storeFactory.get(SourceDocumentStore.class);
    }

    @Override
    public PageResponse<SourceDocumentDTO> getSources(String search, String status, String sourceType, int page, int pageSize) {
        List<SourceDocumentEntity> entities = sourceDocumentStore.findByCondition(search, status, sourceType);
        long total = sourceDocumentStore.countByCondition(search, status, sourceType);

        // 分页处理
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, entities.size());
        List<SourceDocumentEntity> pageEntities = entities.subList(startIndex, endIndex);

        // 转换为 DTO
        List<SourceDocumentDTO> dtos = pageEntities.stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());

        return PageResponse.<SourceDocumentDTO>builder()
                .list(dtos)
                .total(total)
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Override
    public SourceDocumentDTO getSource(String sourceId) {
        return sourceDocumentStore.findById(sourceId)
                .map(this::entityToDto)
                .orElse(null);
    }

    @Override
    public SourceDocumentDTO createSource(CreateSourceRequest request) {
        String id = UUID.randomUUID().toString();
        SourceDocumentEntity entity = SourceDocumentEntity.builder()
                .id(id)
                .title(request.getTitle())
                .sourcePath(request.getSourcePath())
                .sourceType(request.getSourceType())
                .backend(request.getBackend())
                .provider(request.getProvider())
                .enabled(BooleanUtils.isTrue(request.getEnabled()))
                .status("active")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        sourceDocumentStore.save(entity);
        return entityToDto(entity);
    }

    @Override
    public SourceDocumentDTO updateSource(String sourceId, UpdateSourceRequest request) {
        return sourceDocumentStore.findById(sourceId)
                .map(entity -> {
                    entity.setTitle(request.getTitle());
                    entity.setSourcePath(request.getSourcePath());
                    entity.setSourceType(request.getSourceType());
                    entity.setBackend(request.getBackend());
                    entity.setProvider(request.getProvider());
                    entity.setEnabled(request.isEnabled());
                    entity.setStatus(request.getStatus());
                    entity.setUpdatedAt(Instant.now());

                    sourceDocumentStore.save(entity);
                    return entityToDto(entity);
                })
                .orElse(null);
    }

    @Override
    public boolean deleteSource(String sourceId) {
        return sourceDocumentStore.deleteById(sourceId);
    }

    @Override
    public String syncSource(String sourceId) {
        // TODO: 实现数据源同步逻辑
        return "同步成功";
    }

    private SourceDocumentDTO entityToDto(SourceDocumentEntity entity) {
        return SourceDocumentDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .sourcePath(entity.getSourcePath())
                .sourceType(entity.getSourceType())
                .backend(entity.getBackend())
                .provider(entity.getProvider())
                .enabled(entity.isEnabled())
                .status(entity.getStatus())
                .lastIndexedAt(entity.getLastIndexedAt())
                .itemCount(entity.getItemCount())
                .fileCount(entity.getFileCount())
                .errorMessage(entity.getErrorMessage())
                .isFusedSource(entity.isFusedSource())
                .isBuiltIn(entity.isBuiltIn())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
