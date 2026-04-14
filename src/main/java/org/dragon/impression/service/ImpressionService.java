package org.dragon.impression.service;

import lombok.extern.slf4j.Slf4j;
import org.dragon.impression.dto.CreateImpressionRequest;
import org.dragon.impression.dto.ImpressionDTO;
import org.dragon.impression.entity.ImpressionEntity;
import org.dragon.impression.enums.ImpressionType;
import org.dragon.impression.store.ImpressionStore;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ImpressionService 印象服务
 * 负责管理 Character 和 Workspace 之间的印象/评价
 */
@Slf4j
@Service
public class ImpressionService {

    private final ImpressionStore impressionStore;

    public ImpressionService(StoreFactory storeFactory) {
        this.impressionStore = storeFactory.get(ImpressionStore.class);
    }

    /**
     * 创建印象
     */
    public String createImpression(CreateImpressionRequest request) {
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        ImpressionEntity entity = ImpressionEntity.builder()
                .id(id)
                .sourceType(request.getSourceType())
                .sourceId(request.getSourceId())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .name(request.getName())
                .value(request.getValue())
                .sentiment(request.getSentiment())
                .trustLevel(request.getTrustLevel())
                .summary(request.getSummary())
                .createdAt(now)
                .updatedAt(now)
                .build();

        impressionStore.save(entity);
        log.info("[ImpressionService] Created impression: id={}, source={}:{}, target={}:{}",
                id, request.getSourceType(), request.getSourceId(), request.getTargetType(), request.getTargetId());
        return id;
    }

    /**
     * 更新印象
     */
    public void updateImpression(String id, CreateImpressionRequest request) {
        ImpressionEntity entity = impressionStore.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Impression not found: " + id));

        entity.setName(request.getName());
        entity.setValue(request.getValue());
        entity.setSentiment(request.getSentiment());
        entity.setTrustLevel(request.getTrustLevel());
        entity.setSummary(request.getSummary());
        entity.setUpdatedAt(LocalDateTime.now());

        impressionStore.update(entity);
        log.info("[ImpressionService] Updated impression: id={}", id);
    }

    /**
     * 删除印象
     */
    public void deleteImpression(String id) {
        impressionStore.delete(id);
        log.info("[ImpressionService] Deleted impression: id={}", id);
    }

    /**
     * 获取印象详情
     */
    public ImpressionDTO getImpression(String id) {
        return impressionStore.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Impression not found: " + id));
    }

    /**
     * 获取源实体的所有印象
     */
    public List<ImpressionDTO> getImpressionsBySource(ImpressionType sourceType, String sourceId) {
        return impressionStore.findBySource(sourceType, sourceId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取目标实体的所有印象
     */
    public List<ImpressionDTO> getImpressionsByTarget(ImpressionType targetType, String targetId) {
        return impressionStore.findByTarget(targetType, targetId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取两个实体之间的特定印象
     */
    public Optional<ImpressionDTO> getImpressionBetween(ImpressionType sourceType, String sourceId,
                                                         ImpressionType targetType, String targetId,
                                                         String impressionName) {
        return impressionStore.findBySourceAndTarget(sourceType, sourceId, targetType, targetId)
                .filter(e -> e.getName().equals(impressionName))
                .map(this::toDTO);
    }

    /**
     * 删除源实体对目标实体的所有印象
     */
    public void deleteImpressionsBySourceAndTarget(ImpressionType sourceType, String sourceId,
                                                    ImpressionType targetType, String targetId) {
        impressionStore.deleteBySourceAndTarget(sourceType, sourceId, targetType, targetId);
        log.info("[ImpressionService] Deleted impressions: source={}:{}, target={}:{}",
                sourceType, sourceId, targetType, targetId);
    }

    private ImpressionDTO toDTO(ImpressionEntity entity) {
        return ImpressionDTO.builder()
                .id(entity.getId())
                .sourceType(entity.getSourceType())
                .sourceId(entity.getSourceId())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .name(entity.getName())
                .value(entity.getValue())
                .sentiment(entity.getSentiment())
                .trustLevel(entity.getTrustLevel())
                .summary(entity.getSummary())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
