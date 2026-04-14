package org.dragon.asset.factory;

import org.dragon.asset.enums.AssociationType;
import org.dragon.asset.service.AssetAssociationService;
import org.dragon.asset.service.AssetMemberService;
import org.dragon.asset.service.AssetPublishStatusService;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.datasource.entity.TraitEntity;
import org.dragon.memory.entity.MemoryEntry;
import org.dragon.memory.entity.MemoryId;
import org.dragon.memory.service.core.CharacterMemoryService;
import org.dragon.permission.enums.ResourceType;
import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.store.SkillStore;
import org.dragon.trait.store.TraitStore;
import org.dragon.util.UserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AssetFactory 资产工厂
 *
 * <p>提供资产的创建和复制方法，包括空白资产创建和深度复制。
 * 所有方法统一设置 owner 和 publishStatus，避免遗漏。
 *
 * @author yijunw
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssetFactory {

    private final CharacterRegistry characterRegistry;
    private final SkillStore skillStore;
    private final TraitStore traitStore;
    private final AssetMemberService assetMemberService;
    private final AssetPublishStatusService publishStatusService;
    private final AssetAssociationService assetAssociationService;
    private final CharacterMemoryService characterMemoryService;

    // ========== 空白资产创建 ==========

    /**
     * 创建空白 Character（带 owner 和 publishStatus）
     */
    public Character createBlankCharacter(String name, String description) {
        Long userId = Long.valueOf(UserUtils.getUserId());
        String id = UUID.randomUUID().toString();

        Character character = new Character();
        character.setId(id);
        character.setName(name);
        character.setDescription(description);
        character.setCreatedAt(LocalDateTime.now());
        character.setUpdatedAt(LocalDateTime.now());

        characterRegistry.register(character);
        assetMemberService.addOwnerDirectly(ResourceType.CHARACTER, id, userId);
        publishStatusService.initializeStatus(ResourceType.CHARACTER, id, String.valueOf(userId));

        log.info("[AssetFactory] Created blank character: {}", id);
        return character;
    }

    /**
     * 创建空白 SkillDO（带 owner 和 publishStatus）
     */
    public SkillDO createBlankSkill(String name, String displayName, String description) {
        Long userId = Long.valueOf(UserUtils.getUserId());
        String id = UUID.randomUUID().toString();

        SkillDO skill = new SkillDO();
        skill.setSkillId(id);
        skill.setName(name);
        skill.setDisplayName(displayName);
        skill.setDescription(description);
        skill.setVersion(1);
        skill.setStatus(org.dragon.skill.enums.SkillStatus.DRAFT);
        skill.setCreatedAt(LocalDateTime.now());

        skillStore.save(skill);
        assetMemberService.addOwnerDirectly(ResourceType.SKILL, id, userId);
        publishStatusService.initializeStatus(ResourceType.SKILL, id, String.valueOf(userId));

        log.info("[AssetFactory] Created blank skill: {}", id);
        return skill;
    }

    /**
     * 创建空白 TraitEntity（带 owner 和 publishStatus）
     */
    public TraitEntity createBlankTrait(String name, String description, String content) {
        Long userId = Long.valueOf(UserUtils.getUserId());
        String id = UUID.randomUUID().toString();

        TraitEntity trait = TraitEntity.builder()
                .id(id)
                .name(name)
                .description(description)
                .content(content)
                .enabled(true)
                .usedByCount(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        traitStore.save(trait);
        assetMemberService.addOwnerDirectly(ResourceType.TRAIT, id, userId);
        publishStatusService.initializeStatus(ResourceType.TRAIT, id, String.valueOf(userId));

        log.info("[AssetFactory] Created blank trait: {}", id);
        return trait;
    }

    // ========== 资产复制（深度复制）==========

    /**
     * 复制 Character 及其关联的 Memory、Skill、Trait
     *
     * <p>全量复制，包括关联的子资产都会设置 owner 和 publishStatus。
     *
     * @param source     源 Character
     * @param operatorId 操作人 ID
     * @return 复制的 Character
     */
    public Character copyCharacter(Character source, Long operatorId) {
        // 1. 复制 Character 实体
        Character copied = copyCharacterEntity(source);

        // 2. 复制关联的 Memory（MEMORY_CHARACTER）
        List<String> memoryIds = assetAssociationService.getMemoriesForCharacter(source.getId());
        for (String memoryId : memoryIds) {
            copyMemory(memoryId, source.getId(), copied.getId());
        }

        // 3. 复制关联的 Skill（SKILL_CHARACTER）
        List<String> skillIds = assetAssociationService.findBySource(
                        AssociationType.SKILL_CHARACTER, ResourceType.CHARACTER, source.getId())
                .stream()
                .map(a -> a.getTargetId())
                .toList();
        for (String skillId : skillIds) {
            String newSkillId = copySkill(skillId, operatorId);
            if (newSkillId != null) {
                assetAssociationService.createAssociation(
                        AssociationType.SKILL_CHARACTER,
                        ResourceType.CHARACTER, copied.getId(),
                        ResourceType.SKILL, newSkillId
                );
            }
        }

        // 4. 复制关联的 Trait（TRAIT_CHARACTER）
        List<String> traitIds = assetAssociationService.getTraitsForCharacter(source.getId());
        for (String traitId : traitIds) {
            String newTraitId = copyTrait(traitId, operatorId);
            if (newTraitId != null) {
                assetAssociationService.createAssociation(
                        AssociationType.TRAIT_CHARACTER,
                        ResourceType.CHARACTER, copied.getId(),
                        ResourceType.TRAIT, newTraitId
                );
            }
        }

        log.info("[AssetFactory] Copied character: {} -> {}", source.getId(), copied.getId());
        return copied;
    }

    private Character copyCharacterEntity(Character source) {
        Character character = new Character();
        character.setName(source.getName());
        character.setDescription(source.getDescription());
        character.setPromptTemplate(source.getPromptTemplate());
        character.setMbti(source.getMbti());
        character.setMindConfig(source.getMindConfig());

        if (source.getAllowedTools() != null) {
            character.setAllowedTools(new HashSet<>(source.getAllowedTools()));
        }
        if (source.getDefaultTools() != null) {
            character.setDefaultTools(List.copyOf(source.getDefaultTools()));
        }

        Map<String, Object> extensions = new HashMap<>();
        if (source.getExtensions() != null) {
            extensions.putAll(source.getExtensions());
        }
        extensions.put("source", "expert_copy");
        extensions.put("sourceCharacterId", source.getId());
        character.setExtensions(extensions);

        character.setCreatedAt(LocalDateTime.now());
        character.setUpdatedAt(LocalDateTime.now());

        String newId = UUID.randomUUID().toString();
        character.setId(newId);
        characterRegistry.register(character);

        return character;
    }

    private void copyMemory(String sourceMemoryId, String sourceCharacterId, String targetCharacterId) {
        MemoryId memoryId = MemoryId.of(sourceMemoryId);
        MemoryEntry source = characterMemoryService.get(sourceCharacterId, memoryId).orElse(null);
        if (source == null) {
            log.warn("[AssetFactory] Memory not found for id: {} in character: {}", sourceMemoryId, sourceCharacterId);
            return;
        }

        MemoryEntry copy = MemoryEntry.builder()
                .id(MemoryId.generate())
                .title(source.getTitle())
                .description(source.getDescription())
                .type(source.getType())
                .scope(source.getScope())
                .ownerId(targetCharacterId)
                .fileName(generateNewFileName(source.getFileName()))
                .content(source.getContent())
                .tags(source.getTags() != null ? new HashMap<>(source.getTags()) : new HashMap<>())
                .build();

        MemoryEntry created = characterMemoryService.create(targetCharacterId, copy);
        log.info("[AssetFactory] Copied memory: {} -> {}", sourceMemoryId, created.getId().getValue());

        assetAssociationService.createAssociation(
                AssociationType.MEMORY_CHARACTER,
                ResourceType.CHARACTER, targetCharacterId,
                ResourceType.MEMORY, created.getId().getValue()
        );
    }

    /**
     * 复制 Skill（带 owner 和 publishStatus）
     *
     * @param sourceSkillId 源 Skill ID
     * @param operatorId    操作人 ID
     * @return 复制的 Skill ID
     */
    public String copySkill(String sourceSkillId, Long operatorId) {
        SkillDO source = skillStore.findLatestBySkillId(sourceSkillId).orElse(null);
        if (source == null) {
            log.warn("[AssetFactory] Skill not found for id: {}", sourceSkillId);
            return null;
        }

        SkillDO copy = new SkillDO();
        copy.setSkillId(UUID.randomUUID().toString());
        copy.setName(source.getName());
        copy.setDisplayName(source.getDisplayName());
        copy.setDescription(source.getDescription());
        copy.setContent(source.getContent());
        copy.setAliases(source.getAliases());
        copy.setWhenToUse(source.getWhenToUse());
        copy.setArgumentHint(source.getArgumentHint());
        copy.setAllowedTools(source.getAllowedTools());
        copy.setModel(source.getModel());
        copy.setDisableModelInvocation(source.getDisableModelInvocation());
        copy.setUserInvocable(source.getUserInvocable());
        copy.setExecutionContext(source.getExecutionContext());
        copy.setEffort(source.getEffort());
        copy.setCategory(source.getCategory());
        copy.setVisibility(source.getVisibility());
        copy.setCreatorType(source.getCreatorType());
        copy.setCreatorId(0L);
        copy.setCreatorName(source.getCreatorName());
        copy.setEditorId(0L);
        copy.setEditorName(source.getEditorName());
        copy.setStatus(org.dragon.skill.enums.SkillStatus.DRAFT);
        copy.setVersion(1);
        copy.setStorageType(source.getStorageType());
        copy.setStorageInfo(source.getStorageInfo());
        copy.setPersist(source.getPersist());
        copy.setPersistMode(source.getPersistMode());

        skillStore.save(copy);
        log.info("[AssetFactory] Copied skill: {} -> {}", sourceSkillId, copy.getSkillId());

        assetMemberService.addOwnerDirectly(ResourceType.SKILL, copy.getSkillId(), operatorId);
        publishStatusService.initializeStatus(ResourceType.SKILL, copy.getSkillId(), String.valueOf(operatorId));

        return copy.getSkillId();
    }

    /**
     * 复制 Trait（带 owner 和 publishStatus）
     *
     * @param sourceTraitId 源 Trait ID
     * @param operatorId   操作人 ID
     * @return 复制的 Trait ID
     */
    public String copyTrait(String sourceTraitId, Long operatorId) {
        TraitEntity source = traitStore.findById(sourceTraitId).orElse(null);
        if (source == null) {
            log.warn("[AssetFactory] Trait not found for id: {}", sourceTraitId);
            return null;
        }

        TraitEntity copy = TraitEntity.builder()
                .id(UUID.randomUUID().toString())
                .name(source.getName())
                .description(source.getDescription())
                .content(source.getContent())
                .enabled(source.getEnabled())
                .usedByCount(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        traitStore.save(copy);
        log.info("[AssetFactory] Copied trait: {} -> {}", sourceTraitId, copy.getId());

        // 增加源 trait 的引用计数
        traitStore.incrementUsedByCount(sourceTraitId);

        assetMemberService.addOwnerDirectly(ResourceType.TRAIT, copy.getId(), operatorId);
        publishStatusService.initializeStatus(ResourceType.TRAIT, copy.getId(), String.valueOf(operatorId));

        return copy.getId();
    }

    private String generateNewFileName(String originalFileName) {
        if (originalFileName == null) {
            return UUID.randomUUID().toString() + ".md";
        }
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return originalFileName.substring(0, dotIndex) + "_copy" + originalFileName.substring(dotIndex);
        }
        return originalFileName + "_copy";
    }
}