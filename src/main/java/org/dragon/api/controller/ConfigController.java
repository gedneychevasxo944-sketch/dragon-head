package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.api.dto.ApiResponse;
import org.dragon.api.dto.PageResponse;
import org.dragon.config.context.InheritanceContext;
import org.dragon.config.dto.AssetConfigVO;
import org.dragon.config.dto.AssetTypeOption;
import org.dragon.config.dto.ConfigItemVO;
import org.dragon.config.dto.ImpactAnalysis;
import org.dragon.config.dto.ConfigTopologyGraphVO;
import org.dragon.config.dto.ConfigTopologyVO;
import org.dragon.config.dto.EffectChainVO;
import org.dragon.config.enums.ConfigLevel;
import org.dragon.config.model.InheritanceConfig;
import org.dragon.config.model.InheritanceConfig.AssetType;
import org.dragon.config.model.InheritanceConfig.Level;
import org.dragon.config.service.ConfigApplication;
import org.dragon.config.service.ConfigEffectService;
import org.dragon.config.service.ConfigImpactAnalyzer;
import org.dragon.config.service.ConfigTopologyService;
import org.dragon.asset.service.AssetMemberService;
import org.dragon.permission.checker.PermissionChecker;
import org.dragon.permission.enums.ResourceType;
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
    private final ConfigImpactAnalyzer configImpactAnalyzer;
    private final PermissionChecker permissionChecker;
    private final AssetMemberService assetMemberService;

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

        // 检查权限
        String[] parent = determineParentResource(request.getLevel(), request.getWorkspaceId(),
                request.getCharacterId(), request.getToolId(), request.getSkillId(), request.getMemoryId());
        if (parent != null) {
            permissionChecker.checkEdit(parent[0], parent[1]);
        }

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
     * 创建配置项
     * POST /api/v1/config/items
     */
    @Operation(summary = "创建配置项")
    @PostMapping("/items")
    public ApiResponse<Map<String, Object>> createConfigItem(@RequestBody CreateConfigRequest request) {
        log.info("[ConfigController] createConfigItem key={} level={}", request.getConfigKey(), request.getLevel());

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

        // 建立所有者关系
        String[] parent = determineParentResource(request.getLevel(), request.getWorkspaceId(),
                request.getCharacterId(), request.getToolId(), request.getSkillId(), request.getMemoryId());
        if (parent != null) {
            ResourceType resourceType = ResourceType.valueOf(parent[0]);
            assetMemberService.addOwnerDirectly(resourceType, parent[1], getCurrentUserId());
        }

        InheritanceContext context = buildContext(request.getLevel(), request.getWorkspaceId(), request.getCharacterId(), request.getToolId(), request.getSkillId(), request.getMemoryId());
        return ApiResponse.success(toMap(configApplication.getEffectiveConfig(request.getConfigKey(), context)));
    }

    /**
     * 删除配置项
     * DELETE /api/v1/config/items
     */
    @Operation(summary = "删除配置项")
    @DeleteMapping("/items")
    public ApiResponse<Void> deleteConfigItem(@RequestBody DeleteConfigRequest request) {
        log.info("[ConfigController] deleteConfigItem key={} level={}", request.getConfigKey(), request.getLevel());

        // 检查权限
        String[] parent = determineParentResource(request.getLevel(), request.getWorkspaceId(),
                request.getCharacterId(), request.getToolId(), request.getSkillId(), request.getMemoryId());
        if (parent != null) {
            permissionChecker.checkDelete(parent[0], parent[1]);
        }

        InheritanceContext context = buildContext(request.getLevel(), request.getWorkspaceId(), request.getCharacterId(), request.getToolId(), request.getSkillId(), request.getMemoryId());
        configApplication.deleteConfigValue(request.getConfigKey(), context);

        return ApiResponse.success(null);
    }

    /**
     * 获取生效值
     * GET /api/v1/config/effective
     *
     * <p>level 参数支持简化名称：GLOBAL, USER, WORKSPACE, CHARACTER, SKILL, TOOL, MEMORY
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

        ConfigLevel configLevel = toConfigLevel(level.toUpperCase());
        if (configLevel == null) {
            return ApiResponse.error(400, "Invalid level");
        }

        InheritanceContext context = buildContext(configLevel, workspaceId, characterId, toolId, skillId, memoryId);
        ConfigEffectService.EffectiveConfig ec = configApplication.getEffectiveConfig(configKey, context);

        return ApiResponse.success(toMap(ec));
    }

    /**
     * 将简化层级名称转换为 ConfigLevel
     */
    private ConfigLevel toConfigLevel(String levelName) {
        return switch (levelName) {
            case "GLOBAL" -> ConfigLevel.GLOBAL;
            case "USER" -> ConfigLevel.STUDIO;
            case "WORKSPACE" -> ConfigLevel.STUDIO_WORKSPACE;
            case "CHARACTER" -> ConfigLevel.GLOBAL_CHARACTER;
            case "SKILL" -> ConfigLevel.GLOBAL_SKILL;
            case "TOOL" -> ConfigLevel.GLOBAL_TOOL;
            case "MEMORY" -> ConfigLevel.GLOBAL_MEMORY;
            default -> {
                // 尝试直接作为 ConfigLevel 名称使用
                try {
                    yield ConfigLevel.valueOf(levelName);
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
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

    // ==================== 资产类型选项 ====================

    /**
     * 获取资产类型选项列表（用于渐进式配置层级选择）
     * GET /api/v1/config/asset-types
     */
    @Operation(summary = "获取资产类型选项列表")
    @GetMapping("/asset-types")
    public ApiResponse<List<AssetTypeOption>> listAssetTypes() {
        log.info("[ConfigController] listAssetTypes");

        List<AssetTypeOption> options = java.util.Arrays.stream(AssetType.values())
                .map(type -> {
                    List<String> parentLevels = InheritanceConfig.getParentLevels(type).stream()
                            .map(Level::name)
                            .toList();
                    return AssetTypeOption.builder()
                            .type(type.name())
                            .label(getAssetTypeLabel(type))
                            .parentLevels(parentLevels)
                            .icon(getAssetTypeIcon(type))
                            .build();
                })
                .toList();

        return ApiResponse.success(options);
    }

    private String getAssetTypeLabel(AssetType type) {
        return switch (type) {
            case WORKSPACE -> "工作空间";
            case CHARACTER -> "角色";
            case SKILL -> "技能";
            case TOOL -> "工具";
            case MEMORY -> "记忆";
        };
    }

    private String getAssetTypeIcon(AssetType type) {
        return switch (type) {
            case WORKSPACE -> "workspaces";
            case CHARACTER -> "person";
            case SKILL -> "psychology";
            case TOOL -> "build";
            case MEMORY -> "memory";
        };
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
            @PathVariable String assetId,
            @RequestParam(required = false) String parentLevel) {
        log.info("[ConfigController] getAssetConfigs assetType={} assetId={} parentLevel={}", assetType, assetId, parentLevel);
        AssetConfigVO result = configTopologyService.getAssetConfigs(assetType, assetId, parentLevel);
        return ApiResponse.success(result);
    }

    // ==================== 视角二：配置链路拓扑 ====================

    /**
     * 获取配置链路拓扑
     * GET /api/v1/config/topology
     */
    @Operation(summary = "获取配置链路拓扑")
    @GetMapping("/topology")
    public ApiResponse<ConfigTopologyVO> getTopology(
            @RequestParam String configKey,
            @RequestParam String assetType,
            @RequestParam String assetId,
            @RequestParam(required = false) String parentLevel) {
        log.info("[ConfigController] getTopology configKey={} assetType={} assetId={} parentLevel={}", configKey, assetType, assetId, parentLevel);
        ConfigTopologyVO result = configTopologyService.getConfigChain(configKey, assetType, assetId, parentLevel);
        return ApiResponse.success(result);
    }

    // ==================== 影响面分析 ====================

    /**
     * 分析配置变更影响面
     * GET /api/v1/config/impact
     *
     * <p>level 参数支持简化名称：GLOBAL, USER, WORKSPACE, CHARACTER, SKILL, TOOL, MEMORY
     */
    @Operation(summary = "分析配置变更影响面")
    @GetMapping("/impact")
    public ApiResponse<ImpactAnalysis> analyzeImpact(
            @RequestParam String level,
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) String characterId,
            @RequestParam(required = false) String toolId,
            @RequestParam(required = false) String skillId,
            @RequestParam(required = false) String memoryId) {
        log.info("[ConfigController] analyzeImpact level={} workspaceId={} characterId={}", level, workspaceId, characterId);
        ConfigLevel configLevel = ConfigLevel.valueOf(level.toUpperCase());
        ImpactAnalysis result = configImpactAnalyzer.analyzeImpact(configLevel, workspaceId, characterId, toolId, skillId, memoryId);
        return ApiResponse.success(result);
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

    /** 创建配置项请求 */
    @lombok.Data
    public static class CreateConfigRequest {
        private String configKey;
        private Object value;
        private ConfigLevel level;
        private String workspaceId;
        private String characterId;
        private String toolId;
        private String skillId;
        private String memoryId;
    }

    /** 删除配置项请求 */
    @lombok.Data
    public static class DeleteConfigRequest {
        private String configKey;
        private ConfigLevel level;
        private String workspaceId;
        private String characterId;
        private String toolId;
        private String skillId;
        private String memoryId;
    }

    // ==================== 内部工具方法 ====================

    /**
     * 根据 ConfigLevel 确定父级资源类型和ID
     *
     * @return [resourceType, resourceId] 或 null 表示无需权限检查
     */
    private String[] determineParentResource(ConfigLevel level, String workspaceId,
            String characterId, String toolId, String skillId, String memoryId) {
        if (level.hasCharacter() && characterId != null) {
            return new String[]{"CHARACTER", characterId};
        }
        if (level.hasSkill() && skillId != null) {
            return new String[]{"SKILL", skillId};
        }
        if (level.hasTool() && toolId != null) {
            return new String[]{"TOOL", toolId};
        }
        if (level.hasMemory() && memoryId != null) {
            return new String[]{"MEMORY", memoryId};
        }
        if (level.hasWorkspace() && workspaceId != null) {
            return new String[]{"WORKSPACE", workspaceId};
        }
        return null;
    }

    /**
     * 获取当前用户ID（临时方案，需根据实际认证方式调整）
     */
    private Long getCurrentUserId() {
        // TODO: 从 SecurityContext 或其他方式获取当前用户ID
        return 1L;
    }

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