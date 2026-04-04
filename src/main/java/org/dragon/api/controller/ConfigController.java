package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.application.ConfigApplication;
import org.dragon.api.dto.ApiResponse;
import org.dragon.api.dto.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
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

/**
 * ConfigController 配置中心模块 API
 *
 * <p>对应前端 /config 页面，包含配置项管理、草稿发布、回滚、变更记录、影响分析等接口。
 * Base URL: /api/v1/config
 *
 * @author zhz
 * @version 1.0
 */
@Tag(name = "Config", description = "配置中心模块")
@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ConfigController {

    private final ConfigApplication configApplication;

    // ==================== 27. Config（配置中心）====================

    /**
     * 27.1 获取配置项列表
     * GET /api/v1/config/items
     */
    @Operation(summary = "获取配置项列表（分页+筛选）")
    @GetMapping("/items")
    public ApiResponse<PageResponse<Map<String, Object>>> listConfigItems(
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String scopeType,
            @RequestParam(required = false) String scopeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isDraft,
            @RequestParam(required = false) Boolean hasOverride,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<Map<String, Object>> result = configApplication.listConfigItems(
                domain, scopeType, scopeId, search, isDraft, hasOverride, page, pageSize);
        return ApiResponse.success(result);
    }

    /**
     * 27.2 更新配置项（保存草稿）
     * PUT /api/v1/config/items/:id
     */
    @Operation(summary = "更新配置项（可保存为草稿）")
    @PutMapping("/items/{id}")
    public ApiResponse<Map<String, Object>> updateConfigItem(
            @PathVariable String id,
            @RequestBody UpdateConfigRequest request) {
        Map<String, Object> result = configApplication.updateConfigItem(
                id, request.getValue(), request.getSaveAsDraft());
        return ApiResponse.success(result);
    }

    /**
     * 27.3 发布配置草稿
     * POST /api/v1/config/items/:id/publish
     */
    @Operation(summary = "发布配置草稿")
    @PostMapping("/items/{id}/publish")
    public ApiResponse<Map<String, Object>> publishConfigItem(@PathVariable String id) {
        Map<String, Object> result = configApplication.publishConfigItem(id);
        return ApiResponse.success(result);
    }

    /**
     * 27.4 回滚配置
     * POST /api/v1/config/items/:id/rollback
     */
    @Operation(summary = "回滚配置到指定版本")
    @PostMapping("/items/{id}/rollback")
    public ApiResponse<Map<String, Object>> rollbackConfigItem(
            @PathVariable String id,
            @RequestBody RollbackConfigRequest request) {
        Map<String, Object> result = configApplication.rollbackConfigItem(id, request.getChangeRecordId());
        return ApiResponse.success(result);
    }

    /**
     * 27.5 获取生效链
     * GET /api/v1/config/items/:id/effect-chain
     */
    @Operation(summary = "获取配置项生效链")
    @GetMapping("/items/{id}/effect-chain")
    public ApiResponse<List<Map<String, Object>>> getEffectChain(@PathVariable String id) {
        List<Map<String, Object>> chain = configApplication.getEffectChain(id);
        return ApiResponse.success(chain);
    }

    /**
     * 27.6 获取变更记录
     * GET /api/v1/config/change-records
     */
    @Operation(summary = "获取配置变更记录")
    @GetMapping("/change-records")
    public ApiResponse<PageResponse<Map<String, Object>>> listChangeRecords(
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String configItemId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<Map<String, Object>> result = configApplication.listChangeRecords(
                domain, configItemId, page, pageSize);
        return ApiResponse.success(result);
    }

    /**
     * 27.7 获取影响分析
     * GET /api/v1/config/items/:id/impact
     */
    @Operation(summary = "获取配置项变更影响分析")
    @GetMapping("/items/{id}/impact")
    public ApiResponse<Map<String, Object>> getImpactAnalysis(@PathVariable String id) {
        Map<String, Object> impact = configApplication.getImpactAnalysis(id);
        return ApiResponse.success(impact);
    }

    // ==================== 请求体 DTO ====================

    /** 更新配置项请求 */
    @Data
    public static class UpdateConfigRequest {
        /** 新的配置值 */
        private Object value;
        /** 是否保存为草稿 */
        private Boolean saveAsDraft;
    }

    /** 回滚配置请求 */
    @Data
    public static class RollbackConfigRequest {
        /** 回滚目标变更记录 ID */
        private String changeRecordId;
    }
}
