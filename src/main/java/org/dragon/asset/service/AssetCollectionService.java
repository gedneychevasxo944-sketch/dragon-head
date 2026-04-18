package org.dragon.asset.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.asset.tag.dto.AssetTagDTO;
import org.dragon.asset.tag.service.AssetTagService;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.profile.CharacterProfile;
import org.dragon.datasource.entity.TraitEntity;
import org.dragon.observer.Observer;
import org.dragon.observer.ObserverRegistry;
import org.dragon.permission.enums.ResourceType;
import org.dragon.permission.service.PermissionService;
import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.dto.SkillSummaryVO;
import org.dragon.skill.store.SkillStore;
import org.dragon.trait.store.TraitStore;
import org.dragon.util.UserUtils;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.WorkspaceFacadeService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AssetCollectionService 资产统一列表服务
 *
 * <p>统一处理所有资产类型的列表查询，包含 visibility 过滤和 builtin/expert 显隐控制。
 *
 * @author yijunw
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetCollectionService {

    // Character
    private final CharacterRegistry characterRegistry;
    private final PermissionService permissionService;
    private final AssetPublishStatusService publishStatusService;
    private final AssetMarkService assetMarkService;

    // Trait
    private final TraitStore traitStore;
    private final AssetTagService assetTagService;

    // Skill
    private final SkillStore skillStore;

    // Workspace
    private final WorkspaceFacadeService workspaceFacadeService;

    // Observer
    private final ObserverRegistry observerRegistry;

    // ==================== Character ====================

    /**
     * 分页获取角色列表（含 builtin/expert 过滤）
     */
    public PageResponse<Character> listCharacters(int page, int pageSize, String search,
                                                String status, String source,
                                                boolean includeBuiltin, boolean includeExpert) {
        List<Character> all = characterRegistry.listAll();

        Set<String> visibleIds = getVisibleAssetIds(ResourceType.CHARACTER, includeBuiltin, includeExpert);

        final Set<String> finalVisibleIds = visibleIds;
        List<Character> filtered = all.stream()
                .filter(c -> {
                    if (!finalVisibleIds.contains(c.getId())) {
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

    private CharacterProfile.Status parseStatus(String status) {
        try {
            return CharacterProfile.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 获取用户可见的资产 ID（成员资产 + 已发布资产），并根据 includeBuiltin/includeExpert 过滤。
     *
     * @param resourceType    资源类型
     * @param includeBuiltin  是否包含内置资产
     * @param includeExpert   是否包含专家资产
     * @return 过滤后的可见资产 ID 集合
     */
    private Set<String> getVisibleAssetIds(ResourceType resourceType, boolean includeBuiltin, boolean includeExpert) {
        Long userId = Long.valueOf(UserUtils.getUserId());

        // 成员资产 + 已发布资产
        List<String> memberIds = permissionService.getVisibleAssets(resourceType, userId);
        List<String> publishedIds = publishStatusService.getPublishedAssetIds(resourceType);

        Set<String> visibleIds = new HashSet<>(memberIds);
        visibleIds.addAll(publishedIds);

        // builtin/expert 过滤
        if (!includeBuiltin) {
            visibleIds = assetMarkService.filterOutBuiltins(resourceType, visibleIds);
        }
        if (!includeExpert) {
            visibleIds = assetMarkService.filterOutExperts(resourceType, visibleIds);
        }

        return visibleIds;
    }

    // ==================== Trait ====================

    /**
     * 分页获取 Trait 列表（含 builtin/expert 过滤）
     */
    public PageResponse<Map<String, Object>> listTraits(int page, int pageSize, String search,
                                                        String tagName, String publishStatus,
                                                        boolean includeBuiltin, boolean includeExpert) {
        Set<String> visibleIds = getVisibleAssetIds(ResourceType.TRAIT, includeBuiltin, includeExpert);

        List<TraitEntity> allTraits;
        if (search != null && !search.isBlank()) {
            allTraits = traitStore.search(search);
        } else {
            allTraits = traitStore.findAll();
        }

        // 按标签筛选
        if (tagName != null && !tagName.isBlank() && !"all".equalsIgnoreCase(tagName)) {
            Set<String> taggedIds = assetTagService.findByTagNameAndResourceType(tagName, ResourceType.TRAIT.name())
                    .stream()
                    .map(e -> e.getResourceId())
                    .collect(Collectors.toSet());
            allTraits = allTraits.stream()
                    .filter(t -> taggedIds.contains(t.getId()))
                    .toList();
        }

        // 按可见性过滤
        final Set<String> finalVisibleIds = visibleIds;
        allTraits = allTraits.stream()
                .filter(t -> finalVisibleIds.contains(String.valueOf(t.getId())))
                .toList();

        // 按发布状态筛选
        if (publishStatus != null && !publishStatus.isBlank()) {
            List<String> filteredIds = publishStatusService.getAssetIdsByStatus(ResourceType.TRAIT,
                    org.dragon.asset.enums.PublishStatus.valueOf(publishStatus));
            allTraits = allTraits.stream()
                    .filter(t -> filteredIds.contains(String.valueOf(t.getId())))
                    .toList();
        }

        long total = allTraits.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, allTraits.size());
        List<TraitEntity> pageTraits = fromIndex >= allTraits.size()
                ? List.of()
                : allTraits.subList(fromIndex, toIndex);

        // 批量加载当前页所有 Trait 的标签，避免 N+1 查询
        List<String> pageIds = pageTraits.stream().map(TraitEntity::getId).toList();
        Map<String, List<AssetTagDTO>> tagsMap = assetTagService.getTagsForAssets(ResourceType.TRAIT, pageIds);

        List<Map<String, Object>> pageData = pageTraits.stream()
                .map(t -> toTraitMap(t, tagsMap.getOrDefault(t.getId(), List.of())))
                .toList();

        return PageResponse.of(pageData, total, page, pageSize);
    }

    private Map<String, Object> toTraitMap(TraitEntity trait, List<AssetTagDTO> tags) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", String.valueOf(trait.getId()));
        map.put("name", trait.getName());
        map.put("description", trait.getDescription());
        map.put("content", trait.getContent());
        map.put("enabled", trait.getEnabled());
        map.put("usedByCount", trait.getUsedByCount());
        map.put("createdAt", trait.getCreateTime() != null ? trait.getCreateTime().toString() : null);
        map.put("updatedAt", trait.getUpdateTime() != null ? trait.getUpdateTime().toString() : null);
        org.dragon.asset.enums.PublishStatus status = publishStatusService.getStatusOrDefault(
                ResourceType.TRAIT, String.valueOf(trait.getId()));
        map.put("publishStatus", status.name());
        map.put("tags", tags);
        return map;
    }

    // ==================== Skill ====================

    /**
     * 分页获取 Skill 列表（占位：includeBuiltin/includeExpert 暂不生效）
     */
    public PageResponse<SkillSummaryVO> listSkills(int page, int pageSize, String search,
                                                  String visibility, String assetState,
                                                  String runtimeStatus, String category,
                                                  boolean includeBuiltin, boolean includeExpert) {
        // TODO: Skill 的 builtin/expert 标记过滤暂未实现
        // 目前 SkillQueryService.pageSearch() 已有 expert 过滤
        int offset = (page - 1) * pageSize;
        List<SkillDO> rows = skillStore.search(
                search, null, category, visibility, null, "created_at", "desc", offset, pageSize);
        int total = skillStore.countSearch(search, null, category, visibility, null);

        List<SkillSummaryVO> items = rows.stream()
                .map(this::toSkillSummaryVO)
                .collect(Collectors.toList());

        return PageResponse.of(items, total, page, pageSize);
    }

    private SkillSummaryVO toSkillSummaryVO(SkillDO s) {
        return SkillSummaryVO.builder()
                .skillId(s.getSkillId())
                .version(s.getVersion())
                .name(s.getName())
                .displayName(s.getDisplayName())
                .description(s.getDescription())
                .whenToUse(s.getWhenToUse())
                .argumentHint(s.getArgumentHint())
                .category(s.getCategory())
                .visibility(s.getVisibility())
                .status(s.getStatus())
                .executionContext(s.getExecutionContext())
                .creatorId(s.getCreatorId())
                .creatorName(s.getCreatorName())
                .createdAt(s.getCreatedAt())
                .publishedAt(s.getPublishedAt())
                .build();
    }

    // ==================== Workspace ====================

    /**
     * 分页获取 Workspace 列表（占位：includeBuiltin/includeExpert 暂不生效）
     */
    public PageResponse<Workspace> listWorkspaces(int page, int pageSize, String search,
                                                 String status, boolean includeBuiltin, boolean includeExpert) {
        List<Workspace> all = workspaceFacadeService.listAllWorkspaces();

        // 过滤
        List<Workspace> filtered = all.stream()
                .filter(w -> {
                    if (search != null && !search.isBlank()) {
                        String s = search.toLowerCase();
                        boolean nameMatch = w.getName() != null && w.getName().toLowerCase().contains(s);
                        boolean descMatch = w.getDescription() != null && w.getDescription().toLowerCase().contains(s);
                        if (!nameMatch && !descMatch) return false;
                    }
                    if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
                        if (!status.equalsIgnoreCase(w.getStatus().name())) return false;
                    }
                    return true;
                })
                .toList();

        long total = filtered.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<Workspace> pageData = fromIndex >= filtered.size() ? List.of() : filtered.subList(fromIndex, toIndex);

        return PageResponse.of(pageData, total, page, pageSize);
    }

    // ==================== Observer ====================

    /**
     * 分页获取 Observer 列表（占位：includeBuiltin/includeExpert 暂不生效）
     */
    public PageResponse<Observer> listObservers(int page, int pageSize, String search,
                                              boolean includeBuiltin, boolean includeExpert) {
        List<Observer> all = observerRegistry.listAll();

        // 过滤
        List<Observer> filtered = all.stream()
                .filter(o -> {
                    if (search != null && !search.isBlank()) {
                        String s = search.toLowerCase();
                        boolean nameMatch = o.getName() != null && o.getName().toLowerCase().contains(s);
                        boolean descMatch = o.getDescription() != null && o.getDescription().toLowerCase().contains(s);
                        if (!nameMatch && !descMatch) return false;
                    }
                    return true;
                })
                .toList();

        long total = filtered.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<Observer> pageData = fromIndex >= filtered.size() ? List.of() : filtered.subList(fromIndex, toIndex);

        return PageResponse.of(pageData, total, page, pageSize);
    }
}