package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.api.controller.dto.CharacterDetailDTO;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.asset.service.AssetAssociationService;
import org.dragon.character.Character;
import org.dragon.character.service.CharacterService;
import org.dragon.permission.checker.PermissionChecker;
import org.dragon.trait.service.TraitService;
import org.dragon.workspace.DeploymentService;
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
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * CharacterController 角色管理 API
 *
 * <p>对应前端 /studio/characters 页面，包含 Character CRUD、派驻等功能。
 * Base URL: /api/v1/characters
 *
 * @author yijunw
 * @version 1.0
 */
@Tag(name = "Character", description = "角色管理")
@RestController
@RequestMapping("/api/v1/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;
    private final DeploymentService deploymentService;
    private final TraitService traitService;
    private final PermissionChecker permissionChecker;
    private final AssetAssociationService assetAssociationService;

    // ==================== Character CRUD ====================

    /**
     * 创建角色
     * POST /api/v1/characters
     */
    @Operation(summary = "创建角色")
    @PostMapping
    public ApiResponse<Character> createCharacter(@RequestBody Character character) {
        Character created = characterService.createCharacter(character);
        return ApiResponse.success(created);
    }

    /**
     * 获取角色详情
     * GET /api/v1/characters/:id
     */
    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    public ApiResponse<CharacterDetailDTO> getCharacter(@PathVariable String id) {
        permissionChecker.checkView("CHARACTER", id);
        return characterService.getCharacterDetail(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Character not found: " + id));
    }

    /**
     * 更新角色
     * PUT /api/v1/characters/:id
     */
    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    public ApiResponse<Character> updateCharacter(
            @PathVariable String id,
            @RequestBody Character character) {
        permissionChecker.checkEdit("CHARACTER", id);
        Character updated = characterService.updateCharacter(id, character);
        return ApiResponse.success(updated);
    }

    /**
     * 删除角色
     * DELETE /api/v1/characters/:id
     */
    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> deleteCharacter(@PathVariable String id) {
        permissionChecker.checkDelete("CHARACTER", id);
        characterService.deleteCharacter(id);
        return ApiResponse.success(Map.of("success", true));
    }

    /**
     * 独立运行角色（发送消息）
     * POST /api/v1/characters/:id/run
     */
    @Operation(summary = "独立运行角色 - 发送消息")
    @PostMapping("/{id}/run")
    public ApiResponse<Map<String, Object>> runCharacter(
            @PathVariable String id,
            @RequestBody RunCharacterRequest request) {
        permissionChecker.checkUse("CHARACTER", id);
        Map<String, Object> result = characterService.runCharacter(id, request.getMessage(), request.getSessionId());
        return ApiResponse.success(result);
    }

    /**
     * 获取角色的 Traits
     * GET /api/v1/characters/:id/traits
     */
    @Operation(summary = "获取角色的 Traits")
    @GetMapping("/{id}/traits")
    public ApiResponse<List<Map<String, Object>>> getCharacterTraits(@PathVariable String id) {
        permissionChecker.checkView("CHARACTER", id);
        List<String> traitIds = assetAssociationService.getTraitsForCharacter(id);
        List<Map<String, Object>> traits = traitIds.stream()
                .map(traitService::getTrait)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        return ApiResponse.success(traits);
    }

    // ==================== Deployment（派驻记录）====================

    /**
     * 获取派驻记录列表
     * GET /api/v1/characters/deployments
     */
    @Operation(summary = "获取派驻记录列表")
    @GetMapping("/deployments")
    public ApiResponse<PageResponse<Map<String, Object>>> listDeployments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String characterId,
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) String status) {
        List<Map<String, Object>> all = deploymentService.listDeployments(page, pageSize, characterId, workspaceId);
        return ApiResponse.success(PageResponse.of(all, all.size(), page, pageSize));
    }

    /**
     * 派驻角色到 Workspace
     * POST /api/v1/characters/deployments
     */
    @Operation(summary = "派驻角色到 Workspace")
    @PostMapping("/deployments")
    public ApiResponse<Map<String, Object>> deployCharacter(@RequestBody DeployRequest request) {
        Map<String, Object> result = deploymentService.deployCharacter(
                request.getCharacterId(),
                request.getWorkspaceId(),
                request.getRole(),
                request.getPosition(),
                request.getLevel());
        return ApiResponse.success(result);
    }

    /**
     * 撤销派驻
     * DELETE /api/v1/characters/deployments/:deploymentId
     */
    @Operation(summary = "撤销派驻")
    @DeleteMapping("/deployments/{deploymentId}")
    public ApiResponse<Map<String, Object>> undeployCharacter(@PathVariable String deploymentId) {
        deploymentService.undeployCharacter(deploymentId);
        return ApiResponse.success(Map.of("success", true));
    }

    // ==================== 请求体 DTO ====================

    /** 角色运行请求 */
    @Data
    public static class RunCharacterRequest {
        /** 用户消息 */
        private String message;
        /** 会话 ID（可选） */
        private String sessionId;
    }

    /** 派驻请求 */
    @Data
    public static class DeployRequest {
        private String characterId;
        private String workspaceId;
        private String role;
        private String position;
        private Integer level;
    }
}