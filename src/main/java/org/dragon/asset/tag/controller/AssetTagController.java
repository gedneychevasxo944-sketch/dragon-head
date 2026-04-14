package org.dragon.asset.tag.controller;

import lombok.extern.slf4j.Slf4j;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.asset.tag.dto.AssetTagDTO;
import org.dragon.asset.tag.service.AssetTagService;
import org.dragon.permission.enums.ResourceType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * AssetTagController 资产标签 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class AssetTagController {

    private final AssetTagService assetTagService;

    public AssetTagController(AssetTagService assetTagService) {
        this.assetTagService = assetTagService;
    }

    /**
     * 给资产添加标签
     */
    @PostMapping("/assets/{resourceType}/{resourceId}/tags")
    public ApiResponse<Void> tagAsset(
            @PathVariable String resourceType,
            @PathVariable String resourceId,
            @RequestBody List<String> tagNames) {
        ResourceType type = ResourceType.valueOf(resourceType.toUpperCase());
        assetTagService.tagAssets(type, resourceId, tagNames);
        return ApiResponse.success(null);
    }

    /**
     * 移除资产标签
     */
    @DeleteMapping("/assets/{resourceType}/{resourceId}/tags/{tagName}")
    public ApiResponse<Void> untagAsset(
            @PathVariable String resourceType,
            @PathVariable String resourceId,
            @PathVariable String tagName) {
        ResourceType type = ResourceType.valueOf(resourceType.toUpperCase());
        assetTagService.untagAsset(type, resourceId, tagName);
        return ApiResponse.success(null);
    }

    /**
     * 获取资产的所有标签
     */
    @GetMapping("/assets/{resourceType}/{resourceId}/tags")
    public ApiResponse<List<AssetTagDTO>> getTagsForAsset(
            @PathVariable String resourceType,
            @PathVariable String resourceId) {
        ResourceType type = ResourceType.valueOf(resourceType.toUpperCase());
        List<AssetTagDTO> tags = assetTagService.getTagsForAsset(type, resourceId);
        return ApiResponse.success(tags);
    }

    /**
     * 按资产类型获取所有标签（去重）
     */
    @GetMapping("/tags")
    public ApiResponse<Set<String>> listTagNames(@RequestParam String resourceType) {
        ResourceType type = ResourceType.valueOf(resourceType.toUpperCase());
        Set<String> tagNames = assetTagService.getTagNamesByResourceType(type);
        return ApiResponse.success(tagNames);
    }
}