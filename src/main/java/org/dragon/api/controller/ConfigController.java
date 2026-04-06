package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.api.dto.ApiResponse;
import org.dragon.api.dto.PageResponse;
import org.dragon.config.enums.ConfigScopeType;
import org.dragon.config.service.ConfigApplication;
import org.dragon.config.service.ConfigEffectService;
import org.dragon.config.service.ConfigImpactAnalyzer;
import org.dragon.permission.checker.PermissionChecker;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ConfigController 配置中心模块 API
 *
 * <p>对应前端 /config 页面，包含配置项管理、查询、影响分析等接口。
 * Base URL: /api/v1/config
 */
@Slf4j
@Tag(name = "Config", description = "配置中心模块")
@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigApplication configApplication;
    private final PermissionChecker permissionChecker;

    // ==================== 配置项管理 ====================

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
        log.info("[ConfigController] listConfigItems domain={} scopeType={} scopeId={}", domain, scopeType, scopeId);

        if (scopeType == null || scopeType.isBlank()) {
            return ApiResponse.success(PageResponse.of(List.of(), 0, page, pageSize));
        }

        ConfigScopeType type;
        try {
            type = ConfigScopeType.valueOf(scopeType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ApiResponse.success(PageResponse.of(List.of(), 0, page, pageSize));
        }

        List<ConfigApplication.ConfigItemVO> items = configApplication.listConfigItems(type, scopeId);

        // 过滤搜索关键词
        if (search != null && !search.isBlank()) {
            items = items.stream()
                    .filter(item -> item.getConfigKey().toLowerCase().contains(search.toLowerCase()))
                    .toList();
        }

        long total = items.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, items.size());
        List<Map<String, Object>> pageData = fromIndex >= items.size() ? List.of()
                : items.subList(fromIndex, toIndex).stream()
                        .map(this::toMap)
                        .toList();

        return ApiResponse.success(PageResponse.of(pageData, total, page, pageSize));
    }

    /**
     * 27.2 更新配置项
     * PUT /api/v1/config/items/:id
     */
    @Operation(summary = "更新配置项")
    @PutMapping("/items/{id}")
    public ApiResponse<Map<String, Object>> updateConfigItem(
            @PathVariable String id,
            @RequestBody UpdateConfigRequest request) {
        log.info("[ConfigController] updateConfigItem id={}", id);
        permissionChecker.checkEdit("CONFIG", id);

        ParsedConfigId parsed = parseConfigId(id);
        if (parsed == null) {
            return ApiResponse.error(400, "Invalid config id format");
        }

        configApplication.setConfigValue(parsed.scopeType, parsed.scopeId, parsed.configKey, parsed.targetId, request.getValue());
        return ApiResponse.success(toMap(configApplication.getEffectiveValue(parsed.scopeType, parsed.scopeId, parsed.configKey, parsed.targetId)));
    }

    /**
     * 27.3 发布配置草稿（占位）
     * POST /api/v1/config/items/:id/publish
     */
    @Operation(summary = "发布配置草稿")
    @PostMapping("/items/{id}/publish")
    public ApiResponse<Map<String, Object>> publishConfigItem(@PathVariable String id) {
        log.info("[ConfigController] publishConfigItem id={}", id);
        permissionChecker.checkPermission("CONFIG", id, "PUBLISH");
        // 占位：Draft/Publish 机制待实现
        return ApiResponse.success(Map.of("id", id, "status", "published"));
    }

    /**
     * 27.4 回滚配置（占位）
     * POST /api/v1/config/items/:id/rollback
     */
    @Operation(summary = "回滚配置到指定版本")
    @PostMapping("/items/{id}/rollback")
    public ApiResponse<Map<String, Object>> rollbackConfigItem(
            @PathVariable String id,
            @RequestBody RollbackConfigRequest request) {
        log.info("[ConfigController] rollbackConfigItem id={}", id);
        permissionChecker.checkEdit("CONFIG", id);
        // 占位：回滚机制待实现
        return ApiResponse.success(Map.of("id", id, "status", "rollback_not_implemented"));
    }

    /**
     * 27.5 获取生效链
     * GET /api/v1/config/items/:id/effect-chain
     */
    @Operation(summary = "获取配置项生效链")
    @GetMapping("/items/{id}/effect-chain")
    public ApiResponse<List<Map<String, Object>>> getEffectChain(@PathVariable String id) {
        log.info("[ConfigController] getEffectChain id={}", id);
        permissionChecker.checkView("CONFIG", id);

        ParsedConfigId parsed = parseConfigId(id);
        if (parsed == null) {
            return ApiResponse.error(400, "Invalid config id format");
        }

        // 构建继承链响应
        List<Map<String, Object>> chain = new ArrayList<>();
        ConfigScopeType type = parsed.scopeType;

        // 添加当前级别
        Map<String, Object> current = new HashMap<>();
        current.put("level", type.name());
        current.put("scopeId", parsed.scopeId);
        current.put("value", null);
        current.put("effective", true);
        chain.add(current);

        // 添加 GLOBAL
        Map<String, Object> global = new HashMap<>();
        global.put("level", "GLOBAL");
        global.put("scopeId", "-");
        global.put("value", null);
        global.put("effective", true);
        chain.add(global);

        return ApiResponse.success(chain);
    }

    /**
     * 27.6 获取变更记录（占位）
     * GET /api/v1/config/change-records
     */
    @Operation(summary = "获取配置变更记录")
    @GetMapping("/change-records")
    public ApiResponse<PageResponse<Map<String, Object>>> listChangeRecords(
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String configItemId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        log.info("[ConfigController] listChangeRecords domain={} configItemId={}", domain, configItemId);
        // 占位：变更记录待实现
        return ApiResponse.success(PageResponse.of(List.of(), 0, page, pageSize));
    }

    /**
     * 27.7 获取影响分析
     * GET /api/v1/config/items/:id/impact
     */
    @Operation(summary = "获取配置项变更影响分析")
    @GetMapping("/items/{id}/impact")
    public ApiResponse<Map<String, Object>> getImpactAnalysis(@PathVariable String id) {
        log.info("[ConfigController] getImpactAnalysis id={}", id);
        permissionChecker.checkView("CONFIG", id);

        ParsedConfigId parsed = parseConfigId(id);
        if (parsed == null) {
            return ApiResponse.error(400, "Invalid config id format");
        }

        ConfigImpactAnalyzer.ImpactAnalysis analysis = configApplication.getImpactAnalysis(
                parsed.scopeType, parsed.scopeId, parsed.configKey);

        Map<String, Object> result = new HashMap<>();
        result.put("configId", id);
        result.put("scopeType", parsed.scopeType.name());
        result.put("scopeId", parsed.scopeId);
        result.put("configKey", parsed.configKey);
        result.put("impactItems", analysis.getImpacts().stream()
                .map(item -> Map.of(
                        "resourceType", item.getResourceType(),
                        "resourceId", item.getResourceId(),
                        "impactType", item.getImpactType().name(),
                        "description", item.getDescription()
                ))
                .toList());
        result.put("affectedCount", analysis.getImpacts().size());

        return ApiResponse.success(result);
    }

    // ==================== 请求体 DTO ====================

    /** 更新配置项请求 */
    @lombok.Data
    public static class UpdateConfigRequest {
        /** 新的配置值 */
        private Object value;
        /** 是否保存为草稿 */
        private Boolean saveAsDraft;
    }

    /** 回滚配置请求 */
    @lombok.Data
    public static class RollbackConfigRequest {
        /** 回滚目标变更记录 ID */
        private String changeRecordId;
    }

    // ==================== 内部工具方法 ====================

    /**
     * 解析配置ID
     * 格式: {scopeType}:{scopeId}:{configKey} 或 {scopeType}:{scopeId}:{targetType}:{targetId}:{configKey}
     */
    private ParsedConfigId parseConfigId(String configId) {
        if (configId == null || configId.isBlank()) {
            return null;
        }

        String[] parts = configId.split(":", 5);
        if (parts.length < 3) {
            return null;
        }

        try {
            ConfigScopeType scopeType = ConfigScopeType.valueOf(parts[0].toUpperCase());
            String scopeId = parts[1];
            String configKey;
            String targetId = null;

            if (parts.length == 3) {
                configKey = parts[2];
            } else if (parts.length == 5) {
                // 格式: scopeType:scopeId:targetType:targetId:configKey
                configKey = parts[4];
                // targetId = parts[3]; // 当前实现不使用 targetId
            } else {
                return null;
            }

            return new ParsedConfigId(scopeType, scopeId, configKey, targetId);
        } catch (IllegalArgumentException e) {
            log.warn("[ConfigController] Invalid scopeType in configId: {}", configId);
            return null;
        }
    }

    private record ParsedConfigId(ConfigScopeType scopeType, String scopeId, String configKey, String targetId) {}

    private Map<String, Object> toMap(ConfigApplication.ConfigItemVO item) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", item.getConfigKeyFull());
        map.put("configKey", item.getConfigKey());
        map.put("effectiveValue", item.getEffectiveValue());
        map.put("storeValue", item.getStoreValue());
        map.put("displayStatus", item.getDisplayStatus());
        map.put("source", item.getSource());
        map.put("valueType", item.getValueType());
        map.put("description", item.getDescription());
        map.put("defaultValue", item.getDefaultValue());
        return map;
    }

    private Map<String, Object> toMap(ConfigEffectService.EffectiveConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("scopeType", config.getScopeType().name());
        map.put("scopeId", config.getScopeId());
        map.put("configKey", config.getConfigKey());
        map.put("effectiveValue", config.getEffectiveValue());
        map.put("valueType", config.getValueType());
        map.put("source", config.getSource());
        map.put("isInherited", config.isInherited());
        map.put("displayStatus", config.getDisplayStatus().name());
        return map;
    }
}