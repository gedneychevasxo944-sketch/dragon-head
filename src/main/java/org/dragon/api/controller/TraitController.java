package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.permission.checker.PermissionChecker;
import org.dragon.trait.service.TraitService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * TraitController Trait 管理 API
 *
 * <p>对应前端 /studio/traits 页面，包含 Trait CRUD 功能。
 * Base URL: /api/v1/traits
 *
 * @author yijunw
 * @version 1.0
 */
@Tag(name = "Trait", description = "特征片段管理")
@RestController
@RequestMapping("/api/v1/traits")
@RequiredArgsConstructor
public class TraitController {

    private final TraitService traitService;
    private final PermissionChecker permissionChecker;

    /**
     * 获取 Trait 列表
     * GET /api/v1/traits
     */
    @Operation(summary = "获取 Trait 列表")
    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> listTraits(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String publishStatus) {
        return ApiResponse.success(traitService.listTraits(page, pageSize, search, category, publishStatus));
    }

    /**
     * 创建 Trait
     * POST /api/v1/traits
     */
    @Operation(summary = "创建 Trait")
    @PostMapping
    public ApiResponse<Map<String, Object>> createTrait(@RequestBody Map<String, Object> traitData) {
        return ApiResponse.success(traitService.createTrait(traitData));
    }

    /**
     * 获取 Trait 详情
     * GET /api/v1/traits/:id
     */
    @Operation(summary = "获取 Trait 详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getTrait(@PathVariable String id) {
        permissionChecker.checkView("TRAIT", id);
        Optional<Map<String, Object>> trait = traitService.getTrait(id);
        return trait.map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Trait not found: " + id));
    }

    /**
     * 更新 Trait
     * PUT /api/v1/traits/:id
     */
    @Operation(summary = "更新 Trait")
    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> updateTrait(
            @PathVariable String id,
            @RequestBody Map<String, Object> traitData) {
        permissionChecker.checkEdit("TRAIT", id);
        Optional<Map<String, Object>> updated = traitService.updateTrait(id, traitData);
        return updated.map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Trait not found: " + id));
    }

    /**
     * 删除 Trait
     * DELETE /api/v1/traits/:id
     */
    @Operation(summary = "删除 Trait")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTrait(@PathVariable String id) {
        permissionChecker.checkDelete("TRAIT", id);
        boolean deleted = traitService.deleteTrait(id);
        return deleted ? ApiResponse.success() : ApiResponse.error(404, "Trait not found: " + id);
    }
}