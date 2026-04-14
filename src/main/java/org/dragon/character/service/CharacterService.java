package org.dragon.character.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.dragon.api.controller.dto.CharacterDetailDTO;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.asset.enums.AssociationType;
import org.dragon.asset.service.AssetAssociationService;
import org.dragon.asset.service.AssetMemberService;
import org.dragon.asset.service.AssetPublishStatusService;
import org.dragon.asset.tag.dto.AssetTagDTO;
import org.dragon.asset.tag.service.AssetTagService;
import org.dragon.character.Character;
import org.dragon.expert.service.ExpertService;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.profile.CharacterProfile;
import org.dragon.permission.enums.ResourceType;
import org.dragon.permission.service.PermissionService;
import org.dragon.skill.store.SkillStore;
import org.dragon.trait.store.TraitStore;
import org.dragon.util.UserUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * CharacterService 角色领域服务
 *
 * <p>提供 Character CRUD、运行、统计等业务逻辑。
 * 对应前端 /studio 页面角色相关功能。
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterRegistry characterRegistry;
    private final PermissionService permissionService;
    private final AssetMemberService assetMemberService;
    private final AssetPublishStatusService publishStatusService;
    private final TraitStore traitStore;
    private final SkillStore skillStore;
    private final AssetAssociationService assetAssociationService;
    private final AssetTagService assetTagService;
    private final ExpertService expertService;

    /**
     * 分页获取角色列表，支持状态/搜索筛选。
     *
     * @param page     页码（从 1 开始）
     * @param pageSize 每页数量
     * @param search   名称/描述搜索关键词
     * @param status   状态筛选（null 表示全部）
     * @param source   来源筛选（存储在 extensions.source，null 表示全部）
     * @return 分页结果
     */
    public PageResponse<Character> listCharacters(int page, int pageSize, String search,
                                                  String status, String source) {
        List<Character> all = characterRegistry.listAll();

        // 按用户可见性过滤：成员资产 + 已发布资产
        Long userId = Long.parseLong(UserUtils.getUserId());
        List<String> memberCharacterIds = permissionService.getVisibleAssets(ResourceType.CHARACTER, userId);
        List<String> publishedCharacterIds = publishStatusService.getPublishedAssetIds(ResourceType.CHARACTER);

        // 合并可见性：成员资产 + 已发布资产
        java.util.Set<String> visibleIds = new java.util.HashSet<>(memberCharacterIds);
        visibleIds.addAll(publishedCharacterIds);

        // 排除 Expert 标记的资产
        java.util.Set<String> visibleNonExpertIds = expertService.filterOutExpertMarked(
                ResourceType.CHARACTER, java.util.List.copyOf(visibleIds));

        // 过滤
        List<Character> filtered = all.stream()
                .filter(c -> {
                    // 可见性过滤（排除 Expert 标记的资产）
                    if (!visibleNonExpertIds.contains(c.getId())) {
                        return false;
                    }
                    if (search != null && !search.isBlank()) {
                        String s = search.toLowerCase();
                        boolean nameMatch = c.getName() != null && c.getName().toLowerCase().contains(s);
                        boolean descMatch = c.getDescription() != null && c.getDescription().toLowerCase().contains(s);
                        boolean idMatch = c.getId() != null && c.getId().toLowerCase().contains(s);
                        if (!nameMatch && !descMatch && !idMatch) return false;
                    }
                    if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
                        CharacterProfile.Status st = parseStatus(status);
                        if (st != null && c.getStatus() != st) return false;
                    }
                    if (source != null && !source.isBlank() && !"all".equalsIgnoreCase(source)) {
                        Object ext = c.getExtensions() != null ? c.getExtensions().get("source") : null;
                        if (!source.equalsIgnoreCase(String.valueOf(ext))) return false;
                    }
                    return true;
                })
                .toList();

        long total = filtered.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<Character> pageData = fromIndex >= filtered.size() ? List.of() : filtered.subList(fromIndex, toIndex);

        return PageResponse.of(pageData, total, page, pageSize);
    }

    /**
     * 创建角色。
     *
     * @param character 角色实体
     * @return 创建后的角色
     */
    public Character createCharacter(Character character) {
        characterRegistry.register(character);

        // 添加创建者为 Owner
        Long userId = Long.valueOf(UserUtils.getUserId());
        assetMemberService.addOwnerDirectly(ResourceType.CHARACTER, character.getId(), userId);

        // 初始化发布状态（默认为 DRAFT）
        publishStatusService.initializeStatus(ResourceType.CHARACTER, character.getId(), String.valueOf(userId));

        log.info("[CharacterService] Created character: {}", character.getId());
        return characterRegistry.get(character.getId()).orElse(character);
    }

    /**
     * 获取角色详情。
     *
     * @param characterId 角色 ID
     * @return 角色
     */
    public Optional<Character> getCharacter(String characterId) {
        return characterRegistry.get(characterId);
    }

    /**
     * 获取角色详情（含 Skill 和 Trait 完整信息）。
     *
     * @param characterId 角色 ID
     * @return CharacterDetailDTO
     */
    public Optional<CharacterDetailDTO> getCharacterDetail(String characterId) {
        Optional<Character> optChar = characterRegistry.get(characterId);
        if (optChar.isEmpty()) {
            return Optional.empty();
        }

        Character character = optChar.get();

        // 查询关联的 Skill（SKILL_CHARACTER: source=CHARACTER, target=SKILL）
        List<CharacterDetailDTO.SkillInfo> skillInfos = assetAssociationService
                .findBySource(AssociationType.SKILL_CHARACTER, ResourceType.CHARACTER, characterId)
                .stream()
                .map(assoc -> skillStore.findLatestActiveBySkillId(assoc.getTargetId())
                        .map(skillDO -> CharacterDetailDTO.SkillInfo.builder()
                                .skillId(skillDO.getSkillId())
                                .name(skillDO.getName())
                                .displayName(skillDO.getDisplayName())
                                .description(skillDO.getDescription())
                                .category(skillDO.getCategory() != null ? skillDO.getCategory().name() : null)
                                .build())
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        // 查询关联的 Trait
        List<String> traitIds = assetAssociationService.getTraitsForCharacter(characterId);
        List<CharacterDetailDTO.TraitInfo> traitInfos;
        if (traitIds == null || traitIds.isEmpty()) {
            traitInfos = List.of();
        } else {
            traitInfos = traitStore.findByIds(traitIds).stream()
                    .map(entity -> {
                        List<String> tagNames = assetTagService.getTagsForAsset(ResourceType.TRAIT, entity.getId())
                                .stream().map(AssetTagDTO::getName).collect(Collectors.toList());
                        return CharacterDetailDTO.fromTraitEntity(entity, tagNames);
                    })
                    .collect(Collectors.toList());
        }

        CharacterDetailDTO dto = CharacterDetailDTO.builder()
                .character(character)
                .skills(skillInfos)
                .traits(traitInfos)
                .build();

        return Optional.of(dto);
    }

    /**
     * 更新角色信息。
     *
     * @param characterId 角色 ID
     * @param character  更新内容
     * @return 更新后的角色
     */
    public Character updateCharacter(String characterId, Character character) {
        character.setId(characterId);
        characterRegistry.update(character);
        log.info("[CharacterService] Updated character: {}", characterId);
        return characterRegistry.get(characterId).orElse(character);
    }

    /**
     * 删除（注销）角色。
     *
     * @param characterId 角色 ID
     */
    public void deleteCharacter(String characterId) {
        characterRegistry.unregister(characterId);
        // 删除发布状态
        publishStatusService.deleteStatus(ResourceType.CHARACTER, characterId);
        log.info("[CharacterService] Deleted character: {}", characterId);
    }

    /**
     * 独立运行角色（向指定角色发送消息，用于 Studio 测试页面）。
     *
     * @param characterId 角色 ID
     * @param message    用户消息
     * @param sessionId  会话 ID（可选）
     * @return 角色回复
     */
    public Map<String, Object> runCharacter(String characterId, String message, String sessionId) {
        Character character = characterRegistry.get(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        String reply = character.run(message);
        String sid = sessionId != null ? sessionId : java.util.UUID.randomUUID().toString();

        return Map.of(
                "sessionId", sid,
                "reply", reply != null ? reply : "",
                "timestamp", java.time.LocalDateTime.now().toString()
        );
    }

    /**
     * 获取角色统计数据。
     *
     * @return 统计信息 Map
     */
    public Map<String, Object> getCharacterStats() {
        List<Character> all = characterRegistry.listAll();
        long totalCharacters = all.size();
        long activeCharacters = all.stream()
                .filter(c -> c.getStatus() == CharacterProfile.Status.RUNNING
                        || c.getStatus() == CharacterProfile.Status.LOADED)
                .count();
        long runningCharacters = all.stream()
                .filter(c -> c.getStatus() == CharacterProfile.Status.RUNNING)
                .count();

        // 计算派驻数量
        long totalDeployments = all.stream()
                .mapToLong(c -> assetAssociationService.getWorkspacesForCharacter(c.getId()).size())
                .sum();

        return Map.of(
                "totalCharacters", totalCharacters,
                "activeCharacters", activeCharacters,
                "runningCharacters", runningCharacters,
                "totalDeployments", totalDeployments
        );
    }

    private CharacterProfile.Status parseStatus(String status) {
        try {
            return CharacterProfile.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}