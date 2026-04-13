package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.character.Character;
import org.dragon.character.service.CharacterTemplateService;
import org.dragon.permission.checker.PermissionChecker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * TemplateController 模板管理 API
 *
 * <p>对应前端 /studio/templates 页面，包含内置角色模板的查询和派生功能。
 * Base URL: /api/v1/templates
 *
 * @author yijunw
 * @version 1.0
 */
@Tag(name = "Template", description = "角色模板管理")
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final CharacterTemplateService characterTemplateService;
    private final PermissionChecker permissionChecker;

    /**
     * 获取模板列表
     * GET /api/v1/templates
     */
    @Operation(summary = "获取内置角色模板列表")
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listTemplates(
            @RequestParam(required = false) String category) {
        return ApiResponse.success(characterTemplateService.listTemplates(category));
    }

    /**
     * 获取模板详情
     * GET /api/v1/templates/:id
     */
    @Operation(summary = "获取模板详情")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getTemplate(@PathVariable String id) {
        Map<String, Object> template = characterTemplateService.getTemplate(id);
        if (template == null) {
            return ApiResponse.error(404, "Template not found: " + id);
        }
        return ApiResponse.success(template);
    }

    /**
     * 从模板派生创建角色
     * POST /api/v1/templates/:id/derive
     */
    @Operation(summary = "从模板派生创建角色")
    @PostMapping("/{id}/derive")
    public ApiResponse<Character> deriveCharacterFromTemplate(
            @PathVariable String id,
            @RequestBody DeriveTemplateRequest request) {
        permissionChecker.checkEdit("TEMPLATE", id);
        Character created = characterTemplateService.deriveCharacterFromTemplate(
                id, request.getName(), request.getDescription());
        return ApiResponse.success(created);
    }

    // ==================== 请求体 DTO ====================

    /** 从模板派生请求 */
    @Data
    public static class DeriveTemplateRequest {
        private String name;
        private String description;
    }
}