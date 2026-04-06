package org.dragon.config.service;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dragon.config.context.InheritanceContext;
import org.dragon.config.context.InheritanceContext.ContextScope;
import org.dragon.config.enums.ConfigScopeType;
import org.dragon.config.enums.ScopeBits;
import org.dragon.config.store.ConfigStore;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ConfigApplication 配置应用服务
 *
 * <p>提供配置的查询、设置、列表等操作
 */
@Slf4j
@Service
public class ConfigApplication {

    private final ConfigStore configStore;
    private final ConfigEffectService configEffectService;
    private final ConfigImpactAnalyzer configImpactAnalyzer;

    public ConfigApplication(StoreFactory storeFactory, ConfigEffectService configEffectService,
                            ConfigImpactAnalyzer configImpactAnalyzer) {
        this.configStore = storeFactory.get(ConfigStore.class);
        this.configEffectService = configEffectService;
        this.configImpactAnalyzer = configImpactAnalyzer;
    }

    /**
     * 获取配置项列表（含 displayStatus）
     *
     * @param context 继承上下文
     * @return 配置项列表
     */
    public List<ConfigItemVO> listConfigItems(InheritanceContext context) {
        List<ConfigStore.ConfigStoreItem> allItems = configStore.listAll();
        List<ConfigItemVO> result = new ArrayList<>();

        // 获取上下文中最具体的 scope，用于过滤
        ContextScope mostSpecific = context != null ? context.getMostSpecificScope() : null;
        ConfigScopeType filterScopeType = mostSpecific != null ? mostSpecific.getScopeType() : null;
        String filterScopeId = mostSpecific != null ? mostSpecific.getScopeId() : null;

        for (ConfigStore.ConfigStoreItem item : allItems) {
            // 根据 context 过滤：只显示与当前 scope 相关的配置
            if (!matchesScope(item, filterScopeType, filterScopeId)) {
                continue;
            }

            // 获取生效值
            ConfigEffectService.EffectiveConfig ec = configEffectService.getEffectiveConfig(item.configKey(), context);

            // 构建 configKeyFull
            String configKeyFull = buildConfigKeyFull(item, filterScopeType);

            // 判断 displayStatus
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
                    .configKeyFull(configKeyFull)
                    .effectiveValue(ec.getEffectiveValue())
                    .storeValue(item.value())
                    .displayStatus(displayStatus)
                    .source(ec.getSource())
                    .valueType(ec.getValueType())
                    .description(null)
                    .defaultValue(null)
                    .build());
        }

        return result;
    }

    /**
     * 判断存储项是否匹配指定的 scope
     */
    private boolean matchesScope(ConfigStore.ConfigStoreItem item, ConfigScopeType scopeType, String scopeId) {
        if (scopeType == null || scopeType == ConfigScopeType.GLOBAL) {
            return true; // 全局上下文显示所有
        }

        return switch (scopeType) {
            case WORKSPACE -> item.workspaceId() != null && item.workspaceId().equals(scopeId);
            case CHARACTER -> item.characterId() != null && item.characterId().equals(scopeId);
            case TOOL -> item.toolId() != null && item.toolId().equals(scopeId);
            case SKILL -> item.skillId() != null && item.skillId().equals(scopeId);
            default -> true;
        };
    }

    /**
     * 构建完整配置键
     */
    private String buildConfigKeyFull(ConfigStore.ConfigStoreItem item, ConfigScopeType scopeType) {
        return item.configKey();
    }

    /**
     * 获取单个配置项的生效值
     *
     * @param configKey 配置键
     * @param context 继承上下文
     * @return 生效值结果
     */
    public ConfigEffectService.EffectiveConfig getEffectiveConfig(String configKey, InheritanceContext context) {
        return configEffectService.getEffectiveConfig(configKey, context);
    }

    /**
     * 设置配置值（设置到指定层级）
     *
     * @param configKey 配置键
     * @param value 配置值
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     */
    public void setConfigValue(String configKey, Object value, ConfigScopeType scopeType, String scopeId) {
        int scopeBits = ScopeBits.of(scopeType);
        String workspaceId = extractScopeId(scopeType, scopeId, ConfigScopeType.WORKSPACE);
        String characterId = extractScopeId(scopeType, scopeId, ConfigScopeType.CHARACTER);
        String toolId = extractScopeId(scopeType, scopeId, ConfigScopeType.TOOL);
        String skillId = extractScopeId(scopeType, scopeId, ConfigScopeType.SKILL);

        configStore.set(configKey, value, scopeBits, workspaceId, characterId, toolId, skillId);
        log.info("[ConfigApplication] Config set: {} = {} at {}", configKey, value, scopeType);
    }

    /**
     * 删除配置（从指定层级删除）
     *
     * @param configKey 配置键
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     */
    public void deleteConfigValue(String configKey, ConfigScopeType scopeType, String scopeId) {
        int scopeBits = ScopeBits.of(scopeType);
        String workspaceId = extractScopeId(scopeType, scopeId, ConfigScopeType.WORKSPACE);
        String characterId = extractScopeId(scopeType, scopeId, ConfigScopeType.CHARACTER);
        String toolId = extractScopeId(scopeType, scopeId, ConfigScopeType.TOOL);
        String skillId = extractScopeId(scopeType, scopeId, ConfigScopeType.SKILL);

        configStore.set(configKey, null, scopeBits, workspaceId, characterId, toolId, skillId);
        log.info("[ConfigApplication] Config deleted: {} at {}", configKey, scopeType);
    }

    /**
     * 从上下文中提取指定类型的 scopeId
     */
    private String extractScopeId(ConfigScopeType targetType, String targetId, ConfigScopeType desiredType) {
        if (targetType == desiredType) {
            return targetId;
        }
        return null;
    }

    /**
     * 配置项 VO
     */
    @Data
    @Builder
    public static class ConfigItemVO {
        /** 配置键简称（用于列表展示） */
        private String configKey;
        /** 完整配置键 */
        private String configKeyFull;
        /** 生效值 */
        private Object effectiveValue;
        /** Store 中的值（null 表示未设置） */
        private Object storeValue;
        /** 显示状态：SET, USE_DEFAULT, NOT_SET */
        private String displayStatus;
        /** 值来源：GLOBAL, STUDIO, WORKSPACE, CHARACTER, DEFAULT */
        private String source;
        /** 值类型 */
        private String valueType;
        /** 描述 */
        private String description;
        /** 默认值 */
        private Object defaultValue;
    }

    /**
     * 获取配置变更的影响分析
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param configKey 配置键
     * @return 影响分析结果
     */
    public ConfigImpactAnalyzer.ImpactAnalysis getImpactAnalysis(ConfigScopeType scopeType, String scopeId, String configKey) {
        return configImpactAnalyzer.analyzeImpact(scopeType, scopeId, configKey);
    }

    // ==================== Prompt 管理方法 ====================

    /**
     * 获取 Prompt - 使用继承链查询
     *
     * @param promptKey prompt键 (如 "observer.suggestion")
     * @param context 继承上下文
     * @return 找到的prompt或null
     */
    public String getPrompt(String promptKey, InheritanceContext context) {
        ConfigEffectService.EffectiveConfig ec = getEffectiveConfig("prompt/" + promptKey, context);
        if (ec != null && ec.getEffectiveValue() != null) {
            return (String) ec.getEffectiveValue();
        }
        return null;
    }

    /**
     * 获取 Prompt - 支持 4 级层级查找
     * 优先级: workspace+character > character > workspace > global
     *
     * @param workspace 工作空间ID (可为空)
     * @param characterId 角色ID (可为空)
     * @param promptKey prompt键 (如 "observer.suggestion")
     * @return 找到的prompt或null
     */
    public String getPrompt(String workspace, String characterId, String promptKey) {
        // 1. 最高优先级: workspace+character 级别
        if (workspace != null && characterId != null) {
            String value = getPromptAtScope("prompt/" + promptKey,
                    ConfigScopeType.CHARACTER, workspace, characterId);
            if (value != null) {
                log.debug("[ConfigApplication] Found prompt at workspace+character level");
                return value;
            }
        }

        // 2. character 级别 (独立于 workspace)
        if (characterId != null) {
            String value = getPromptAtScope("prompt/" + promptKey,
                    ConfigScopeType.CHARACTER, null, characterId);
            if (value != null) {
                log.debug("[ConfigApplication] Found prompt at character level");
                return value;
            }
        }

        // 3. workspace 级别
        if (workspace != null) {
            String value = getPromptAtScope("prompt/" + promptKey,
                    ConfigScopeType.WORKSPACE, null, workspace);
            if (value != null) {
                log.debug("[ConfigApplication] Found prompt at workspace level");
                return value;
            }
        }

        // 4. global 级别 (最低优先级)
        String value = getPromptAtScope("prompt/" + promptKey,
                ConfigScopeType.GLOBAL, null, "-");
        if (value != null) {
            log.debug("[ConfigApplication] Found prompt at global level");
            return value;
        }

        log.debug("[ConfigApplication] No prompt found for key: {}", promptKey);
        return null;
    }

    /**
     * 在指定层级获取 Prompt
     */
    private String getPromptAtScope(String promptKey, ConfigScopeType scopeType,
                                    String workspaceId, String scopeId) {
        InheritanceContext context;
        if (scopeType == ConfigScopeType.GLOBAL) {
            context = InheritanceContext.forGlobal();
        } else if (scopeType == ConfigScopeType.CHARACTER && workspaceId != null) {
            context = InheritanceContext.forCharacter(null, workspaceId, scopeId);
        } else if (scopeType == ConfigScopeType.WORKSPACE) {
            context = InheritanceContext.forWorkspace(null, scopeId);
        } else if (scopeType == ConfigScopeType.CHARACTER) {
            context = InheritanceContext.builder()
                    .scopes(new ArrayList<>(List.of(
                            ContextScope.of(ConfigScopeType.GLOBAL, "-"),
                            ContextScope.of(ConfigScopeType.CHARACTER, scopeId)
                    )))
                    .build();
        } else {
            return null;
        }

        return getPrompt(promptKey, context);
    }

    /**
     * 获取全局默认 Prompt
     *
     * @param promptKey prompt键
     * @param defaultValue 默认值
     * @return prompt或默认值
     */
    public String getGlobalPrompt(String promptKey, String defaultValue) {
        String value = getPrompt(promptKey, InheritanceContext.forGlobal());
        return value != null ? value : defaultValue;
    }

    /**
     * 获取 Workspace 级别的 Prompt
     *
     * @param workspace 工作空间ID
     * @param promptKey prompt键
     * @param defaultValue 默认值
     * @return prompt或默认值
     */
    public String getWorkspacePrompt(String workspace, String promptKey, String defaultValue) {
        String value = getPrompt(workspace, null, promptKey);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取 Workspace 级别的 Prompt（无默认值）
     *
     * @param workspace 工作空间ID
     * @param promptKey prompt键
     * @return prompt或null
     */
    public String getWorkspacePrompt(String workspace, String promptKey) {
        return getPrompt(workspace, null, promptKey);
    }

    /**
     * 设置 Prompt 到 Workspace+Character 级别
     *
     * @param workspace 工作空间ID
     * @param characterId 角色ID
     * @param promptKey prompt键
     * @param content prompt内容
     */
    public void setWorkspaceCharacterPrompt(String workspace, String characterId, String promptKey, String content) {
        int scopeBits = ScopeBits.combine(ConfigScopeType.GLOBAL, ConfigScopeType.WORKSPACE, ConfigScopeType.CHARACTER);
        configStore.set("prompt/" + promptKey, content, scopeBits, workspace, characterId, null, null);
        log.info("[ConfigApplication] Set Workspace+Character prompt: workspace={}, char={}, key={}",
                workspace, characterId, promptKey);
    }

    /**
     * 设置 Prompt 到 Workspace 级别
     *
     * @param workspace 工作空间ID
     * @param promptKey prompt键
     * @param content prompt内容
     */
    public void setWorkspacePrompt(String workspace, String promptKey, String content) {
        int scopeBits = ScopeBits.combine(ConfigScopeType.GLOBAL, ConfigScopeType.WORKSPACE);
        configStore.set("prompt/" + promptKey, content, scopeBits, workspace, null, null, null);
        log.info("[ConfigApplication] Set Workspace prompt: workspace={}, key={}", workspace, promptKey);
    }

    /**
     * 设置全局 Prompt
     *
     * @param promptKey prompt键
     * @param content prompt内容
     */
    public void setGlobalPrompt(String promptKey, String content) {
        int scopeBits = ScopeBits.of(ConfigScopeType.GLOBAL);
        configStore.set("prompt/" + promptKey, content, scopeBits, null, null, null, null);
        log.info("[ConfigApplication] Set Global prompt: key={}", promptKey);
    }

    // ==================== 便捷的配置值获取方法 ====================

    /**
     * 获取 String 配置值（带默认值）
     *
     * @param configKey 配置键
     * @param context 继承上下文
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    public String getStringValue(String configKey, InheritanceContext context, String defaultValue) {
        ConfigEffectService.EffectiveConfig ec = getEffectiveConfig(configKey, context);
        if (ec.getEffectiveValue() != null) {
            return ec.getEffectiveValue().toString();
        }
        return defaultValue;
    }

    /**
     * 获取 String 配置值（无默认值，返回 null）
     *
     * @param configKey 配置键
     * @param context 继承上下文
     * @return 配置值或 null
     */
    public String getStringValue(String configKey, InheritanceContext context) {
        return getStringValue(configKey, context, null);
    }

    /**
     * 获取 Integer 配置值（带默认值）
     *
     * @param configKey 配置键
     * @param context 继承上下文
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
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

    /**
     * 获取 Integer 配置值（无默认值，返回 0）
     *
     * @param configKey 配置键
     * @param context 继承上下文
     * @return 配置值或 0
     */
    public int getIntValue(String configKey, InheritanceContext context) {
        return getIntValue(configKey, context, 0);
    }

    /**
     * 获取 Long 配置值（带默认值）
     *
     * @param configKey 配置键
     * @param context 继承上下文
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
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

    /**
     * 获取 Long 配置值（无默认值，返回 0L）
     *
     * @param configKey 配置键
     * @param context 继承上下文
     * @return 配置值或 0L
     */
    public long getLongValue(String configKey, InheritanceContext context) {
        return getLongValue(configKey, context, 0L);
    }

    /**
     * 获取 Double 配置值（带默认值）
     *
     * @param configKey 配置键
     * @param context 继承上下文
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
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

    /**
     * 获取 Double 配置值（无默认值，返回 0.0）
     *
     * @param configKey 配置键
     * @param context 继承上下文
     * @return 配置值或 0.0
     */
    public double getDoubleValue(String configKey, InheritanceContext context) {
        return getDoubleValue(configKey, context, 0.0);
    }

    /**
     * 获取 Boolean 配置值（带默认值）
     *
     * @param configKey 配置键
     * @param context 继承上下文
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
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

    /**
     * 获取 Boolean 配置值（无默认值，返回 false）
     *
     * @param configKey 配置键
     * @param context 继承上下文
     * @return 配置值或 false
     */
    public boolean getBooleanValue(String configKey, InheritanceContext context) {
        return getBooleanValue(configKey, context, false);
    }

    /**
     * 获取 List 配置值（带默认值）
     *
     * @param configKey 配置键
     * @param context 继承上下文
     * @param defaultValue 默认值
     * @param <T> 列表元素类型
     * @return 配置值或默认值
     */
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

    /**
     * 获取 List 配置值（无默认值，返回空列表）
     *
     * @param configKey 配置键
     * @param context 继承上下文
     * @param <T> 列表元素类型
     * @return 配置值或空列表
     */
    public <T> List<T> getListValue(String configKey, InheritanceContext context) {
        return getListValue(configKey, context, List.of());
    }
}