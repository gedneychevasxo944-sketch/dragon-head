package org.dragon.studio.service;

import lombok.RequiredArgsConstructor;
import org.dragon.api.dto.PageResponse;
import org.dragon.datasource.entity.TraitEntity;
import org.dragon.permission.enums.ResourceType;
import org.dragon.asset.service.AssetPublishStatusService;
import org.dragon.asset.service.AssetMemberService;
import org.dragon.store.StoreFactory;
import org.dragon.studio.store.TraitStore;
import org.dragon.util.UserUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TraitService 特征片段服务
 */
@Service
@RequiredArgsConstructor
public class TraitService {

    private final StoreFactory storeFactory;
    private final AssetMemberService assetMemberService;
    private final AssetPublishStatusService publishStatusService;

    private TraitStore getStore() {
        return storeFactory.get(TraitStore.class);
    }

    /**
     * 创建 Trait
     */
    public Map<String, Object> createTrait(Map<String, Object> traitData) {
        TraitEntity trait = TraitEntity.builder()
                .name((String) traitData.get("name"))
                .category((String) traitData.get("category"))
                .description((String) traitData.get("description"))
                .content((String) traitData.get("content"))
                .enabled(true)
                .usedByCount(0)
                .createTime(LocalDateTime.now())
                .build();

        getStore().save(trait);

        // 添加创建者为 Owner
        Long userId = Long.parseLong(UserUtils.getUserId());
        assetMemberService.addOwnerDirectly(ResourceType.TRAIT, String.valueOf(trait.getId()), userId);

        // 初始化发布状态（默认为 DRAFT）
        publishStatusService.initializeStatus(ResourceType.TRAIT, String.valueOf(trait.getId()), String.valueOf(userId));

        return toMap(trait);
    }

    /**
     * 获取 Trait 详情
     */
    public Optional<Map<String, Object>> getTrait(Long id) {
        return getStore().findById(id).map(this::toMap);
    }

    /**
     * 更新 Trait
     */
    public Optional<Map<String, Object>> updateTrait(Long id, Map<String, Object> traitData) {
        return getStore().findById(id).map(existing -> {
            if (traitData.containsKey("name")) {
                existing.setName((String) traitData.get("name"));
            }
            if (traitData.containsKey("category")) {
                existing.setCategory((String) traitData.get("category"));
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
            return toMap(existing);
        });
    }

    /**
     * 删除 Trait
     */
    public boolean deleteTrait(Long id) {
        Optional<TraitEntity> existing = getStore().findById(id);
        if (existing.isPresent()) {
            getStore().delete(id);
            return true;
        }
        return false;
    }

    /**
     * 分页查询 Trait 列表
     */
    public PageResponse<Map<String, Object>> listTraits(int page, int pageSize, String search, String category) {
        List<TraitEntity> allTraits;

        // 按条件过滤
        if (search != null && !search.isBlank()) {
            allTraits = getStore().search(search);
        } else if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
            allTraits = getStore().findByCategory(category);
        } else {
            allTraits = getStore().findAll();
        }

        // 进一步过滤
        if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
            allTraits = allTraits.stream()
                    .filter(t -> category.equals(t.getCategory()))
                    .toList();
        }

        long total = allTraits.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, allTraits.size());
        List<Map<String, Object>> pageData = fromIndex >= allTraits.size()
                ? List.of()
                : allTraits.subList(fromIndex, toIndex).stream().map(this::toMap).toList();

        return PageResponse.of(pageData, total, page, pageSize);
    }

    /**
     * 增加引用计数
     */
    public void incrementUsedByCount(Long traitId) {
        getStore().incrementUsedByCount(traitId);
    }

    /**
     * 减少引用计数
     */
    public void decrementUsedByCount(Long traitId) {
        getStore().decrementUsedByCount(traitId);
    }

    /**
     * 转换为 Map
     */
    public Map<String, Object> toMap(TraitEntity trait) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", String.valueOf(trait.getId()));
        map.put("name", trait.getName());
        map.put("category", trait.getCategory());
        map.put("description", trait.getDescription());
        map.put("content", trait.getContent());
        map.put("enabled", trait.getEnabled());
        map.put("usedByCount", trait.getUsedByCount());
        map.put("createdAt", trait.getCreateTime() != null ? trait.getCreateTime().toString() : null);
        map.put("updatedAt", trait.getUpdateTime() != null ? trait.getUpdateTime().toString() : null);
        return map;
    }
}
