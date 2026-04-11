package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.application.StudioApplication;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.character.Character;
import org.dragon.character.service.CharacterService;
import org.dragon.character.service.CharacterTemplateService;
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
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * StudioController Studio 模块 API
 *
 * <p>对应前端 /studio 页面，包含 Character、Trait、Template、Deployment 相关接口。
 * Base URL: /api/v1/studio
 *
 * @author zhz
 * @version 1.0
 */
@Tag(name = "Studio", description = "Studio 模块：角色、特征、模板、派驻")
@RestController
@RequestMapping("/api/v1/studio")
@RequiredArgsConstructor
public class StudioController {

    private final StudioApplication studioApplication;
    private final CharacterService characterService;
    private final CharacterTemplateService characterTemplateService;
    private final DeploymentService deploymentService;
    private final TraitService traitService;
    private final PermissionChecker permissionChecker;

    // ==================== 1. Character（角色）====================

    /**
     * 1.1 获取角色列表
     * GET /api/v1/studio/characters
     */

    @Operation(summary = "获取角色列表（分页+筛选）")
    @GetMapping("/characters")
    public ApiResponse<PageResponse<Character>> listCharacters(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source) {
        PageResponse<Character> result = characterService.listCharacters(page, pageSize, search, status, source);
        return ApiResponse.success(result);
    }

    /**
     * 1.2 创建角色
     * POST /api/v1/studio/characters
     */
    @Operation(summary = "创建角色")
    @PostMapping("/characters")
    public ApiResponse<Character> createCharacter(@RequestBody Character character) {
        Character created = characterService.createCharacter(character);
        return ApiResponse.success(created);
    }

    /**
     * 1.3 获取角色详情
     * GET /api/v1/studio/characters/:id
     */
    @Operation(summary = "获取角色详情")
    @GetMapping("/characters/{id}")
    public ApiResponse<Character> getCharacter(@PathVariable String id) {
        permissionChecker.checkView("CHARACTER", id);
        return characterService.getCharacter(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Character not found: " + id));
    }

    /**
     * 1.4 更新角色
     * PUT /api/v1/studio/characters/:id
     */
    @Operation(summary = "更新角色")
    @PutMapping("/characters/{id}")
    public ApiResponse<Character> updateCharacter(
            @PathVariable String id,
            @RequestBody Character character) {
        permissionChecker.checkEdit("CHARACTER", id);
        Character updated = characterService.updateCharacter(id, character);
        return ApiResponse.success(updated);
    }

    /**
     * 1.5 删除角色
     * DELETE /api/v1/studio/characters/:id
     */
    @Operation(summary = "删除角色")
    @DeleteMapping("/characters/{id}")
    public ApiResponse<Map<String, Object>> deleteCharacter(@PathVariable String id) {
        permissionChecker.checkDelete("CHARACTER", id);
        characterService.deleteCharacter(id);
        return ApiResponse.success(Map.of("success", true));
    }

    /**
     * 1.6 获取 Studio 统计数据
     * GET /api/v1/studio/stats
     */
    @Operation(summary = "获取 Studio 统计数据")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStudioStats() {
        Map<String, Object> stats = characterService.getCharacterStats();
        return ApiResponse.success(stats);
    }

    /**
     * 1.7 独立运行角色（发送消息）
     * POST /api/v1/studio/characters/:id/run
     */
    @Operation(summary = "独立运行角色 - 发送消息")
    @PostMapping("/characters/{id}/run")
    public ApiResponse<Map<String, Object>> runCharacter(
            @PathVariable String id,
            @RequestBody RunCharacterRequest request) {
        permissionChecker.checkUse("CHARACTER", id);
        Map<String, Object> result = characterService.runCharacter(id, request.getMessage(), request.getSessionId());
        return ApiResponse.success(result);
    }

    // ==================== 2. Trait（特征片段）====================

    /**
     * 2.1 获取 Trait 列表
     * GET /api/v1/studio/traits
     */
    @Operation(summary = "获取 Trait 列表")
    @GetMapping("/traits")
    public ApiResponse<PageResponse<Map<String, Object>>> listTraits(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category) {
        return ApiResponse.success(traitService.listTraits(page, pageSize, search, category));
    }

    /**
     * 2.2 创建 Trait
     * POST /api/v1/studio/traits
     */
    @Operation(summary = "创建 Trait")
    @PostMapping("/traits")
    public ApiResponse<Map<String, Object>> createTrait(@RequestBody Map<String, Object> traitData) {
        return ApiResponse.success(traitService.createTrait(traitData));
    }

    /**
     * 2.3 获取 Trait 详情
     * GET /api/v1/studio/traits/:id
     */
    @Operation(summary = "获取 Trait 详情")
    @GetMapping("/traits/{id}")
    public ApiResponse<Map<String, Object>> getTrait(@PathVariable String id) {
        permissionChecker.checkView("TRAIT", id);
        Optional<Map<String, Object>> trait = traitService.getTrait(Long.parseLong(id));
        return trait.map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Trait not found: " + id));
    }

    /**
     * 2.4 更新 Trait
     * PUT /api/v1/studio/traits/:id
     */
    @Operation(summary = "更新 Trait")
    @PutMapping("/traits/{id}")
    public ApiResponse<Map<String, Object>> updateTrait(
            @PathVariable String id,
            @RequestBody Map<String, Object> traitData) {
        permissionChecker.checkEdit("TRAIT", id);
        Optional<Map<String, Object>> updated = traitService.updateTrait(Long.parseLong(id), traitData);
        return updated.map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Trait not found: " + id));
    }

    /**
     * 2.5 删除 Trait
     * DELETE /api/v1/studio/traits/:id
     */
    @Operation(summary = "删除 Trait")
    @DeleteMapping("/traits/{id}")
    public ApiResponse<Void> deleteTrait(@PathVariable String id) {
        permissionChecker.checkDelete("TRAIT", id);
        boolean deleted = traitService.deleteTrait(Long.parseLong(id));
        return deleted ? ApiResponse.success() : ApiResponse.error(404, "Trait not found: " + id);
    }

    // ==================== 3. Template（内置模板）====================

    /**
     * 3.1 获取模板列表
     * GET /api/v1/studio/templates
     */
    @Operation(summary = "获取内置角色模板列表")
    @GetMapping("/templates")
    public ApiResponse<List<Map<String, Object>>> listTemplates(
            @RequestParam(required = false) String category) {
        return ApiResponse.success(characterTemplateService.listTemplates(category));
    }

    /**
     * 3.2 从模板创建角色
     * POST /api/v1/studio/templates/:id/derive
     */
    @Operation(summary = "从模板派生创建角色")
    @PostMapping("/templates/{id}/derive")
    public ApiResponse<Character> deriveCharacterFromTemplate(
            @PathVariable String id,
            @RequestBody DeriveTemplateRequest request) {
        permissionChecker.checkEdit("TEMPLATE", id);
        Character created = characterTemplateService.deriveCharacterFromTemplate(
                id, request.getName(), request.getDescription());
        return ApiResponse.success(created);
    }

    // ==================== 4. Deployment（派驻记录）====================

    /**
     * 4.1 获取派驻记录列表
     * GET /api/v1/studio/deployments
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
     * 4.2 派驻角色到 Workspace
     * POST /api/v1/studio/deployments
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
     * 4.3 撤销派驻
     * DELETE /api/v1/studio/deployments/:id
     */
    @Operation(summary = "撤销派驻")
    @DeleteMapping("/deployments/{id}")
    public ApiResponse<Map<String, Object>> undeployCharacter(@PathVariable String id) {
        deploymentService.undeployCharacter(id);
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

    /** 从模板派生请求 */
    @Data
    public static class DeriveTemplateRequest {
        private String name;
        private String description;
    }
}