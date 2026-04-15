package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.permission.checker.PermissionChecker;
import org.dragon.permission.enums.ResourceType;
import org.dragon.expert.derive.CreateContext;
import org.dragon.datasource.entity.ExpertEntity;
import org.dragon.expert.service.ExpertService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * ExpertController Expert 管理 API
 *
 * <p>提供 Expert 的创建、查询、派生等功能。
 *
 * @author yijunw
 */
@Tag(name = "Expert", description = "Expert 管理")
@RestController
@RequestMapping("/api/v1/experts")
@RequiredArgsConstructor
public class ExpertController {

    private final ExpertService expertService;
    private final PermissionChecker permissionChecker;

    // ==================== 创建 Expert ====================

    /**
     * 白板创建 Expert（一步完成）
     * POST /api/v1/experts/create-with-expert
     */
    @Operation(summary = "白板创建 Expert")
    @PostMapping("/create-with-expert")
    public ApiResponse<Object> createWithExpert(@RequestBody CreateExpertRequest request) {
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

        Object asset = expertService.createWithExpert(context);
        return ApiResponse.success(asset);
    }

    /**
     * 从已有资产创建 Expert（fork）
     * POST /api/v1/experts/from-asset
     */
    @Operation(summary = "从资产创建 Expert")
    @PostMapping("/from-asset")
    public ApiResponse<ExpertVO> createExpertFromAsset(@RequestBody CreateExpertFromAssetRequest request) {
        permissionChecker.checkEdit(request.getResourceType().name(), request.getResourceId());

        ExpertEntity mark = expertService.createExpertFromAsset(
                request.getResourceType(),
                request.getResourceId(),
                request.getCategory(),
                request.getPreview(),
                request.getTargetAudience()
        );
        return ApiResponse.success(toVO(mark));
    }

    // ==================== 标记/取消标记 ====================

    /**
     * 取消 Expert 标记
     * DELETE /api/v1/experts/{resourceType}/{resourceId}
     */
    @Operation(summary = "取消 Expert 标记")
    @DeleteMapping("/{resourceType}/{resourceId}")
    public ApiResponse<Void> unmarkExpert(
            @PathVariable ResourceType resourceType,
            @PathVariable String resourceId) {
        permissionChecker.checkEdit(resourceType.name(), resourceId);
        expertService.unmarkExpert(resourceType, resourceId);
        return ApiResponse.success();
    }

    // ==================== 查询 ====================

    /**
     * 获取 Expert 详情
     * GET /api/v1/experts/{resourceType}/{resourceId}
     */
    @Operation(summary = "获取 Expert 详情")
    @GetMapping("/{resourceType}/{resourceId}")
    public ApiResponse<ExpertVO> getExpert(
            @PathVariable ResourceType resourceType,
            @PathVariable String resourceId) {
        Optional<ExpertEntity> markOpt = expertService.getExpertMark(resourceType, resourceId);
        return markOpt.map(m -> ApiResponse.success(toVOWithAsset(m)))
                .orElse(ApiResponse.error(404, "Expert not found"));
    }

    // ==================== 派生 ====================

    /**
     * 从 Expert 派生创建新资产
     * POST /api/v1/experts/{resourceType}/{resourceId}/derive
     */
    @Operation(summary = "从 Expert 派生创建新资产")
    @PostMapping("/{resourceType}/{resourceId}/derive")
    public ApiResponse<Object> deriveFromExpert(
            @PathVariable ResourceType resourceType,
            @PathVariable String resourceId) {
        // permissionChecker.checkEdit(resourceType.name(), resourceId);

        Object derived = expertService.deriveFromExpert(resourceType, resourceId);
        return ApiResponse.success(derived);
    }

    // ==================== DTO ==========

    @Data
    public static class CreateExpertRequest {
        private ResourceType resourceType;
        private String name;
        private String description;
        private String category;
        private String preview;
        private String targetAudience;
        private Map<String, Object> config;
    }

    @Data
    public static class CreateExpertFromAssetRequest {
        private ResourceType resourceType;
        private String resourceId;
        private String category;
        private String preview;
        private String targetAudience;
    }

    @Data
    @Builder
    public static class ExpertVO {
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

    private ExpertVO toVO(ExpertEntity mark) {
        return ExpertVO.builder()
                .id(mark.getId())
                .resourceType(mark.getResourceType())
                .resourceId(mark.getResourceId())
                .category(mark.getCategory())
                .preview(mark.getPreview())
                .targetAudience(mark.getTargetAudience())
                .usageCount(mark.getUsageCount())
                .build();
    }

    private ExpertVO toVOWithAsset(ExpertEntity mark) {
        ExpertVO vo = toVO(mark);
        vo.setAsset(expertService.getExpertAsset(mark.getResourceType(), mark.getResourceId()));
        return vo;
    }
}