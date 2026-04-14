package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.character.Character;
import org.dragon.permission.checker.PermissionChecker;
import org.dragon.permission.enums.ResourceType;
import org.dragon.template.derive.CreateContext;
import org.dragon.template.derive.DeriveTemplateRequest;
import org.dragon.datasource.entity.TemplateMarkEntity;
import org.dragon.template.service.TemplateMarkService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TemplateController 模板管理 API
 *
 * <p>提供通用的模板管理功能，支持将各类资产（Character、Skill、Trait等）标记为模板，
 * 并通过模板派生创建新资产。
 *
 * @author yijunw
 * @version 1.0
 */
@Tag(name = "Template", description = "模板管理")
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateMarkService templateMarkService;
    private final PermissionChecker permissionChecker;

    // ==================== 白板创建模板 ====================

    /**
     * 白板创建模板（一步完成）
     * POST /api/v1/templates/create-with-template
     */
    @Operation(summary = "白板创建模板")
    @PostMapping("/create-with-template")
    public ApiResponse<Object> createWithTemplate(@RequestBody CreateTemplateRequest request) {
        permissionChecker.checkEdit(request.getResourceType().name(), null);

        CreateContext context = CreateContext.builder()
                .resourceType(request.getResourceType())
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .preview(request.getPreview())
                .targetAudience(request.getTargetAudience())
                .config(request.getConfig())
                .build();

        Object asset = templateMarkService.createWithTemplate(context);
        return ApiResponse.success(asset);
    }

    // ==================== 标记/取消标记 ====================

    /**
     * 将资产标记为模板
     * POST /api/v1/templates/mark
     */
    @Operation(summary = "将资产标记为模板")
    @PostMapping("/mark")
    public ApiResponse<TemplateMarkVO> markAsTemplate(@RequestBody MarkTemplateRequest request) {
        permissionChecker.checkEdit(request.getResourceType().name(), request.getResourceId());

        TemplateMarkEntity mark = templateMarkService.markAsTemplate(
                request.getResourceType(),
                request.getResourceId(),
                request.getCategory(),
                request.getPreview(),
                request.getTargetAudience()
        );
        return ApiResponse.success(toVO(mark));
    }

    /**
     * 取消模板标记
     * DELETE /api/v1/templates/mark/{resourceType}/{resourceId}
     */
    @Operation(summary = "取消模板标记")
    @DeleteMapping("/mark/{resourceType}/{resourceId}")
    public ApiResponse<Void> unmarkTemplate(
            @PathVariable ResourceType resourceType,
            @PathVariable String resourceId) {
        permissionChecker.checkEdit(resourceType.name(), resourceId);
        templateMarkService.unmarkTemplate(resourceType, resourceId);
        return ApiResponse.success();
    }

    // ==================== 查询 ====================

    /**
     * 获取模板列表
     * GET /api/v1/templates?resourceType=CHARACTER&category=助手
     */
    @Operation(summary = "获取模板列表")
    @GetMapping
    public ApiResponse<List<TemplateMarkVO>> listTemplates(
            @RequestParam(required = false) ResourceType resourceType,
            @RequestParam(required = false) String category) {
        List<TemplateMarkEntity> marks = templateMarkService.listTemplates(resourceType, category);
        List<TemplateMarkVO> vos = marks.stream().map(this::toVOWithAsset).toList();
        return ApiResponse.success(vos);
    }

    /**
     * 获取模板详情
     * GET /api/v1/templates/{resourceType}/{resourceId}
     */
    @Operation(summary = "获取模板详情")
    @GetMapping("/{resourceType}/{resourceId}")
    public ApiResponse<TemplateMarkVO> getTemplate(
            @PathVariable ResourceType resourceType,
            @PathVariable String resourceId) {
        Optional<TemplateMarkEntity> markOpt = templateMarkService.getTemplateMark(resourceType, resourceId);
        return markOpt.map(m -> ApiResponse.success(toVOWithAsset(m)))
                .orElse(ApiResponse.error(404, "Template not found"));
    }

    // ==================== 派生 ====================

    /**
     * 从模板派生创建资产
     * POST /api/v1/templates/{resourceType}/{resourceId}/derive
     */
    @Operation(summary = "从模板派生创建资产")
    @PostMapping("/{resourceType}/{resourceId}/derive")
    public ApiResponse<Object> deriveFromTemplate(
            @PathVariable ResourceType resourceType,
            @PathVariable String resourceId,
            @RequestBody DeriveTemplateRequest request) {
        permissionChecker.checkEdit(resourceType.name(), resourceId);

        Object derived = templateMarkService.derive(resourceType, resourceId, request);
        return ApiResponse.success(derived);
    }

    // ==================== DTO ==========

    @Data
    public static class CreateTemplateRequest {
        private ResourceType resourceType;
        private String name;
        private String description;
        private String category;
        private String preview;
        private String targetAudience;
        private Map<String, Object> config;
    }

    @Data
    public static class MarkTemplateRequest {
        private ResourceType resourceType;
        private String resourceId;
        private String category;
        private String preview;
        private String targetAudience;
    }

    @Data
    @Builder
    public static class TemplateMarkVO {
        private String id;
        private ResourceType resourceType;
        private String resourceId;
        private String category;
        private String preview;
        private String targetAudience;
        private Integer usageCount;
        private Object asset;
    }

    // ==================== 内部方法 ==========

    private TemplateMarkVO toVO(TemplateMarkEntity mark) {
        return TemplateMarkVO.builder()
                .id(mark.getId())
                .resourceType(mark.getResourceType())
                .resourceId(mark.getResourceId())
                .category(mark.getCategory())
                .preview(mark.getPreview())
                .targetAudience(mark.getTargetAudience())
                .usageCount(mark.getUsageCount())
                .build();
    }

    private TemplateMarkVO toVOWithAsset(TemplateMarkEntity mark) {
        TemplateMarkVO vo = toVO(mark);
        vo.setAsset(templateMarkService.getTemplateAsset(mark.getResourceType(), mark.getResourceId()));
        return vo;
    }
}