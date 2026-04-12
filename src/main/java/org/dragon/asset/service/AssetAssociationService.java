package org.dragon.asset.service;

import lombok.extern.slf4j.Slf4j;
import org.dragon.asset.dto.AssetAssociationDTO;
import org.dragon.asset.enums.AssociationType;
import org.dragon.asset.store.AssetAssociationStore;
import org.dragon.datasource.entity.AssetAssociationEntity;
import org.dragon.permission.enums.ResourceType;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AssetAssociationService 资产关联服务
 * 负责维护资产之间的关联关系
 */
@Slf4j
@Service
public class AssetAssociationService {

    private final AssetAssociationStore assetAssociationStore;

    public AssetAssociationService(StoreFactory storeFactory) {
        this.assetAssociationStore = storeFactory.get(AssetAssociationStore.class);
    }

    /**
     * 创建资产关联
     */
    public void createAssociation(AssociationType type, ResourceType sourceType, String sourceId,
                                  ResourceType targetType, String targetId) {
        if (assetAssociationStore.exists(type, sourceType, sourceId, targetType, targetId)) {
            log.warn("[AssetAssociationService] Association already exists: type={}, source={}:{}, target={}:{}",
                    type, sourceType, sourceId, targetType, targetId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        AssetAssociationEntity entity = AssetAssociationEntity.builder()
                .associationType(type)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .targetType(targetType)
                .targetId(targetId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assetAssociationStore.save(entity);
        log.info("[AssetAssociationService] Created association: type={}, source={}:{}, target={}:{}",
                type, sourceType, sourceId, targetType, targetId);
    }

    /**
     * 删除资产关联
     */
    public void removeAssociation(AssociationType type, ResourceType sourceType, String sourceId,
                                  ResourceType targetType, String targetId) {
        assetAssociationStore.deleteBySourceAndTarget(type, sourceType, sourceId, targetType, targetId);
        log.info("[AssetAssociationService] Removed association: type={}, source={}:{}, target={}:{}",
                type, sourceType, sourceId, targetType, targetId);
    }

    /**
     * 按源资产查询关联
     */
    public List<AssetAssociationDTO> findBySource(AssociationType type, ResourceType sourceType, String sourceId) {
        return assetAssociationStore.findBySource(type, sourceType, sourceId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按目标资产查询关联
     */
    public List<AssetAssociationDTO> findByTarget(AssociationType type, ResourceType targetType, String targetId) {
        return assetAssociationStore.findByTarget(type, targetType, targetId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按关联类型查询
     */
    public List<AssetAssociationDTO> findByType(AssociationType type) {
        return assetAssociationStore.findByType(type).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 检查关联是否存在
     */
    public boolean exists(AssociationType type, ResourceType sourceType, String sourceId,
                          ResourceType targetType, String targetId) {
        return assetAssociationStore.exists(type, sourceType, sourceId, targetType, targetId);
    }

    /**
     * 获取某 Workspace 下的所有 Character ID
     */
    public List<String> getCharactersInWorkspace(String workspaceId) {
        return assetAssociationStore.findByTarget(AssociationType.CHARACTER_WORKSPACE,
                ResourceType.WORKSPACE, workspaceId).stream()
                .map(AssetAssociationEntity::getSourceId)
                .collect(Collectors.toList());
    }

    /**
     * 获取某 Character 所属的所有 Workspace ID
     */
    public List<String> getWorkspacesForCharacter(String characterId) {
        return assetAssociationStore.findBySource(AssociationType.CHARACTER_WORKSPACE,
                ResourceType.CHARACTER, characterId).stream()
                .map(AssetAssociationEntity::getTargetId)
                .collect(Collectors.toList());
    }

    /**
     * 获取某 Character 关联的所有 Memory ID
     */
    public List<String> getMemoriesForCharacter(String characterId) {
        return assetAssociationStore.findBySource(AssociationType.MEMORY_CHARACTER,
                ResourceType.CHARACTER, characterId).stream()
                .map(AssetAssociationEntity::getTargetId)
                .collect(Collectors.toList());
    }

    /**
     * 获取某 Character 关联的所有 Trait ID
     */
    public List<String> getTraitsForCharacter(String characterId) {
        return assetAssociationStore.findBySource(AssociationType.TRAIT_CHARACTER,
                ResourceType.CHARACTER, characterId).stream()
                .map(AssetAssociationEntity::getTargetId)
                .collect(Collectors.toList());
    }

    /**
     * 获取某 Workspace 关联的所有 Memory ID
     */
    public List<String> getMemoriesForWorkspace(String workspaceId) {
        return assetAssociationStore.findBySource(AssociationType.MEMORY_WORKSPACE,
                ResourceType.WORKSPACE, workspaceId).stream()
                .map(AssetAssociationEntity::getTargetId)
                .collect(Collectors.toList());
    }

    /**
     * 获取某 Workspace 关联的所有 Observer ID
     */
    public List<String> getObserversForWorkspace(String workspaceId) {
        return assetAssociationStore.findBySource(AssociationType.OBSERVER_WORKSPACE,
                ResourceType.WORKSPACE, workspaceId).stream()
                .map(AssetAssociationEntity::getTargetId)
                .collect(Collectors.toList());
    }

    /**
     * 获取某 Skill 关联的所有 Tool ID
     */
    public List<String> getToolsForSkill(String skillId) {
        return assetAssociationStore.findBySource(AssociationType.TOOL_SKILL,
                ResourceType.SKILL, skillId).stream()
                .map(AssetAssociationEntity::getTargetId)
                .collect(Collectors.toList());
    }

    private AssetAssociationDTO toDTO(AssetAssociationEntity entity) {
        return AssetAssociationDTO.builder()
                .id(entity.getId())
                .associationType(entity.getAssociationType())
                .sourceType(entity.getSourceType())
                .sourceId(entity.getSourceId())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
