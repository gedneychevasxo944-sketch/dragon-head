package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.asset.dto.AssetAssociationDTO;
import org.dragon.asset.dto.CreateAssociationRequest;
import org.dragon.asset.enums.AssociationType;
import org.dragon.asset.service.AssetAssociationService;
import org.dragon.permission.enums.ResourceType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * AssetAssociationController 资产关联管理 API
 *
 * <p>提供资产关联的创建、删除、查询等接口。
 * Base URL: /api/v1/asset-associations
 */
@Tag(name = "AssetAssociation", description = "资产关联管理")
@RestController
@RequestMapping("/api/v1/asset-associations")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AssetAssociationController {

    private final AssetAssociationService assetAssociationService;

    /**
     * 创建资产关联
     * POST /api/v1/asset-associations
     */
    @Operation(summary = "创建资产关联")
    @PostMapping
    public ApiResponse<Map<String, Object>> createAssociation(@RequestBody CreateAssociationRequest request) {
        assetAssociationService.createAssociation(
                request.getAssociationType(),
                request.getSourceType(),
                request.getSourceId(),
                request.getTargetType(),
                request.getTargetId()
        );
        return ApiResponse.success(Map.of("success", true, "message", "关联创建成功"));
    }

    /**
     * 删除资产关联
     * DELETE /api/v1/asset-associations
     */
    @Operation(summary = "删除资产关联")
    @DeleteMapping
    public ApiResponse<Map<String, Object>> removeAssociation(@RequestBody CreateAssociationRequest request) {
        assetAssociationService.removeAssociation(
                request.getAssociationType(),
                request.getSourceType(),
                request.getSourceId(),
                request.getTargetType(),
                request.getTargetId()
        );
        return ApiResponse.success(Map.of("success", true, "message", "关联删除成功"));
    }

    /**
     * 按源资产查询关联
     * GET /api/v1/asset-associations/source/{type}/{id}
     */
    @Operation(summary = "按源资产查询关联")
    @GetMapping("/source/{type}/{id}")
    public ApiResponse<List<AssetAssociationDTO>> findBySource(
            @PathVariable String type,
            @PathVariable String id,
            @RequestParam(defaultValue = "CHARACTER_WORKSPACE") String associationType) {
        ResourceType resourceType = ResourceType.valueOf(type.toUpperCase());
        AssociationType assocType = AssociationType.valueOf(associationType.toUpperCase());
        List<AssetAssociationDTO> associations = assetAssociationService.findBySource(assocType, resourceType, id);
        return ApiResponse.success(associations);
    }

    /**
     * 按目标资产查询关联
     * GET /api/v1/asset-associations/target/{type}/{id}
     */
    @Operation(summary = "按目标资产查询关联")
    @GetMapping("/target/{type}/{id}")
    public ApiResponse<List<AssetAssociationDTO>> findByTarget(
            @PathVariable String type,
            @PathVariable String id,
            @RequestParam(defaultValue = "CHARACTER_WORKSPACE") String associationType) {
        ResourceType resourceType = ResourceType.valueOf(type.toUpperCase());
        AssociationType assocType = AssociationType.valueOf(associationType.toUpperCase());
        List<AssetAssociationDTO> associations = assetAssociationService.findByTarget(assocType, resourceType, id);
        return ApiResponse.success(associations);
    }

    /**
     * 获取 Workspace 下的所有 Character
     * GET /api/v1/asset-associations/workspace/{workspaceId}/characters
     */
    @Operation(summary = "获取 Workspace 下的所有 Character")
    @GetMapping("/workspace/{workspaceId}/characters")
    public ApiResponse<List<String>> getCharactersInWorkspace(@PathVariable String workspaceId) {
        List<String> characterIds = assetAssociationService.getCharactersInWorkspace(workspaceId);
        return ApiResponse.success(characterIds);
    }

    /**
     * 获取 Character 关联的所有 Memory
     * GET /api/v1/asset-associations/character/{characterId}/memories
     */
    @Operation(summary = "获取 Character 关联的所有 Memory")
    @GetMapping("/character/{characterId}/memories")
    public ApiResponse<List<String>> getMemoriesForCharacter(@PathVariable String characterId) {
        List<String> memoryIds = assetAssociationService.getMemoriesForCharacter(characterId);
        return ApiResponse.success(memoryIds);
    }

    /**
     * 获取 Workspace 关联的所有 Memory
     * GET /api/v1/asset-associations/workspace/{workspaceId}/memories
     */
    @Operation(summary = "获取 Workspace 关联的所有 Memory")
    @GetMapping("/workspace/{workspaceId}/memories")
    public ApiResponse<List<String>> getMemoriesForWorkspace(@PathVariable String workspaceId) {
        List<String> memoryIds = assetAssociationService.getMemoriesForWorkspace(workspaceId);
        return ApiResponse.success(memoryIds);
    }

    /**
     * 获取 Workspace 关联的所有 Observer
     * GET /api/v1/asset-associations/workspace/{workspaceId}/observers
     */
    @Operation(summary = "获取 Workspace 关联的所有 Observer")
    @GetMapping("/workspace/{workspaceId}/observers")
    public ApiResponse<List<String>> getObserversForWorkspace(@PathVariable String workspaceId) {
        List<String> observerIds = assetAssociationService.getObserversForWorkspace(workspaceId);
        return ApiResponse.success(observerIds);
    }

    /**
     * 获取 Skill 关联的所有 Tool
     * GET /api/v1/asset-associations/skill/{skillId}/tools
     */
    @Operation(summary = "获取 Skill 关联的所有 Tool")
    @GetMapping("/skill/{skillId}/tools")
    public ApiResponse<List<String>> getToolsForSkill(@PathVariable String skillId) {
        List<String> toolIds = assetAssociationService.getToolsForSkill(skillId);
        return ApiResponse.success(toolIds);
    }

    /**
     * 启用资产关联（通过五元组定位）
     * PUT /api/v1/asset-associations/enable
     */
    @Operation(summary = "启用资产关联")
    @PutMapping("/enable")
    public ApiResponse<Map<String, Object>> enableAssociation(@RequestBody CreateAssociationRequest request) {
        assetAssociationService.enableAssociation(
                request.getAssociationType(),
                request.getSourceType(),
                request.getSourceId(),
                request.getTargetType(),
                request.getTargetId()
        );
        return ApiResponse.success(Map.of("success", true, "message", "关联已启用"));
    }

    /**
     * 禁用资产关联（保留记录但不生效，通过五元组定位）
     * PUT /api/v1/asset-associations/disable
     */
    @Operation(summary = "禁用资产关联")
    @PutMapping("/disable")
    public ApiResponse<Map<String, Object>> disableAssociation(@RequestBody CreateAssociationRequest request) {
        assetAssociationService.disableAssociation(
                request.getAssociationType(),
                request.getSourceType(),
                request.getSourceId(),
                request.getTargetType(),
                request.getTargetId()
        );
        return ApiResponse.success(Map.of("success", true, "message", "关联已禁用"));
    }

    /**
     * 检查关联是否存在
     * GET /api/v1/asset-associations/exists
     */
    @Operation(summary = "检查关联是否存在")
    @GetMapping("/exists")
    public ApiResponse<Map<String, Object>> exists(
            @RequestParam String associationType,
            @RequestParam String sourceType,
            @RequestParam String sourceId,
            @RequestParam String targetType,
            @RequestParam String targetId) {
        boolean exists = assetAssociationService.exists(
                AssociationType.valueOf(associationType.toUpperCase()),
                ResourceType.valueOf(sourceType.toUpperCase()),
                sourceId,
                ResourceType.valueOf(targetType.toUpperCase()),
                targetId
        );
        return ApiResponse.success(Map.of("exists", exists));
    }
}
