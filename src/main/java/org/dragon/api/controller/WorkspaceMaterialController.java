package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.api.controller.dto.MaterialResponse;
import org.dragon.material.Material;
import org.dragon.workspace.material.WorkspaceMaterialService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * WorkspaceMaterialController 工作空间物料模块 API
 *
 * <p>对应前端 Workspace 详情页物料 Tab
 * Base URL: /api/v1/workspaces/{workspaceId}/materials
 *
 * @author dragon
 * @version 1.0
 */
@Slf4j
@Tag(name = "Workspace Material", description = "工作空间物料管理")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/materials")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class WorkspaceMaterialController {

    private final WorkspaceMaterialService workspaceMaterialService;

    /**
     * 获取物料列表
     * GET /api/v1/workspaces/{workspaceId}/materials
     */
    @Operation(summary = "获取物料列表")
    @GetMapping
    public ApiResponse<List<MaterialResponse>> listMaterials(@PathVariable String workspaceId) {
        List<Material> materials = workspaceMaterialService.listByWorkspace(workspaceId);
        List<MaterialResponse> response = materials.stream()
                .map(MaterialResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(response);
    }

    /**
     * 获取物料详情
     * GET /api/v1/workspaces/{workspaceId}/materials/{materialId}
     */
    @Operation(summary = "获取物料详情")
    @GetMapping("/{materialId}")
    public ApiResponse<MaterialResponse> getMaterial(
            @PathVariable String workspaceId,
            @PathVariable String materialId) {
        return workspaceMaterialService.get(materialId)
                .map(MaterialResponse::from)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Material not found: " + materialId));
    }

    /**
     * 上传物料
     * POST /api/v1/workspaces/{workspaceId}/materials
     */
    @Operation(summary = "上传物料文件")
    @PostMapping
    public ApiResponse<MaterialResponse> uploadMaterial(
            @PathVariable String workspaceId,
            @RequestParam("file") MultipartFile file) throws IOException {
        String uploader = getCurrentUserId();
        Material material = workspaceMaterialService.upload(
                workspaceId,
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType(),
                uploader);
        log.info("[WorkspaceMaterialController] Uploaded material: {} to workspace: {}", material.getId(), workspaceId);
        return ApiResponse.success(MaterialResponse.from(material));
    }

    /**
     * 删除物料
     * DELETE /api/v1/workspaces/{workspaceId}/materials/{materialId}
     */
    @Operation(summary = "删除物料")
    @DeleteMapping("/{materialId}")
    public ApiResponse<Map<String, Object>> deleteMaterial(
            @PathVariable String workspaceId,
            @PathVariable String materialId) {
        workspaceMaterialService.delete(materialId);
        log.info("[WorkspaceMaterialController] Deleted material: {} from workspace: {}", materialId, workspaceId);
        return ApiResponse.success(Map.of("success", true));
    }

    /**
     * 获取当前用户 ID（临时实现，后续从 SecurityContext 获取）
     */
    private String getCurrentUserId() {
        try {
            return org.dragon.util.UserUtils.getUserId();
        } catch (Exception e) {
            return "anonymous";
        }
    }
}
