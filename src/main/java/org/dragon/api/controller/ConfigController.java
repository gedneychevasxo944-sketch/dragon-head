package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.api.dto.ApiResponse;
import org.dragon.api.dto.PageResponse;
import org.dragon.config.context.InheritanceContext;
import org.dragon.config.dto.AssetConfigVO;
import org.dragon.config.dto.ConfigItemVO;
import org.dragon.config.dto.ConfigTopologyGraphVO;
import org.dragon.config.dto.ConfigTopologyVO;
import org.dragon.config.dto.EffectChainVO;
import org.dragon.config.enums.ConfigLevel;
import org.dragon.config.service.ConfigApplication;
import org.dragon.config.service.ConfigEffectService;
import org.dragon.config.service.ConfigTopologyService;
import org.dragon.permission.checker.PermissionChecker;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ConfigController 配置中心模块 API
 *
 * <p>对应前端 /config 页面，包含配置项管理、查询等接口。
 * Base URL: /api/v1/config
 */
@Slf4j
@Tag(name = "Config", description = "配置中心模块")
@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigApplication configApplication;
    private final ConfigTopologyService configTopologyService;
    private final PermissionChecker permissionChecker;

    // ==================== 配置项管理 ====================

    /**
     * 获取配置项列表
     * GET /api/v1/config/items
     */
    @Operation(summary = "获取配置项列表（分页+筛选）")
    @GetMapping("/items")
    public ApiResponse<PageResponse<Map<String, Object>>> listConfigItems(
            @RequestParam(required = false) String scopeType,
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) String characterId,
            @RequestParam(required = false) String toolId,
            @RequestParam(required = false) String skillId,
            @RequestParam(required = false) String memoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        log.info("[ConfigController] listConfigItems scopeType={} workspaceId={} characterId={} toolId={} skillId={} memoryId={}",
                scopeType, workspaceId, characterId, toolId, skillId, memoryId);

        if (scopeType == null || scopeType.isBlank()) {
            return ApiResponse.success(PageResponse.of(List.of(), 0, page, pageSize));
        }

        ConfigLevel configLevel;
        try {
            configLevel = ConfigLevel.valueOf(scopeType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ApiResponse.success(PageResponse.of(List.of(), 0, page, pageSize));
        }

        InheritanceContext context = buildContext(configLevel, workspaceId, characterId, toolId, skillId, memoryId);
        List<ConfigItemVO> items = configApplication.listConfigItems(context);

        // 过滤搜索关键词
        if (search != null && !search.isBlank()) {
            items = items.stream()
                    .filter(item -> item.getKey().toLowerCase().contains(search.toLowerCase()) ||
                            (item.getName() != null && item.getName().toLowerCase().contains(search.toLowerCase())))
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
     * 获取配置生效链
     * GET /api/v1/config/items/{configKey}/effect-chain
     */
    @Operation(summary = "获取配置生效链")
    @GetMapping("/items/{configKey}/effect-chain")
    public ApiResponse<List<EffectChainVO>> getEffectChain(
            @PathVariable String configKey,
            @RequestParam String scopeType,
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) String characterId,
            @RequestParam(required = false) String toolId,
            @RequestParam(required = false) String skillId,
            @RequestParam(required = false) String memoryId) {
        log.info("[ConfigController] getEffectChain key={} scopeType={} workspaceId={} characterId={} toolId={} skillId={} memoryId={}",
                configKey, scopeType, workspaceId, characterId, toolId, skillId, memoryId);

        ConfigLevel configLevel;
        try {
            configLevel = ConfigLevel.valueOf(scopeType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, "Invalid scopeType");
        }

        InheritanceContext context = buildContext(configLevel, workspaceId, characterId, toolId, skillId, memoryId);
        List<EffectChainVO> chain = configApplication.getEffectChain(configKey, context);
        return ApiResponse.success(chain);
    }

    /**
     * 更新配置项
     * PUT /api/v1/config/items
     */
    @Operation(summary = "更新配置项")
    @PutMapping("/items")
    public ApiResponse<Map<String, Object>> updateConfigItem(@RequestBody UpdateConfigRequest request) {
        log.info("[ConfigController] updateConfigItem key={} level={}", request.getConfigKey(), request.getLevel());

        configApplication.setConfigValue(
                request.getConfigKey(),
                request.getValue(),
                request.getLevel(),
                request.getWorkspaceId(),
                request.getCharacterId(),
                request.getToolId(),
                request.getSkillId(),
                request.getMemoryId()
        );

        InheritanceContext context = buildContext(request.getLevel(), request.getWorkspaceId(), request.getCharacterId(), request.getToolId(), request.getSkillId(), request.getMemoryId());
        return ApiResponse.success(toMap(configApplication.getEffectiveConfig(request.getConfigKey(), context)));
    }

    /**
     * 获取生效值
     * GET /api/v1/config/effective
     */
    @Operation(summary = "获取配置生效值")
    @GetMapping("/effective")
    public ApiResponse<Map<String, Object>> getEffectiveValue(
            @RequestParam String configKey,
            @RequestParam String level,
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) String characterId,
            @RequestParam(required = false) String toolId,
            @RequestParam(required = false) String skillId,
            @RequestParam(required = false) String memoryId) {
        log.info("[ConfigController] getEffectiveValue key={} level={}", configKey, level);

        ConfigLevel configLevel;
        try {
            configLevel = ConfigLevel.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, "Invalid level");
        }

        InheritanceContext context = buildContext(configLevel, workspaceId, characterId, toolId, skillId, memoryId);
        ConfigEffectService.EffectiveConfig ec = configApplication.getEffectiveConfig(configKey, context);

        return ApiResponse.success(toMap(ec));
    }

    // ==================== 简化后的配置项列表（仅元数据）====================

    /**
     * 获取配置项元数据列表
     * GET /api/v1/config/metadata
     */
    @Operation(summary = "获取配置项元数据列表")
    @GetMapping("/metadata")
    public ApiResponse<List<ConfigItemVO>> listConfigMetadata() {
        log.info("[ConfigController] listConfigMetadata");
        List<ConfigItemVO> items = configApplication.listConfigItems(InheritanceContext.forGlobal());
        return ApiResponse.success(items);
    }

    // ==================== 视角一：按资产查看配置 ====================

    /**
     * 按资产查看配置
     * GET /api/v1/config/asset/{assetType}/{assetId}/configs
     */
    @Operation(summary = "按资产查看配置")
    @GetMapping("/asset/{assetType}/{assetId}/configs")
    public ApiResponse<AssetConfigVO> getAssetConfigs(
            @PathVariable String assetType,
            @PathVariable String assetId) {
        log.info("[ConfigController] getAssetConfigs assetType={} assetId={}", assetType, assetId);
        AssetConfigVO result = configTopologyService.getAssetConfigs(assetType, assetId);
        return ApiResponse.success(result);
    }

    // ==================== 视角二：配置链路拓扑 ====================

    /**
     * 获取配置链路拓扑（图可视化专用）
     * GET /api/v1/config/topology/graph
     */
    @Operation(summary = "获取配置拓扑图（图可视化）")
    @GetMapping("/topology/graph")
    public ApiResponse<ConfigTopologyGraphVO> getConfigTopologyGraph(
            @RequestParam String configKey,
            @RequestParam String assetType,
            @RequestParam String assetId) {
        log.info("[ConfigController] getConfigTopologyGraph configKey={} assetType={} assetId={}", configKey, assetType, assetId);
        ConfigTopologyGraphVO result = configTopologyService.getConfigTopologyGraph(configKey, assetType, assetId);
        return ApiResponse.success(result);
    }

    /**
     * 获取配置链路拓扑
     * GET /api/v1/config/topology
     */
    @Operation(summary = "获取配置链路拓扑")
    @GetMapping("/topology")
    public ApiResponse<ConfigTopologyVO> getTopology(
            @RequestParam(required = false) String configKey,
            @RequestParam(required = false) String assetType,
            @RequestParam(required = false) String assetId) {
        log.info("[ConfigController] getTopology configKey={} assetType={} assetId={}", configKey, assetType, assetId);

        if (configKey != null && assetType != null && assetId != null) {
            // 返回单配置的链路
            ConfigTopologyVO result = configTopologyService.getConfigChain(configKey, assetType, assetId);
            return ApiResponse.success(result);
        } else if (assetType != null && assetId != null) {
            // 返回完整拓扑树
            ConfigTopologyVO result = configTopologyService.getTopology(assetType, assetId);
            return ApiResponse.success(result);
        } else {
            return ApiResponse.error(400, "assetType and assetId are required");
        }
    }

    // ==================== 请求体 DTO ====================

    /** 更新配置项请求 */
    @lombok.Data
    public static class UpdateConfigRequest {
        private String configKey;
        private Object value;
        private ConfigLevel level;
        private String workspaceId;
        private String characterId;
        private String toolId;
        private String skillId;
        private String memoryId;
    }

    // ==================== 内部工具方法 ====================

    /**
     * 根据 level 和 IDs 构建 InheritanceContext
     */
    private InheritanceContext buildContext(ConfigLevel level, String workspaceId, String characterId,
                                             String toolId, String skillId, String memoryId) {
        if (level == null) {
            return InheritanceContext.builder().level(ConfigLevel.GLOBAL).build();
        }
        return InheritanceContext.forLevel(level, workspaceId, characterId, toolId, skillId, memoryId);
    }

    private Map<String, Object> toMap(ConfigItemVO item) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", item.getId());
        map.put("key", item.getKey());
        map.put("name", item.getName());
        map.put("description", item.getDescription());
        map.put("scopeType", item.getScopeType());
        map.put("scopeId", item.getScopeId());
        map.put("dataType", item.getDataType());
        map.put("defaultValue", item.getDefaultValue());
        map.put("currentValue", item.getCurrentValue());
        map.put("effectiveValue", item.getEffectiveValue());
        map.put("displayStatus", item.getDisplayStatus());
        map.put("source", item.getSource());
        map.put("validationRules", item.getValidationRules());
        map.put("options", item.getOptions());
        map.put("lastModified", item.getLastModified());
        map.put("modifiedBy", item.getModifiedBy());
        return map;
    }

    private Map<String, Object> toMap(ConfigEffectService.EffectiveConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("configKey", config.getConfigKey());
        map.put("effectiveValue", config.getEffectiveValue());
        map.put("valueType", config.getValueType());
        map.put("source", config.getSource());
        map.put("isInherited", config.isInherited());
        map.put("displayStatus", config.getDisplayStatus() != null ? config.getDisplayStatus().name() : null);
        return map;
    }
}