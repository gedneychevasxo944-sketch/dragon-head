package org.dragon.config.service;

import lombok.extern.slf4j.Slf4j;
import org.dragon.config.context.InheritanceContext;
import org.dragon.config.dto.ConfigItemVO;
import org.dragon.config.dto.EffectChainVO;
import org.dragon.config.dto.ImpactAnalysis;
import org.dragon.config.enums.ConfigLevel;
import org.dragon.config.store.ConfigStore;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ConfigApplication 配置应用服务
 *
 * <p>提供配置的查询、设置、列表等操作，是配置系统的主要入口。
 */
@Slf4j
@Service
public class ConfigApplication {

    private final ConfigEffectService configEffectService;
    private final ConfigImpactAnalyzer configImpactAnalyzer;
    private final ConfigPromptService configPromptService;
    private final StoreFactory storeFactory;

    public ConfigApplication(ConfigEffectService configEffectService,
                             ConfigImpactAnalyzer configImpactAnalyzer,
                             ConfigPromptService configPromptService,
                             StoreFactory storeFactory) {
        this.configEffectService = configEffectService;
        this.configImpactAnalyzer = configImpactAnalyzer;
        this.configPromptService = configPromptService;
        this.storeFactory = storeFactory;
    }

    private ConfigStore configStore() {
        return storeFactory.get(ConfigStore.class);
    }

    // ==================== 配置查询 ====================

    /**
     * 获取配置项列表（含 displayStatus）
     */
    public List<ConfigItemVO> listConfigItems(InheritanceContext context) {
        List<ConfigStore.ConfigStoreItem> allItems = configStore().listAll();
        List<ConfigItemVO> result = new ArrayList<>();

        for (ConfigStore.ConfigStoreItem item : allItems) {
            ConfigEffectService.EffectiveConfig ec = configEffectService.getEffectiveConfig(item.configKey(), context);

            String displayStatus;
            if (ec.getDisplayStatus() == ConfigEffectService.DisplayStatus.SET) {
                displayStatus = "SET";
            } else if (ec.getDisplayStatus() == ConfigEffectService.DisplayStatus.USE_DEFAULT) {
                displayStatus = "USE_DEFAULT";
            } else {
                displayStatus = "NOT_SET";
            }

            result.add(ConfigItemVO.builder()
                    .key(item.configKey())
                    .scopeType(item.level().name())
                    .effectiveValue(ec.getEffectiveValue())
                    .currentValue(item.value())
                    .displayStatus(displayStatus)
                    .source(ec.getSource())
                    .dataType(ec.getValueType())
                    .build());
        }

        return result;
    }

    /**
     * 获取单个配置项的生效值
     */
    public ConfigEffectService.EffectiveConfig getEffectiveConfig(String configKey, InheritanceContext context) {
        return configEffectService.getEffectiveConfig(configKey, context);
    }

    /**
     * 获取配置生效链（继承链可视化）
     *
     * @param configKey 配置键
     * @param targetLevel 目标层级
     * @param scopeId 作用域 ID（如 workspaceId）
     * @return 生效链节点列表
     */
    public List<EffectChainVO> getEffectChain(String configKey, ConfigLevel targetLevel, String scopeId) {
        List<EffectChainVO> chain = new ArrayList<>();

        // 获取继承链（从具体到全局）
        List<ConfigLevel> inheritanceChain = getInheritanceChain(targetLevel);
        int order = 1;

        for (ConfigLevel level : inheritanceChain) {
            // 构建上下文获取该层的值
            InheritanceContext levelContext = buildContextForLevel(level, scopeId);
            Optional<Object> storeValue = configStore().get(
                    level,
                    levelContext.getWorkspaceId(),
                    levelContext.getCharacterId(),
                    levelContext.getToolId(),
                    levelContext.getSkillId(),
                    levelContext.getMemoryId(),
                    configKey
            );

            chain.add(EffectChainVO.builder()
                    .scopeType(level.name())
                    .scopeId(levelContext.getWorkspaceId())
                    .scopeName(resolveScopeName(level, levelContext.getWorkspaceId()))
                    .value(storeValue.orElse(null))
                    .isOverride(level == targetLevel)
                    .isEmpty(!storeValue.isPresent())
                    .order(order++)
                    .build());
        }

        return chain;
    }

    /**
     * 获取配置生效链（支持多 ID）
     * @param configKey 配置键
     * @param context 继承上下文
     * @return 生效链节点列表
     */
    public List<EffectChainVO> getEffectChain(String configKey, InheritanceContext context) {
        List<EffectChainVO> chain = new ArrayList<>();
        ConfigLevel targetLevel = context.getLevel();

        // 获取继承链（从具体到全局）
        List<ConfigLevel> inheritanceChain = getInheritanceChain(targetLevel);
        int order = 1;

        for (ConfigLevel level : inheritanceChain) {
            // 构建该层级的上下文，使用主上下文中的 IDs
            InheritanceContext levelContext = buildContextForLevel(level, context);
            Optional<Object> storeValue = configStore().get(
                    level,
                    levelContext.getWorkspaceId(),
                    levelContext.getCharacterId(),
                    levelContext.getToolId(),
                    levelContext.getSkillId(),
                    levelContext.getMemoryId(),
                    configKey
            );

            chain.add(EffectChainVO.builder()
                    .scopeType(level.name())
                    .scopeId(getScopeIdForLevel(level, context))
                    .scopeName(resolveScopeName(level, getScopeIdForLevel(level, context)))
                    .value(storeValue.orElse(null))
                    .isOverride(level == targetLevel)
                    .isEmpty(!storeValue.isPresent())
                    .order(order++)
                    .build());
        }

        return chain;
    }

    /**
     * 获取配置的继承链（从具体到全局）
     */
    private List<ConfigLevel> getInheritanceChain(ConfigLevel targetLevel) {
        List<ConfigLevel> chain = new ArrayList<>();
        chain.add(targetLevel);

        for (ConfigLevel candidate : ConfigLevel.values()) {
            if (candidate == targetLevel) {
                continue;
            }
            if (targetLevel.isDescendantOf(candidate) && !hasIntermediateAncestor(targetLevel, candidate)) {
                chain.add(candidate);
            }
        }

        return chain;
    }

    /**
     * 检查 level 和 candidate 之间是否有其他祖先
     */
    private boolean hasIntermediateAncestor(ConfigLevel level, ConfigLevel candidate) {
        for (ConfigLevel other : ConfigLevel.values()) {
            if (other == level || other == candidate) {
                continue;
            }
            if (level.isDescendantOf(other) && other.isDescendantOf(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据层级构建上下文
     */
    private InheritanceContext buildContextForLevel(ConfigLevel level, String scopeId) {
        return switch (level) {
            case GLOBAL_WORKSPACE -> InheritanceContext.forGlobalWorkspace(scopeId);
            case GLOBAL_CHARACTER -> InheritanceContext.forGlobalCharacter(scopeId);
            case GLOBAL_SKILL -> InheritanceContext.forGlobalSkill(scopeId);
            case GLOBAL_TOOL -> InheritanceContext.forGlobalTool(scopeId);
            case GLOBAL_MEMORY -> InheritanceContext.forGlobalMemory(scopeId);
            case GLOBAL_WS_CHAR -> InheritanceContext.forGlobalWsChar(scopeId, null);
            case GLOBAL_WS_SKILL -> InheritanceContext.forGlobalWsSkill(scopeId, null);
            case GLOBAL_WS_TOOL -> InheritanceContext.forGlobalWsTool(scopeId, null);
            case GLOBAL_CHAR_TOOL -> InheritanceContext.forGlobalCharTool(scopeId, null);
            case GLOBAL_WS_CHAR_TOOL -> InheritanceContext.forGlobalWsCharTool(scopeId, null, null);
            case STUDIO_WORKSPACE -> InheritanceContext.forStudioWorkspace(scopeId);
            case STUDIO_WS_CHAR -> InheritanceContext.forStudioWsChar(scopeId, null);
            default -> InheritanceContext.builder().level(level).workspaceId(scopeId).build();
        };
    }

    /**
     * 根据层级和父上下文构建该层级的上下文
     */
    private InheritanceContext buildContextForLevel(ConfigLevel level, InheritanceContext parentContext) {
        return switch (level) {
            case GLOBAL_WORKSPACE -> InheritanceContext.forGlobalWorkspace(parentContext.getWorkspaceId());
            case GLOBAL_CHARACTER -> InheritanceContext.forGlobalCharacter(parentContext.getCharacterId());
            case GLOBAL_SKILL -> InheritanceContext.forGlobalSkill(parentContext.getSkillId());
            case GLOBAL_TOOL -> InheritanceContext.forGlobalTool(parentContext.getToolId());
            case GLOBAL_MEMORY -> InheritanceContext.forGlobalMemory(parentContext.getMemoryId());
            case GLOBAL_WS_CHAR -> InheritanceContext.forGlobalWsChar(parentContext.getWorkspaceId(), parentContext.getCharacterId());
            case GLOBAL_WS_SKILL -> InheritanceContext.forGlobalWsSkill(parentContext.getWorkspaceId(), parentContext.getSkillId());
            case GLOBAL_WS_TOOL -> InheritanceContext.forGlobalWsTool(parentContext.getWorkspaceId(), parentContext.getToolId());
            case GLOBAL_WS_MEMORY -> InheritanceContext.forGlobalWsMemory(parentContext.getWorkspaceId(), parentContext.getMemoryId());
            case GLOBAL_CHAR_TOOL -> InheritanceContext.forGlobalCharTool(parentContext.getCharacterId(), parentContext.getToolId());
            case GLOBAL_CHAR_SKILL -> InheritanceContext.forGlobalCharSkill(parentContext.getCharacterId(), parentContext.getSkillId());
            case GLOBAL_CHAR_MEMORY -> InheritanceContext.forGlobalCharMemory(parentContext.getCharacterId(), parentContext.getMemoryId());
            case GLOBAL_WS_CHAR_TOOL -> InheritanceContext.forGlobalWsCharTool(parentContext.getWorkspaceId(), parentContext.getCharacterId(), parentContext.getToolId());
            case GLOBAL_WS_CHAR_SKILL -> InheritanceContext.forGlobalWsCharSkill(parentContext.getWorkspaceId(), parentContext.getCharacterId(), parentContext.getSkillId());
            case GLOBAL_WS_CHAR_MEMORY -> InheritanceContext.forGlobalWsCharMemory(parentContext.getWorkspaceId(), parentContext.getCharacterId(), parentContext.getMemoryId());
            case STUDIO_WORKSPACE -> InheritanceContext.forStudioWorkspace(parentContext.getWorkspaceId());
            case STUDIO_CHARACTER -> InheritanceContext.forStudioCharacter(parentContext.getCharacterId());
            case STUDIO_SKILL -> InheritanceContext.builder()
                    .level(ConfigLevel.STUDIO_SKILL)
                    .workspaceId(parentContext.getWorkspaceId())
                    .skillId(parentContext.getSkillId())
                    .build();
            case STUDIO_TOOL -> InheritanceContext.builder()
                    .level(ConfigLevel.STUDIO_TOOL)
                    .workspaceId(parentContext.getWorkspaceId())
                    .toolId(parentContext.getToolId())
                    .build();
            case STUDIO_MEMORY -> InheritanceContext.builder()
                    .level(ConfigLevel.STUDIO_MEMORY)
                    .workspaceId(parentContext.getWorkspaceId())
                    .memoryId(parentContext.getMemoryId())
                    .build();
            case STUDIO_WS_CHAR -> InheritanceContext.forStudioWsChar(parentContext.getWorkspaceId(), parentContext.getCharacterId());
            case STUDIO_WS_SKILL -> InheritanceContext.forStudioWsSkill(parentContext.getWorkspaceId(), parentContext.getSkillId());
            case STUDIO_WS_MEMORY -> InheritanceContext.forStudioWsMemory(parentContext.getWorkspaceId(), parentContext.getMemoryId());
            case STUDIO_WS_TOOL -> InheritanceContext.forStudioWsTool(parentContext.getWorkspaceId(), parentContext.getToolId());
            case STUDIO_CHAR_TOOL -> InheritanceContext.forStudioCharTool(parentContext.getCharacterId(), parentContext.getToolId());
            case STUDIO_CHAR_SKILL -> InheritanceContext.forStudioCharSkill(parentContext.getCharacterId(), parentContext.getSkillId());
            case STUDIO_CHAR_MEMORY -> InheritanceContext.forStudioCharMemory(parentContext.getCharacterId(), parentContext.getMemoryId());
            case STUDIO_WS_CHAR_TOOL -> InheritanceContext.forStudioWsCharTool(parentContext.getWorkspaceId(), parentContext.getCharacterId(), parentContext.getToolId());
            case STUDIO_WS_CHAR_SKILL -> InheritanceContext.forStudioWsCharSkill(parentContext.getWorkspaceId(), parentContext.getCharacterId(), parentContext.getSkillId());
            case STUDIO_WS_CHAR_MEMORY -> InheritanceContext.forStudioWsCharMemory(parentContext.getWorkspaceId(), parentContext.getCharacterId(), parentContext.getMemoryId());
            case OBSERVER_GLOBAL_WORKSPACE -> InheritanceContext.forObserverGlobalWorkspace(parentContext.getWorkspaceId());
            case OBSERVER_GLOBAL_CHARACTER -> InheritanceContext.forObserverGlobalCharacter(parentContext.getCharacterId());
            case OBSERVER_GLOBAL_SKILL -> InheritanceContext.forObserverGlobalSkill(parentContext.getSkillId());
            case OBSERVER_GLOBAL_TOOL -> InheritanceContext.forObserverGlobalTool(parentContext.getToolId());
            case OBSERVER_GLOBAL_MEMORY -> InheritanceContext.forObserverGlobalMemory(parentContext.getMemoryId());
            case OBSERVER_GLOBAL_WS_CHAR -> InheritanceContext.forObserverGlobalWsChar(parentContext.getWorkspaceId(), parentContext.getCharacterId());
            case OBSERVER_GLOBAL_WS_SKILL -> InheritanceContext.forObserverGlobalWsSkill(parentContext.getWorkspaceId(), parentContext.getSkillId());
            case OBSERVER_GLOBAL_WS_MEMORY -> InheritanceContext.forObserverGlobalWsMemory(parentContext.getWorkspaceId(), parentContext.getMemoryId());
            case OBSERVER_GLOBAL_WS_TOOL -> InheritanceContext.forObserverGlobalWsTool(parentContext.getWorkspaceId(), parentContext.getToolId());
            case OBSERVER_GLOBAL_CHAR_TOOL -> InheritanceContext.forObserverGlobalCharTool(parentContext.getCharacterId(), parentContext.getToolId());
            case OBSERVER_GLOBAL_CHAR_SKILL -> InheritanceContext.forObserverGlobalCharSkill(parentContext.getCharacterId(), parentContext.getSkillId());
            case OBSERVER_GLOBAL_CHAR_MEMORY -> InheritanceContext.forObserverGlobalCharMemory(parentContext.getCharacterId(), parentContext.getMemoryId());
            case OBSERVER_GLOBAL_WS_CHAR_TOOL -> InheritanceContext.forObserverGlobalWsCharTool(parentContext.getWorkspaceId(), parentContext.getCharacterId(), parentContext.getToolId());
            case OBSERVER_GLOBAL_WS_CHAR_SKILL -> InheritanceContext.forObserverGlobalWsCharSkill(parentContext.getWorkspaceId(), parentContext.getCharacterId(), parentContext.getSkillId());
            case OBSERVER_GLOBAL_WS_CHAR_MEMORY -> InheritanceContext.forObserverGlobalWsCharMemory(parentContext.getWorkspaceId(), parentContext.getCharacterId(), parentContext.getMemoryId());
            default -> InheritanceContext.builder().level(level)
                    .workspaceId(parentContext.getWorkspaceId())
                    .characterId(parentContext.getCharacterId())
                    .toolId(parentContext.getToolId())
                    .skillId(parentContext.getSkillId())
                    .memoryId(parentContext.getMemoryId())
                    .build();
        };
    }

    /**
     * 获取层级对应的 scopeId（用于显示）
     */
    private String getScopeIdForLevel(ConfigLevel level, InheritanceContext context) {
        return switch (level) {
            case GLOBAL_WORKSPACE, STUDIO_WORKSPACE, OBSERVER_GLOBAL_WORKSPACE -> context.getWorkspaceId();
            case GLOBAL_CHARACTER, STUDIO_CHARACTER, OBSERVER_GLOBAL_CHARACTER -> context.getCharacterId();
            case GLOBAL_SKILL, STUDIO_SKILL, OBSERVER_GLOBAL_SKILL -> context.getSkillId();
            case GLOBAL_TOOL, STUDIO_TOOL, OBSERVER_GLOBAL_TOOL -> context.getToolId();
            case GLOBAL_MEMORY, STUDIO_MEMORY, OBSERVER_GLOBAL_MEMORY -> context.getMemoryId();
            case GLOBAL_WS_CHAR, GLOBAL_WS_SKILL, GLOBAL_WS_TOOL, GLOBAL_WS_MEMORY,
                 STUDIO_WS_CHAR, STUDIO_WS_SKILL, STUDIO_WS_MEMORY, STUDIO_WS_TOOL,
                 OBSERVER_GLOBAL_WS_CHAR, OBSERVER_GLOBAL_WS_SKILL, OBSERVER_GLOBAL_WS_MEMORY, OBSERVER_GLOBAL_WS_TOOL -> context.getWorkspaceId();
            case GLOBAL_CHAR_TOOL, GLOBAL_CHAR_SKILL, GLOBAL_CHAR_MEMORY,
                 STUDIO_CHAR_TOOL, STUDIO_CHAR_SKILL, STUDIO_CHAR_MEMORY,
                 OBSERVER_GLOBAL_CHAR_TOOL, OBSERVER_GLOBAL_CHAR_SKILL, OBSERVER_GLOBAL_CHAR_MEMORY -> context.getCharacterId();
            case GLOBAL_WS_CHAR_TOOL, GLOBAL_WS_CHAR_SKILL, GLOBAL_WS_CHAR_MEMORY,
                 STUDIO_WS_CHAR_TOOL, STUDIO_WS_CHAR_SKILL, STUDIO_WS_CHAR_MEMORY,
                 OBSERVER_GLOBAL_WS_CHAR_TOOL, OBSERVER_GLOBAL_WS_CHAR_SKILL, OBSERVER_GLOBAL_WS_CHAR_MEMORY -> context.getWorkspaceId();
            default -> null;
        };
    }

    /**
     * 解析作用域名称
     */
    private String resolveScopeName(ConfigLevel level, String scopeId) {
        if (scopeId == null) {
            return "全局";
        }
        return scopeId;
    }

    // ==================== 配置设置 ====================

    /**
     * 设置配置值
     */
    public void setConfigValue(String configKey, Object value, InheritanceContext context) {
        configStore().set(
                context.getLevel(),
                context.getWorkspaceId(),
                context.getCharacterId(),
                context.getToolId(),
                context.getSkillId(),
                context.getMemoryId(),
                configKey,
                value
        );
        log.info("[ConfigApplication] Config set: {} = {} at {}", configKey, value, context.getLevel());
    }

    /**
     * 设置配置值（简化版，只有 workspaceId）
     */
    public void setConfigValue(String configKey, Object value, ConfigLevel level, String workspaceId) {
        configStore().set(level, workspaceId, null, null, null, null, configKey, value);
        log.info("[ConfigApplication] Config set: {} = {} at {}", configKey, value, level);
    }

    /**
     * 设置配置值（只有 workspaceId 和 characterId）
     */
    public void setConfigValue(String configKey, Object value, ConfigLevel level, String workspaceId, String characterId) {
        configStore().set(level, workspaceId, characterId, null, null, null, configKey, value);
        log.info("[ConfigApplication] Config set: {} = {} at {}", configKey, value, level);
    }

    /**
     * 设置配置值（完整版）
     */
    public void setConfigValue(String configKey, Object value, ConfigLevel level,
                              String workspaceId, String characterId, String toolId,
                              String skillId, String memoryId) {
        configStore().set(level, workspaceId, characterId, toolId, skillId, memoryId, configKey, value);
        log.info("[ConfigApplication] Config set: {} = {} at {}", configKey, value, level);
    }

    /**
     * 删除配置
     */
    public void deleteConfigValue(String configKey, InheritanceContext context) {
        configStore().delete(
                context.getLevel(),
                context.getWorkspaceId(),
                context.getCharacterId(),
                context.getToolId(),
                context.getSkillId(),
                context.getMemoryId(),
                configKey
        );
        log.info("[ConfigApplication] Config deleted: {} at {}", configKey, context.getLevel());
    }

    // ==================== Prompt 管理（委托） ====================

    /**
     * 获取 Prompt
     */
    public String getPrompt(String promptKey, InheritanceContext context) {
        return configPromptService.getPrompt(promptKey, context);
    }

    /**
     * 获取全局默认 Prompt
     */
    public String getGlobalPrompt(String promptKey, String defaultValue) {
        return configPromptService.getGlobalPrompt(promptKey, defaultValue);
    }

    /**
     * 获取 Prompt（兼容旧 API，4 级层级查找）
     */
    public String getPrompt(String workspace, String characterId, String promptKey) {
        return configPromptService.getPrompt(workspace, characterId, promptKey);
    }

    /**
     * 获取 Workspace 级别 Prompt
     */
    public String getWorkspacePrompt(String workspace, String promptKey) {
        return configPromptService.getWorkspacePrompt(workspace, promptKey);
    }

    /**
     * 获取 Workspace 级别 Prompt（带默认值）
     */
    public String getWorkspacePrompt(String workspace, String promptKey, String defaultValue) {
        return configPromptService.getWorkspacePrompt(workspace, promptKey, defaultValue);
    }

    /**
     * 设置全局 Prompt
     */
    public void setGlobalPrompt(String promptKey, String content) {
        configPromptService.setGlobalPrompt(promptKey, content);
    }

    /**
     * 设置 Workspace 级别 Prompt
     */
    public void setWorkspacePrompt(String workspace, String promptKey, String content) {
        configPromptService.setWorkspacePrompt(workspace, promptKey, content);
    }

    /**
     * 设置 Workspace+Character 级别 Prompt
     */
    public void setWorkspaceCharacterPrompt(String workspace, String characterId, String promptKey, String content) {
        configPromptService.setWorkspaceCharacterPrompt(workspace, characterId, promptKey, content);
    }

    // ==================== 影响面分析（委托） ====================

    /**
     * 影响面分析
     */
    public ImpactAnalysis analyzeImpact(ConfigLevel level, String workspaceId, String characterId,
                                      String toolId, String skillId, String memoryId) {
        return configImpactAnalyzer.analyzeImpact(level, workspaceId, characterId, toolId, skillId, memoryId);
    }

    /**
     * 简化版影响分析
     */
    public ImpactAnalysis analyzeImpact(ConfigLevel level, String workspaceId) {
        return configImpactAnalyzer.analyzeImpact(level, workspaceId);
    }

    /**
     * 影响分析（兼容旧 API）
     */
    public ImpactAnalysis analyzeImpact(String configKey, ConfigLevel level, String workspaceId, String characterId) {
        return configImpactAnalyzer.analyzeImpact(configKey, level, workspaceId, characterId);
    }

    // ==================== 便捷的配置值获取方法 ====================

    public String getStringValue(String configKey, InheritanceContext context, String defaultValue) {
        ConfigEffectService.EffectiveConfig ec = getEffectiveConfig(configKey, context);
        if (ec.getEffectiveValue() != null) {
            return ec.getEffectiveValue().toString();
        }
        return defaultValue;
    }

    public String getStringValue(String configKey, InheritanceContext context) {
        return getStringValue(configKey, context, null);
    }

    public int getIntValue(String configKey, InheritanceContext context, int defaultValue) {
        ConfigEffectService.EffectiveConfig ec = getEffectiveConfig(configKey, context);
        if (ec.getEffectiveValue() != null) {
            if (ec.getEffectiveValue() instanceof Number) {
                return ((Number) ec.getEffectiveValue()).intValue();
            }
            return Integer.parseInt(ec.getEffectiveValue().toString());
        }
        return defaultValue;
    }

    public int getIntValue(String configKey, InheritanceContext context) {
        return getIntValue(configKey, context, 0);
    }

    public long getLongValue(String configKey, InheritanceContext context, long defaultValue) {
        ConfigEffectService.EffectiveConfig ec = getEffectiveConfig(configKey, context);
        if (ec.getEffectiveValue() != null) {
            if (ec.getEffectiveValue() instanceof Number) {
                return ((Number) ec.getEffectiveValue()).longValue();
            }
            return Long.parseLong(ec.getEffectiveValue().toString());
        }
        return defaultValue;
    }

    public long getLongValue(String configKey, InheritanceContext context) {
        return getLongValue(configKey, context, 0L);
    }

    public double getDoubleValue(String configKey, InheritanceContext context, double defaultValue) {
        ConfigEffectService.EffectiveConfig ec = getEffectiveConfig(configKey, context);
        if (ec.getEffectiveValue() != null) {
            if (ec.getEffectiveValue() instanceof Number) {
                return ((Number) ec.getEffectiveValue()).doubleValue();
            }
            return Double.parseDouble(ec.getEffectiveValue().toString());
        }
        return defaultValue;
    }

    public double getDoubleValue(String configKey, InheritanceContext context) {
        return getDoubleValue(configKey, context, 0.0);
    }

    public boolean getBooleanValue(String configKey, InheritanceContext context, boolean defaultValue) {
        ConfigEffectService.EffectiveConfig ec = getEffectiveConfig(configKey, context);
        if (ec.getEffectiveValue() != null) {
            if (ec.getEffectiveValue() instanceof Boolean) {
                return (Boolean) ec.getEffectiveValue();
            }
            return Boolean.parseBoolean(ec.getEffectiveValue().toString());
        }
        return defaultValue;
    }

    public boolean getBooleanValue(String configKey, InheritanceContext context) {
        return getBooleanValue(configKey, context, false);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getListValue(String configKey, InheritanceContext context, List<T> defaultValue) {
        ConfigEffectService.EffectiveConfig ec = getEffectiveConfig(configKey, context);
        if (ec.getEffectiveValue() != null) {
            if (ec.getEffectiveValue() instanceof List) {
                return (List<T>) ec.getEffectiveValue();
            }
        }
        return defaultValue;
    }

    public <T> List<T> getListValue(String configKey, InheritanceContext context) {
        return getListValue(configKey, context, List.of());
    }
}