package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dragon.application.ToolApplication;
import org.dragon.api.dto.ApiResponse;
import org.dragon.api.dto.PageResponse;
import org.dragon.permission.checker.PermissionChecker;
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

/**
 * ToolController 工具模块 API
 *
 * <p>对应前端 /tools 页面，包含工具查询、注册、版本管理等接口。
 * Base URL: /api/v1/tools
 *
 * @author zhz
 * @version 1.0
 */
@Tag(name = "Tool", description = "工具模块")
@RestController
@RequestMapping("/api/v1/tools")
@RequiredArgsConstructor
public class ToolController {

    private final ToolApplication toolApplication;
    private final PermissionChecker permissionChecker;

    // ==================== 15. Tool CRUD ====================

    /**
     * 15.1 获取工具列表
     * GET /api/v1/tools
     */
    @Operation(summary = "获取工具列表（分页+筛选）")
    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> listTools(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String toolType,
            @RequestParam(required = false) String runtimeStatus,
            @RequestParam(required = false) String category) {
        PageResponse<Map<String, Object>> result = toolApplication.listTools(page, pageSize, search);
        return ApiResponse.success(result);
    }

    /**
     * 15.2 创建工具
     * POST /api/v1/tools
     */
    @Operation(summary = "创建工具")
    @PostMapping
    public ApiResponse<Map<String, Object>> createTool(@RequestBody Map<String, Object> toolData) {
        // 占位：当前工具系统为运行时注册，不支持持久化创建
        return ApiResponse.error(501, "Tool creation via API not yet implemented. Tools are registered at runtime.");
    }

    /**
     * 15.3 获取工具详情
     * GET /api/v1/tools/:id
     */
    @Operation(summary = "获取工具详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getTool(@PathVariable String id) {
        permissionChecker.checkView("TOOL", id);
        return toolApplication.getTool(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Tool not found: " + id));
    }

    /**
     * 15.4 更新工具
     * PUT /api/v1/tools/:id
     */
    @Operation(summary = "更新工具")
    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> updateTool(
            @PathVariable String id,
            @RequestBody Map<String, Object> toolData) {
        permissionChecker.checkEdit("TOOL", id);
        return ApiResponse.error(501, "Tool update via API not yet implemented.");
    }

    /**
     * 15.5 发布工具版本
     * POST /api/v1/tools/:id/publish
     */
    @Operation(summary = "发布工具版本")
    @PostMapping("/{id}/publish")
    public ApiResponse<Map<String, Object>> publishTool(
            @PathVariable String id,
            @RequestBody Map<String, Object> publishData) {
        permissionChecker.checkPermission("TOOL", id, "PUBLISH");
        return ApiResponse.error(501, "Tool version publishing not yet implemented.");
    }

    /**
     * 15.6 删除工具
     * DELETE /api/v1/tools/:id
     */
    @Operation(summary = "删除工具")
    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> deleteTool(@PathVariable String id) {
        permissionChecker.checkDelete("TOOL", id);
        return ApiResponse.error(501, "Tool deletion via API not yet implemented.");
    }
}