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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
                .enabled(true)
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
     * 启用资产关联（通过五元组定位）
     *
     * @param type       关联类型
     * @param sourceType 源资产类型
     * @param sourceId   源资产 ID
     * @param targetType 目标资产类型
     * @param targetId   目标资产 ID
     */
    public void enableAssociation(AssociationType type, ResourceType sourceType, String sourceId,
                                   ResourceType targetType, String targetId) {
        assetAssociationStore.setEnabled(type, sourceType, sourceId, targetType, targetId, true);
        log.info("[AssetAssociationService] Enabled association: type={}, source={}:{}, target={}:{}",
                type, sourceType, sourceId, targetType, targetId);
    }

    /**
     * 禁用资产关联（保留记录但不生效，通过五元组定位）
     *
     * @param type       关联类型
     * @param sourceType 源资产类型
     * @param sourceId   源资产 ID
     * @param targetType 目标资产类型
     * @param targetId   目标资产 ID
     */
    public void disableAssociation(AssociationType type, ResourceType sourceType, String sourceId,
                                    ResourceType targetType, String targetId) {
        assetAssociationStore.setEnabled(type, sourceType, sourceId, targetType, targetId, false);
        log.info("[AssetAssociationService] Disabled association: type={}, source={}:{}, target={}:{}",
                type, sourceType, sourceId, targetType, targetId);
    }

    /**
     * 获取某 Workspace 下的所有 Character ID
     */
    public List<String> getCharactersInWorkspace(String workspaceId) {
        return assetAssociationStore.findByTarget(AssociationType.CHARACTER_WORKSPACE,
                ResourceType.WORKSPACE, workspaceId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getEnabled()))
                .map(AssetAssociationEntity::getSourceId)
                .collect(Collectors.toList());
    }

    /**
     * 获取某 Character 所属的所有 Workspace ID
     */
    public List<String> getWorkspacesForCharacter(String characterId) {
        return assetAssociationStore.findBySource(AssociationType.CHARACTER_WORKSPACE,
                ResourceType.CHARACTER, characterId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getEnabled()))
                .map(AssetAssociationEntity::getTargetId)
                .collect(Collectors.toList());
    }

    /**
     * 获取某 Character 关联的所有 Memory ID
     */
    public List<String> getMemoriesForCharacter(String characterId) {
        return assetAssociationStore.findBySource(AssociationType.MEMORY_CHARACTER,
                ResourceType.CHARACTER, characterId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getEnabled()))
                .map(AssetAssociationEntity::getTargetId)
                .collect(Collectors.toList());
    }

    /**
     * 获取某 Character 关联的所有 Trait ID
     */
    public List<String> getTraitsForCharacter(String characterId) {
        return assetAssociationStore.findBySource(AssociationType.TRAIT_CHARACTER,
                ResourceType.CHARACTER, characterId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getEnabled()))
                .map(AssetAssociationEntity::getTargetId)
                .collect(Collectors.toList());
    }

    /**
     * 获取某 Workspace 关联的所有 Memory ID
     */
    public List<String> getMemoriesForWorkspace(String workspaceId) {
        return assetAssociationStore.findBySource(AssociationType.MEMORY_WORKSPACE,
                ResourceType.WORKSPACE, workspaceId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getEnabled()))
                .map(AssetAssociationEntity::getTargetId)
                .collect(Collectors.toList());
    }

    /**
     * 获取某 Workspace 关联的所有 Observer ID
     */
    public List<String> getObserversForWorkspace(String workspaceId) {
        return assetAssociationStore.findBySource(AssociationType.OBSERVER_WORKSPACE,
                ResourceType.WORKSPACE, workspaceId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getEnabled()))
                .map(AssetAssociationEntity::getTargetId)
                .collect(Collectors.toList());
    }

    /**
     * 获取某 Skill 关联的所有 Tool ID
     */
    public List<String> getToolsForSkill(String skillId) {
        return assetAssociationStore.findBySource(AssociationType.TOOL_SKILL,
                ResourceType.SKILL, skillId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getEnabled()))
                .map(AssetAssociationEntity::getTargetId)
                .collect(Collectors.toList());
    }

    /**
     * 获取某 Tool 被哪些 Skill 引用（反向查询）
     */
    public List<String> getSkillsForTool(String toolId) {
        return assetAssociationStore.findByTarget(AssociationType.TOOL_SKILL,
                ResourceType.TOOL, toolId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getEnabled()))
                .map(AssetAssociationEntity::getSourceId)
                .collect(Collectors.toList());
    }

    // ── Tool ↔ Character ────────────────────────────────────────────

    /**
     * 获取某 Character 关联的所有 Tool ID
     */
    public List<String> getToolsForCharacter(String characterId) {
        return assetAssociationStore.findByTarget(AssociationType.TOOL_CHARACTER,
                ResourceType.CHARACTER, characterId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getEnabled()))
                .map(AssetAssociationEntity::getSourceId)
                .collect(Collectors.toList());
    }

    /**
     * 获取某 Tool 关联的所有 Character ID
     */
    public List<String> getCharactersForTool(String toolId) {
        return assetAssociationStore.findBySource(AssociationType.TOOL_CHARACTER,
                ResourceType.TOOL, toolId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getEnabled()))
                .map(AssetAssociationEntity::getTargetId)
                .collect(Collectors.toList());
    }

    // ── Skill ↔ Workspace ───────────────────────────────────────────

    /**
     * 获取某 Workspace 关联的所有 Skill 关联记录（含 enabled 状态）。
     *
     * <p>与 {@link #getSkillsForWorkspace} 不同，此方法返回全部记录（包括已禁用的），
     * 适用于需要展示启用/禁用状态的详情页场景。
     */
    public List<AssetAssociationDTO> getSkillBindingsForWorkspace(String workspaceId) {
        return assetAssociationStore.findByTarget(AssociationType.SKILL_WORKSPACE,
                ResourceType.WORKSPACE, workspaceId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取某 Workspace 关联的所有 Skill ID
     */
    public List<String> getSkillsForWorkspace(String workspaceId) {
        return assetAssociationStore.findByTarget(AssociationType.SKILL_WORKSPACE,
                ResourceType.WORKSPACE, workspaceId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getEnabled()))
                .map(AssetAssociationEntity::getSourceId)
                .collect(Collectors.toList());
    }

    /**
     * 获取某 Skill 关联的所有 Workspace ID
     */
    public List<String> getWorkspacesForSkill(String skillId) {
        return assetAssociationStore.findBySource(AssociationType.SKILL_WORKSPACE,
                ResourceType.SKILL, skillId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getEnabled()))
                .map(AssetAssociationEntity::getTargetId)
                .collect(Collectors.toList());
    }

    // ── Skill ↔ Character ───────────────────────────────────────────

    /**
     * 获取某 Character 关联的所有 Skill ID
     */
    public List<String> getSkillsForCharacter(String characterId) {
        return assetAssociationStore.findByTarget(AssociationType.SKILL_CHARACTER,
                ResourceType.CHARACTER, characterId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getEnabled()))
                .map(AssetAssociationEntity::getSourceId)
                .collect(Collectors.toList());
    }

    /**
     * 获取某 Skill 关联的所有 Character ID
     */
    public List<String> getCharactersForSkill(String skillId) {
        return assetAssociationStore.findBySource(AssociationType.SKILL_CHARACTER,
                ResourceType.SKILL, skillId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getEnabled()))
                .map(AssetAssociationEntity::getTargetId)
                .collect(Collectors.toList());
    }

    // ── Tool ↔ Workspace ────────────────────────────────────────────

    /**
     * 获取某 Workspace 关联的所有 Tool ID
     */
    public List<String> getToolsForWorkspace(String workspaceId) {
        return assetAssociationStore.findByTarget(AssociationType.TOOL_WORKSPACE,
                ResourceType.WORKSPACE, workspaceId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getEnabled()))
                .map(AssetAssociationEntity::getSourceId)
                .collect(Collectors.toList());
    }

    /**
     * 获取某 Tool 关联的所有 Workspace ID
     */
    public List<String> getWorkspacesForTool(String toolId) {
        return assetAssociationStore.findBySource(AssociationType.TOOL_WORKSPACE,
                ResourceType.TOOL, toolId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getEnabled()))
                .map(AssetAssociationEntity::getTargetId)
                .collect(Collectors.toList());
    }

    /**
     * 查询某 Character 及其所属 Workspace 可用的所有 Skill ID（SKILL_CHARACTER ∪ SKILL_WORKSPACE）。
     *
     * <p>同一 skillId 若同时出现在两种关联中去重，插入顺序保留（workspace 先、character 后覆盖）。
     *
     * @param characterId Character ID（可为 null，仅查 workspace 级别）
     * @param workspaceId Workspace ID（可为 null，仅查 character 级别）
     * @return 合并去重后的 Skill ID 列表
     */
    public List<String> getAvailableSkills(String characterId, String workspaceId) {
        Set<String> merged = new LinkedHashSet<>();
        if (workspaceId != null) {
            merged.addAll(getSkillsForWorkspace(workspaceId));
        }
        if (characterId != null) {
            merged.addAll(getSkillsForCharacter(characterId));
        }
        return List.copyOf(merged);
    }

    /**
     * 查询某 Character 及其所属 Workspace 可用的所有 Tool ID（TOOL_CHARACTER ∪ TOOL_WORKSPACE）。
     *
     * <p>同一 toolId 若同时出现在两种关联中去重，插入顺序保留（workspace 先、character 后）。
     *
     * @param characterId Character ID（可为 null，仅查 workspace 级别）
     * @param workspaceId Workspace ID（可为 null，仅查 character 级别）
     * @return 合并去重后的 Tool ID 列表
     */
    public List<String> getAvailableTools(String characterId, String workspaceId) {
        Set<String> merged = new LinkedHashSet<>();
        if (workspaceId != null) {
            merged.addAll(getToolsForWorkspace(workspaceId));
        }
        if (characterId != null) {
            merged.addAll(getToolsForCharacter(characterId));
        }
        return List.copyOf(merged);
    }

    private AssetAssociationDTO toDTO(AssetAssociationEntity entity) {
        return AssetAssociationDTO.builder()
                .id(entity.getId())
                .associationType(entity.getAssociationType())
                .sourceType(entity.getSourceType())
                .sourceId(entity.getSourceId())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .enabled(entity.getEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
