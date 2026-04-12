package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.asset.enums.PublishStatus;
import org.dragon.asset.service.AssetPublishStatusService;
import org.dragon.permission.enums.ResourceType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * PublishStatusController 资产发布状态 API
 *
 * <p>获取资产的发布状态（DRAFT / PUBLISHED / PENDING 等）
 * Base URL: /api/v1/publish-status
 */
@Tag(name = "PublishStatus", description = "资产发布状态")
@RestController
@RequestMapping("/api/v1/publish-status")
@RequiredArgsConstructor
public class PublishStatusController {

    private final AssetPublishStatusService publishStatusService;

    /**
     * 获取资产的发布状态
     * GET /api/v1/publish-status/{resourceType}/{resourceId}
     */
    @Operation(summary = "获取资产的发布状态")
    @GetMapping("/{resourceType}/{resourceId}")
    public ApiResponse<Map<String, Object>> getPublishStatus(
            @PathVariable String resourceType,
            @PathVariable String resourceId) {
        ResourceType type;
        try {
            type = ResourceType.valueOf(resourceType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("无效的资源类型: " + resourceType);
        }

        PublishStatus status = publishStatusService.getStatusOrDefault(type, resourceId);

        Map<String, Object> result = new HashMap<>();
        result.put("resourceType", resourceType.toUpperCase());
        result.put("resourceId", resourceId);
        result.put("status", status.name());

        return ApiResponse.success(result);
    }
}
