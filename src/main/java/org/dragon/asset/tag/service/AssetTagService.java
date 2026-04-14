package org.dragon.asset.tag.service;

import lombok.extern.slf4j.Slf4j;
import org.dragon.asset.tag.dto.AssetTagDTO;
import org.dragon.datasource.entity.AssetTagEntity;
import org.dragon.asset.tag.store.AssetTagStore;
import org.dragon.permission.enums.ResourceType;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AssetTagService 资产标签服务
 * 负责管理所有资产的统一标签
 * <p>
 * 标签数据直接存储在 asset_tag 表（加 resource_type + resource_id 列），
 * 不通过 asset_association 关联。
 */
@Slf4j
@Service
public class AssetTagService {

    private final AssetTagStore assetTagStore;

    public AssetTagService(StoreFactory storeFactory) {
        this.assetTagStore = storeFactory.get(AssetTagStore.class);
    }

    /**
     * 给资产绑定标签（幂等：同一资产的同一标签名不会重复创建）
     */
    public void tagAsset(ResourceType resourceType, String resourceId, String tagName, String color, String description) {
        // 幂等检查：同一资产的同一标签名是否已存在
        if (assetTagStore.exists(resourceType.name(), resourceId, tagName)) {
            log.info("[AssetTagService] Tag already exists for asset: {}:{}, tag={}",
                    resourceType, resourceId, tagName);
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        AssetTagEntity entity = AssetTagEntity.builder()
                .name(tagName)
                .color(color)
                .description(description)
                .resourceType(resourceType.name())
                .resourceId(resourceId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assetTagStore.save(entity);
        log.info("[AssetTagService] Tagged asset: resource={}:{}, tag={}",
                resourceType, resourceId, tagName);
    }

    /**
     * 给资产绑定标签（仅标签名，不含其他元数据）
     */
    public void tagAsset(ResourceType resourceType, String resourceId, String tagName) {
        tagAsset(resourceType, resourceId, tagName, null, null);
    }

    /**
     * 批量给资产绑定标签
     */
    public void tagAssets(ResourceType resourceType, String resourceId, List<String> tagNames) {
        for (String tagName : tagNames) {
            tagAsset(resourceType, resourceId, tagName);
        }
    }

    /**
     * 移除资产标签
     */
    public void untagAsset(ResourceType resourceType, String resourceId, String tagName) {
        assetTagStore.delete(resourceType.name(), resourceId, tagName);
        log.info("[AssetTagService] Untagged asset: resource={}:{}, tag={}",
                resourceType, resourceId, tagName);
    }

    /**
     * 获取资产的所有标签
     */
    public List<AssetTagDTO> getTagsForAsset(ResourceType resourceType, String resourceId) {
        return assetTagStore.findByResource(resourceType.name(), resourceId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 批量获取多个资产的标签（一次查询，避免 N+1）
     *
     * @return Map&lt;resourceId, List&lt;AssetTagDTO&gt;&gt;
     */
    public Map<String, List<AssetTagDTO>> getTagsForAssets(ResourceType resourceType, List<String> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return new HashMap<>();
        }

        List<AssetTagEntity> all = assetTagStore.findByResources(resourceType.name(), resourceIds);

        Map<String, List<AssetTagDTO>> result = new HashMap<>();
        for (AssetTagEntity e : all) {
            result.computeIfAbsent(e.getResourceId(), k -> new ArrayList<>()).add(toDTO(e));
        }
        return result;
    }

    /**
     * 按资产类型获取所有标签（去重，返回标签名集合）
     */
    public Set<String> getTagNamesByResourceType(ResourceType resourceType) {
        return assetTagStore.findTagNamesByResourceType(resourceType.name());
    }

    /**
     * 查询某资产是否拥有某标签
     */
    public boolean hasTag(ResourceType resourceType, String resourceId, String tagName) {
        return assetTagStore.exists(resourceType.name(), resourceId, tagName);
    }

    /**
     * 根据标签名称和资产类型查询（用于 TraitService 等按标签名过滤）
     */
    public List<AssetTagEntity> findByTagNameAndResourceType(String tagName, String resourceType) {
        return assetTagStore.findByTagNameAndResourceType(tagName, resourceType);
    }

    private AssetTagDTO toDTO(AssetTagEntity entity) {
        return AssetTagDTO.builder()
                .name(entity.getName())
                .color(entity.getColor())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}