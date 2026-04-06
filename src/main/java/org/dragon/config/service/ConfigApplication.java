package org.dragon.config.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.config.context.InheritanceContext;
import org.dragon.config.dto.ConfigItemVO;
import org.dragon.config.dto.ImpactAnalysis;
import org.dragon.config.enums.ConfigLevel;
import org.dragon.config.store.ConfigStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ConfigApplication 配置应用服务
 *
 * <p>提供配置的查询、设置、列表等操作，是配置系统的主要入口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigApplication {

    private final ConfigStore configStore;
    private final ConfigEffectService configEffectService;
    private final ConfigImpactAnalyzer configImpactAnalyzer;
    private final ConfigPromptService configPromptService;

    // ==================== 配置查询 ====================

    /**
     * 获取配置项列表（含 displayStatus）
     */
    public List<ConfigItemVO> listConfigItems(InheritanceContext context) {
        List<ConfigStore.ConfigStoreItem> allItems = configStore.listAll();
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
                    .configKey(item.configKey())
                    .level(item.level())
                    .effectiveValue(ec.getEffectiveValue())
                    .storeValue(item.value())
                    .displayStatus(displayStatus)
                    .source(ec.getSource())
                    .valueType(ec.getValueType())
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

    // ==================== 配置设置 ====================

    /**
     * 设置配置值
     */
    public void setConfigValue(String configKey, Object value, InheritanceContext context) {
        configStore.set(
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
        configStore.set(level, workspaceId, null, null, null, null, configKey, value);
        log.info("[ConfigApplication] Config set: {} = {} at {}", configKey, value, level);
    }

    /**
     * 设置配置值（只有 workspaceId 和 characterId）
     */
    public void setConfigValue(String configKey, Object value, ConfigLevel level, String workspaceId, String characterId) {
        configStore.set(level, workspaceId, characterId, null, null, null, configKey, value);
        log.info("[ConfigApplication] Config set: {} = {} at {}", configKey, value, level);
    }

    /**
     * 设置配置值（完整版）
     */
    public void setConfigValue(String configKey, Object value, ConfigLevel level,
                              String workspaceId, String characterId, String toolId,
                              String skillId, String memoryId) {
        configStore.set(level, workspaceId, characterId, toolId, skillId, memoryId, configKey, value);
        log.info("[ConfigApplication] Config set: {} = {} at {}", configKey, value, level);
    }

    /**
     * 删除配置
     */
    public void deleteConfigValue(String configKey, InheritanceContext context) {
        configStore.delete(
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