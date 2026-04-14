package org.dragon.trait.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.dragon.api.controller.dto.PageResponse;
import org.dragon.asset.enums.PublishStatus;
import org.dragon.asset.service.AssetMemberService;
import org.dragon.asset.service.AssetPublishStatusService;
import org.dragon.asset.tag.dto.AssetTagDTO;
import org.dragon.asset.tag.service.AssetTagService;
import org.dragon.datasource.entity.TraitEntity;
import org.dragon.expert.service.ExpertService;
import org.dragon.permission.enums.ResourceType;
import org.dragon.store.StoreFactory;
import org.dragon.trait.store.TraitStore;
import org.dragon.util.UserUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * TraitService 特征片段服务
 */
@Service
@RequiredArgsConstructor
public class TraitService {

    private final StoreFactory storeFactory;
    private final AssetMemberService assetMemberService;
    private final AssetPublishStatusService publishStatusService;
    private final AssetTagService assetTagService;
    private final ExpertService expertService;

    private TraitStore getStore() {
        return storeFactory.get(TraitStore.class);
    }

    /**
     * 创建 Trait
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> createTrait(Map<String, Object> traitData) {
        TraitEntity trait = TraitEntity.builder()
                .name((String) traitData.get("name"))
                .description((String) traitData.get("description"))
                .content((String) traitData.get("content"))
                .enabled(true)
                .usedByCount(0)
                .createTime(LocalDateTime.now())
                .build();

        getStore().save(trait);

        // 添加创建者为 Owner
        Long userId = Long.valueOf(UserUtils.getUserId());
        assetMemberService.addOwnerDirectly(ResourceType.TRAIT, trait.getId(), userId);

        // 初始化发布状态（默认为 DRAFT）
        publishStatusService.initializeStatus(ResourceType.TRAIT, trait.getId(), String.valueOf(userId));

        // 绑定标签
        List<String> tagIds = (List<String>) traitData.get("tagIds");
        if (tagIds != null && !tagIds.isEmpty()) {
            assetTagService.tagAssets(ResourceType.TRAIT, trait.getId(), tagIds);
        }

        return toMap(trait);
    }

    /**
     * 获取 Trait 详情
     */
    public Optional<Map<String, Object>> getTrait(String id) {
        return getStore().findById(id).map(this::toMap);
    }

    /**
     * 更新 Trait
     */
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> updateTrait(String id, Map<String, Object> traitData) {
        return getStore().findById(id).map(existing -> {
            if (traitData.containsKey("name")) {
                existing.setName((String) traitData.get("name"));
            }
            if (traitData.containsKey("description")) {
                existing.setDescription((String) traitData.get("description"));
            }
            if (traitData.containsKey("content")) {
                existing.setContent((String) traitData.get("content"));
            }
            if (traitData.containsKey("enabled")) {
                existing.setEnabled((Boolean) traitData.get("enabled"));
            }
            getStore().update(existing);

            // 更新标签（先全量清除再重建）
            if (traitData.containsKey("tagIds")) {
                List<String> tagNames = (List<String>) traitData.get("tagIds");
                // 先移除现有标签
                assetTagService.getTagsForAsset(ResourceType.TRAIT, id)
                        .forEach(tag -> assetTagService.untagAsset(ResourceType.TRAIT, id, tag.getName()));
                // 再绑定新标签
                if (tagNames != null && !tagNames.isEmpty()) {
                    assetTagService.tagAssets(ResourceType.TRAIT, id, tagNames);
                }
            }

            return toMap(existing);
        });
    }

    /**
     * 删除 Trait
     */
    public boolean deleteTrait(String id) {
        Optional<TraitEntity> existing = getStore().findById(id);
        if (existing.isPresent()) {
            getStore().delete(id);
            return true;
        }
        return false;
    }

    /**
     * 分页查询 Trait 列表
     *
     * @param tagName       可选，按标签名称筛选
     * @param publishStatus 可选，按发布状态筛选（DRAFT/PUBLISHED）
     */
    public PageResponse<Map<String, Object>> listTraits(int page, int pageSize, String search, String tagName, String publishStatus) {
        // 获取当前用户可见的 Trait ID（用户作为成员拥有的 + 已发布的）
        Long userId = Long.valueOf(UserUtils.getUserId());
        List<String> memberTraitIds = assetMemberService.getMemberAssetIds(ResourceType.TRAIT, userId);
        List<String> publishedTraitIds = publishStatusService.getPublishedAssetIds(ResourceType.TRAIT);

        // 可见性过滤：用户成员资产 + 已发布资产 的并集
        java.util.Set<String> visibleTraitIds;
        if (memberTraitIds.isEmpty()) {
            visibleTraitIds = new java.util.HashSet<>(publishedTraitIds);
        } else if (publishedTraitIds.isEmpty()) {
            visibleTraitIds = new java.util.HashSet<>(memberTraitIds);
        } else {
            visibleTraitIds = new java.util.HashSet<>(memberTraitIds);
            visibleTraitIds.addAll(publishedTraitIds);
        }

        List<TraitEntity> allTraits;

        if (search != null && !search.isBlank()) {
            allTraits = getStore().search(search);
        } else {
            allTraits = getStore().findAll();
        }

        // 按标签筛选
        if (tagName != null && !tagName.isBlank() && !"all".equalsIgnoreCase(tagName)) {
            java.util.Set<String> taggedIds = assetTagService.findByTagNameAndResourceType(tagName, ResourceType.TRAIT.name())
                    .stream()
                    .map(e -> e.getResourceId())
                    .collect(java.util.stream.Collectors.toSet());
            allTraits = allTraits.stream()
                    .filter(t -> taggedIds.contains(t.getId()))
                    .toList();
        }

        // 按可见性过滤（成员资产 + 已发布资产）
        final java.util.Set<String> finalVisibleIds = visibleTraitIds;
        allTraits = allTraits.stream()
                .filter(t -> finalVisibleIds.contains(String.valueOf(t.getId())))
                .toList();

        // 过滤掉 Expert 标记的资产
        java.util.Set<String> nonExpertIds = expertService.filterOutExpertMarked(
                ResourceType.TRAIT,
                allTraits.stream().map(TraitEntity::getId).collect(java.util.stream.Collectors.toList()));
        allTraits = allTraits.stream()
                .filter(t -> nonExpertIds.contains(t.getId()))
                .toList();

        // 按发布状态筛选
        if (publishStatus != null && !publishStatus.isBlank()) {
            List<String> filteredIds = publishStatusService.getAssetIdsByStatus(ResourceType.TRAIT, PublishStatus.valueOf(publishStatus));
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
                .map(t -> toMap(t, tagsMap.getOrDefault(t.getId(), List.<AssetTagDTO>of())))
                .toList();

        return PageResponse.of(pageData, total, page, pageSize);
    }

    /**
     * 增加引用计数
     */
    public void incrementUsedByCount(String traitId) {
        getStore().incrementUsedByCount(traitId);
    }

    /**
     * 减少引用计数
     */
    public void decrementUsedByCount(String traitId) {
        getStore().decrementUsedByCount(traitId);
    }

    /**
     * 转换为 Map（单条查询，内部获取标签）
     */
    public Map<String, Object> toMap(TraitEntity trait) {
        List<AssetTagDTO> tags = assetTagService.getTagsForAsset(ResourceType.TRAIT, trait.getId());
        return toMap(trait, tags);
    }

    /**
     * 转换为 Map（使用预加载的标签，避免 N+1）
     */
    private Map<String, Object> toMap(TraitEntity trait, List<AssetTagDTO> tags) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", String.valueOf(trait.getId()));
        map.put("name", trait.getName());
        map.put("description", trait.getDescription());
        map.put("content", trait.getContent());
        map.put("enabled", trait.getEnabled());
        map.put("usedByCount", trait.getUsedByCount());
        map.put("createdAt", trait.getCreateTime() != null ? trait.getCreateTime().toString() : null);
        map.put("updatedAt", trait.getUpdateTime() != null ? trait.getUpdateTime().toString() : null);
        PublishStatus status = publishStatusService.getStatusOrDefault(ResourceType.TRAIT, String.valueOf(trait.getId()));
        map.put("publishStatus", status.name());
        map.put("tags", tags);
        return map;
    }
}
