package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.commonsense.CommonSense;
import org.dragon.commonsense.CommonSenseFolder;
import org.dragon.commonsense.CommonSenseService;
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
import java.util.Optional;

/**
 * CommonSenseController CommonSense 管理 API
 *
 * <p>对应前端 /studio/commonsense 页面，包含 CommonSense 和文件夹的 CRUD 功能。
 * Base URL: /api/v1/commonsense
 *
 * @author yijunw
 * @version 1.0
 */
@Tag(name = "CommonSense", description = "常识管理")
@RestController
@RequestMapping("/api/v1/commonsense")
@RequiredArgsConstructor
public class CommonSenseController {

    private final CommonSenseService commonSenseService;

    // ==================== CommonSense CRUD ====================

    /**
     * 获取 CommonSense 列表
     * GET /api/v1/commonsense
     */
    @Operation(summary = "获取 CommonSense 列表")
    @GetMapping
    public ApiResponse<List<CommonSense>> listCommonSenses(
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) String folderId,
            @RequestParam(required = false) String category) {
        List<CommonSense> result;
        if (folderId != null && !folderId.isBlank()) {
            result = commonSenseService.getByFolder(folderId);
        } else if (workspaceId != null && !workspaceId.isBlank()) {
            if (category != null && !category.isBlank()) {
                result = commonSenseService.getByCategory(workspaceId,
                        CommonSense.Category.valueOf(category.toUpperCase()));
            } else {
                result = commonSenseService.getByWorkspace(workspaceId);
            }
        } else {
            result = List.of();
        }
        return ApiResponse.success(result);
    }

    /**
     * 获取 CommonSense 详情
     * GET /api/v1/commonsense/:id
     */
    @Operation(summary = "获取 CommonSense 详情")
    @GetMapping("/{id}")
    public ApiResponse<CommonSense> getCommonSense(@PathVariable String id) {
        Optional<CommonSense> commonSense = commonSenseService.get(id);
        return commonSense.map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "CommonSense not found: " + id));
    }

    /**
     * 创建 CommonSense
     * POST /api/v1/commonsense
     */
    @Operation(summary = "创建 CommonSense")
    @PostMapping
    public ApiResponse<CommonSense> createCommonSense(@RequestBody CommonSense commonSense) {
        CommonSense created = commonSenseService.save(commonSense);
        return ApiResponse.success(created);
    }

    /**
     * 更新 CommonSense
     * PUT /api/v1/commonsense/:id
     */
    @Operation(summary = "更新 CommonSense")
    @PutMapping("/{id}")
    public ApiResponse<CommonSense> updateCommonSense(
            @PathVariable String id,
            @RequestBody CommonSense commonSense) {
        commonSense.setId(id);
        CommonSense updated = commonSenseService.save(commonSense);
        return ApiResponse.success(updated);
    }

    /**
     * 删除 CommonSense
     * DELETE /api/v1/commonsense/:id
     */
    @Operation(summary = "删除 CommonSense")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCommonSense(@PathVariable String id) {
        boolean deleted = commonSenseService.delete(id);
        return deleted ? ApiResponse.success() : ApiResponse.error(404, "CommonSense not found: " + id);
    }

    // ==================== Folder 管理 ====================

    /**
     * 获取文件夹列表
     * GET /api/v1/commonsense/folders
     */
    @Operation(summary = "获取文件夹列表")
    @GetMapping("/folders")
    public ApiResponse<List<CommonSenseFolder>> listFolders(
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) String parentId) {
        List<CommonSenseFolder> result;
        if (parentId != null && !parentId.isBlank()) {
            result = commonSenseService.getChildFolders(parentId);
        } else if (workspaceId != null && !workspaceId.isBlank()) {
            result = commonSenseService.getRootFolders(workspaceId);
        } else {
            result = List.of();
        }
        return ApiResponse.success(result);
    }

    /**
     * 获取文件夹详情
     * GET /api/v1/commonsense/folders/:id
     */
    @Operation(summary = "获取文件夹详情")
    @GetMapping("/folders/{id}")
    public ApiResponse<CommonSenseFolder> getFolder(@PathVariable String id) {
        Optional<CommonSenseFolder> folder = commonSenseService.getFolder(id);
        return folder.map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Folder not found: " + id));
    }

    /**
     * 创建文件夹
     * POST /api/v1/commonsense/folders
     */
    @Operation(summary = "创建文件夹")
    @PostMapping("/folders")
    public ApiResponse<CommonSenseFolder> createFolder(@RequestBody CommonSenseFolder folder) {
        CommonSenseFolder created = commonSenseService.createFolder(folder);
        return ApiResponse.success(created);
    }

    /**
     * 删除文件夹
     * DELETE /api/v1/commonsense/folders/:id
     */
    @Operation(summary = "删除文件夹")
    @DeleteMapping("/folders/{id}")
    public ApiResponse<Void> deleteFolder(@PathVariable String id) {
        boolean deleted = commonSenseService.deleteFolder(id);
        return deleted ? ApiResponse.success() : ApiResponse.error(404, "Folder not found: " + id);
    }
}