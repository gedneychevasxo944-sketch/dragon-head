package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dragon.application.ToolApplication;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.tool.dto.ToolActionLog;
import org.dragon.tool.dto.ToolRegisterRequest;
import org.dragon.tool.dto.ToolVO;
import org.dragon.tool.dto.ToolVersionVO;
import org.dragon.permission.checker.PermissionChecker;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * ToolController 工具模块 API
 *
 * <p>对应前端 /tools 页面，包含工具查询、注册、版本管理等接口。
 * Base URL: /api/v1/tools
 *
 * @author ypf
 * @version 1.0
 */
@Tag(name = "Tool", description = "工具模块")
@RestController
@RequestMapping("/api/v1/tools")
@RequiredArgsConstructor
public class ToolController {

    private final ToolApplication toolApplication;
    private final PermissionChecker permissionChecker;

    // ==================== Tool CRUD ====================

    /**
     * 获取工具列表（分页+筛选）
     * GET /api/v1/tools
     */
    @Operation(summary = "获取工具列表（分页+筛选）")
    @GetMapping
    public ApiResponse<PageResponse<ToolVO>> listTools(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String toolType,
            @RequestParam(required = false) String runtimeStatus,
            @RequestParam(required = false) String category) {
        PageResponse<ToolVO> result = toolApplication.listTools(
                page, pageSize, search, visibility, toolType, runtimeStatus, category);
        return ApiResponse.success(result);
    }

    /**
     * 创建工具
     * POST /api/v1/tools (multipart/form-data)
     */
    @Operation(summary = "创建工具")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ToolVO> createTool(
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestPart("data") ToolRegisterRequest request) {
        ToolVO response = toolApplication.create(files, request);
        return ApiResponse.success(response);
    }

    /**
     * 获取工具详情
     * GET /api/v1/tools/{id}
     */
    @Operation(summary = "获取工具详情")
    @GetMapping("/{id}")
    public ApiResponse<ToolVO> getTool(@PathVariable String id) {
        permissionChecker.checkView("TOOL", id);
        ToolVO response = toolApplication.getDetail(id);
        return ApiResponse.success(response);
    }

    /**
     * 更新工具
     * PUT /api/v1/tools/{id} (multipart/form-data)
     */
    @Operation(summary = "更新工具")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ToolVO> updateTool(
            @PathVariable String id,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestPart("data") ToolRegisterRequest request) {
        permissionChecker.checkEdit("TOOL", id);
        ToolVO response = toolApplication.update(id, files, request);
        return ApiResponse.success(response);
    }

    /**
     * 发布工具版本
     * POST /api/v1/tools/{id}/publish
     */
    @Operation(summary = "发布工具版本")
    @PostMapping("/{id}/publish")
    public ApiResponse<ToolVO> publishTool(
            @PathVariable String id,
            @RequestParam int version,
            @RequestParam(required = false) String releaseNote) {
        permissionChecker.checkPermission("TOOL", id, "PUBLISH");
        ToolVO response = toolApplication.publishTool(id, version, releaseNote);
        return ApiResponse.success(response);
    }

    /**
     * 删除工具
     * DELETE /api/v1/tools/{id}
     */
    @Operation(summary = "删除工具")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTool(@PathVariable String id) {
        permissionChecker.checkDelete("TOOL", id);
        toolApplication.deleteTool(id);
        return ApiResponse.success(null);
    }

    /**
     * 禁用工具
     * POST /api/v1/tools/{id}/disable
     */
    @Operation(summary = "禁用工具")
    @PostMapping("/{id}/disable")
    public ApiResponse<Void> disableTool(@PathVariable String id) {
        toolApplication.disableTool(id);
        return ApiResponse.success(null);
    }

    /**
     * 启用工具
     * POST /api/v1/tools/{id}/enable
     */
    @Operation(summary = "启用工具")
    @PostMapping("/{id}/enable")
    public ApiResponse<Void> enableTool(@PathVariable String id) {
        toolApplication.enableTool(id);
        return ApiResponse.success(null);
    }

    /**
     * 获取工具版本列表
     * GET /api/v1/tools/{id}/versions
     */
    @Operation(summary = "获取工具版本列表")
    @GetMapping("/{id}/versions")
    public ApiResponse<List<ToolVersionVO>> listVersions(@PathVariable String id) {
        List<ToolVersionVO> result = toolApplication.listVersions(id);
        return ApiResponse.success(result);
    }

    /**
     * 获取工具操作日志
     * GET /api/v1/tools/{id}/action-logs
     */
    @Operation(summary = "获取工具操作日志")
    @GetMapping("/{id}/action-logs")
    public ApiResponse<PageResponse<ToolActionLog>> getActionLogs(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<ToolActionLog> result = toolApplication.getActionLogs(id, page, pageSize);
        return ApiResponse.success(result);
    }
}
