package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.api.dto.ApiResponse;
import org.dragon.api.dto.PageResponse;
import org.dragon.config.context.InheritanceContext;
import org.dragon.config.dto.ConfigItemVO;
import org.dragon.config.dto.EffectChainVO;
import org.dragon.config.enums.ConfigLevel;
import org.dragon.config.service.ConfigApplication;
import org.dragon.config.service.ConfigEffectService;
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
            return InheritanceContext.builder().level(ConfigLevel.GLOBAL_WORKSPACE).build();
        }

        return switch (level) {
            // 系统级粒度
            case GLOBAL_WORKSPACE -> InheritanceContext.forGlobalWorkspace(workspaceId);
            case GLOBAL_CHARACTER -> InheritanceContext.forGlobalCharacter(characterId != null ? characterId : workspaceId);
            case GLOBAL_SKILL -> InheritanceContext.forGlobalSkill(skillId != null ? skillId : workspaceId);
            case GLOBAL_TOOL -> InheritanceContext.forGlobalTool(toolId != null ? toolId : workspaceId);
            case GLOBAL_MEMORY -> InheritanceContext.forGlobalMemory(memoryId != null ? memoryId : workspaceId);
            case GLOBAL_WS_CHAR -> InheritanceContext.forGlobalWsChar(workspaceId, characterId);
            case GLOBAL_WS_SKILL -> InheritanceContext.forGlobalWsSkill(workspaceId, skillId);
            case GLOBAL_WS_TOOL -> InheritanceContext.forGlobalWsTool(workspaceId, toolId);
            case GLOBAL_WS_MEMORY -> InheritanceContext.forGlobalWsMemory(workspaceId, memoryId);
            case GLOBAL_CHAR_TOOL -> InheritanceContext.forGlobalCharTool(characterId, toolId);
            case GLOBAL_CHAR_SKILL -> InheritanceContext.forGlobalCharSkill(characterId, skillId);
            case GLOBAL_CHAR_MEMORY -> InheritanceContext.forGlobalCharMemory(characterId, memoryId);
            case GLOBAL_WS_CHAR_TOOL -> InheritanceContext.forGlobalWsCharTool(workspaceId, characterId, toolId);
            case GLOBAL_WS_CHAR_SKILL -> InheritanceContext.forGlobalWsCharSkill(workspaceId, characterId, skillId);
            case GLOBAL_WS_CHAR_MEMORY -> InheritanceContext.forGlobalWsCharMemory(workspaceId, characterId, memoryId);

            // STUDIO 粒度
            case STUDIO_WORKSPACE -> InheritanceContext.forStudioWorkspace(workspaceId);
            case STUDIO_CHARACTER -> InheritanceContext.forStudioCharacter(characterId);
            case STUDIO_SKILL -> InheritanceContext.forStudioSkill(skillId);
            case STUDIO_TOOL -> InheritanceContext.forStudioTool(toolId);
            case STUDIO_MEMORY -> InheritanceContext.forStudioMemory(memoryId);
            case STUDIO_WS_CHAR -> InheritanceContext.forStudioWsChar(workspaceId, characterId);
            case STUDIO_WS_SKILL -> InheritanceContext.forStudioWsSkill(workspaceId, skillId);
            case STUDIO_WS_MEMORY -> InheritanceContext.forStudioWsMemory(workspaceId, memoryId);
            case STUDIO_WS_TOOL -> InheritanceContext.forStudioWsTool(workspaceId, toolId);
            case STUDIO_CHAR_TOOL -> InheritanceContext.forStudioCharTool(characterId, toolId);
            case STUDIO_CHAR_SKILL -> InheritanceContext.forStudioCharSkill(characterId, skillId);
            case STUDIO_CHAR_MEMORY -> InheritanceContext.forStudioCharMemory(characterId, memoryId);
            case STUDIO_WS_CHAR_TOOL -> InheritanceContext.forStudioWsCharTool(workspaceId, characterId, toolId);
            case STUDIO_WS_CHAR_SKILL -> InheritanceContext.forStudioWsCharSkill(workspaceId, characterId, skillId);
            case STUDIO_WS_CHAR_MEMORY -> InheritanceContext.forStudioWsCharMemory(workspaceId, characterId, memoryId);

            // OBSERVER 粒度
            case OBSERVER_GLOBAL_WORKSPACE -> InheritanceContext.forObserverGlobalWorkspace(workspaceId);
            case OBSERVER_GLOBAL_CHARACTER -> InheritanceContext.forObserverGlobalCharacter(characterId);
            case OBSERVER_GLOBAL_SKILL -> InheritanceContext.forObserverGlobalSkill(skillId);
            case OBSERVER_GLOBAL_TOOL -> InheritanceContext.forObserverGlobalTool(toolId);
            case OBSERVER_GLOBAL_MEMORY -> InheritanceContext.forObserverGlobalMemory(memoryId);
            case OBSERVER_GLOBAL_WS_CHAR -> InheritanceContext.forObserverGlobalWsChar(workspaceId, characterId);
            case OBSERVER_GLOBAL_WS_SKILL -> InheritanceContext.forObserverGlobalWsSkill(workspaceId, skillId);
            case OBSERVER_GLOBAL_WS_MEMORY -> InheritanceContext.forObserverGlobalWsMemory(workspaceId, memoryId);
            case OBSERVER_GLOBAL_WS_TOOL -> InheritanceContext.forObserverGlobalWsTool(workspaceId, toolId);
            case OBSERVER_GLOBAL_CHAR_TOOL -> InheritanceContext.forObserverGlobalCharTool(characterId, toolId);
            case OBSERVER_GLOBAL_CHAR_SKILL -> InheritanceContext.forObserverGlobalCharSkill(characterId, skillId);
            case OBSERVER_GLOBAL_CHAR_MEMORY -> InheritanceContext.forObserverGlobalCharMemory(characterId, memoryId);
            case OBSERVER_GLOBAL_WS_CHAR_TOOL -> InheritanceContext.forObserverGlobalWsCharTool(workspaceId, characterId, toolId);
            case OBSERVER_GLOBAL_WS_CHAR_SKILL -> InheritanceContext.forObserverGlobalWsCharSkill(workspaceId, characterId, skillId);
            case OBSERVER_GLOBAL_WS_CHAR_MEMORY -> InheritanceContext.forObserverGlobalWsCharMemory(workspaceId, characterId, memoryId);

            default -> InheritanceContext.builder().level(level)
                    .workspaceId(workspaceId)
                    .characterId(characterId)
                    .toolId(toolId)
                    .skillId(skillId)
                    .memoryId(memoryId)
                    .build();
        };
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