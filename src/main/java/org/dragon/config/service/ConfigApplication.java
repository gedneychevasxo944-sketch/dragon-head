package org.dragon.config.service;

import lombok.Builder;
import lombok.Data;
import org.dragon.config.enums.ConfigScopeType;
import org.dragon.config.store.ConfigDefinitionStore;
import org.dragon.config.store.ConfigKey;
import org.dragon.config.store.ConfigStore;
import org.dragon.datasource.entity.ConfigDefinition;
import org.dragon.store.StoreFactory;
import lombok.extern.slf4j.Slf4j;
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
    private final ConfigDefinitionStore configDefinitionStore;
    private final ConfigEffectService configEffectService;
    private final ConfigImpactAnalyzer configImpactAnalyzer;

    public ConfigApplication(StoreFactory storeFactory, ConfigEffectService configEffectService,
                            ConfigImpactAnalyzer configImpactAnalyzer) {
        this.configStore = storeFactory.get(ConfigStore.class);
        this.configDefinitionStore = storeFactory.get(ConfigDefinitionStore.class);
        this.configEffectService = configEffectService;
        this.configImpactAnalyzer = configImpactAnalyzer;
    }

    /**
     * 获取配置项列表（含 displayStatus）
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @return 配置项列表
     */
    public List<ConfigItemVO> listConfigItems(ConfigScopeType scopeType, String scopeId) {
        return listConfigItems(scopeType, scopeId, null);
    }

    /**
     * 获取配置项列表（含 displayStatus）
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param targetId 目标 ID（如 MEMBER 时为 memberId/characterId，WORKSPACE_REF_OVERRIDE 时为被引用资产 ID）
     * @return 配置项列表
     */
    public List<ConfigItemVO> listConfigItems(ConfigScopeType scopeType, String scopeId, String targetId) {
        // 1. 获取该 scope 下所有配置定义
        List<ConfigDefinition> definitions = configDefinitionStore.findByScopeType(scopeType.name());

        // 2. 为每个定义计算生效值
        List<ConfigItemVO> items = new ArrayList<>();
        for (ConfigDefinition def : definitions) {
            String configKey = def.getConfigKey().replace(scopeType.name() + ":", "");
            ConfigEffectService.EffectiveConfig effective = configEffectService.getEffectiveConfig(
                    scopeType, scopeId, configKey, targetId);

            ConfigItemVO item = ConfigItemVO.builder()
                    .configKey(effective.getConfigKey())
                    .configKeyFull(buildFullKey(scopeType, scopeId, effective.getConfigKey(), targetId))
                    .effectiveValue(effective.getEffectiveValue())
                    .storeValue(effective.getDisplayStatus() == ConfigEffectService.DisplayStatus.SET
                            ? effective.getEffectiveValue() : null)
                    .displayStatus(effective.getDisplayStatus().name())
                    .source(effective.getSource())
                    .valueType(effective.getValueType())
                    .description(def.getDescription())
                    .defaultValue(def.getDefaultValue())
                    .build();

            items.add(item);
        }

        return items;
    }

    /**
     * 获取单个配置项的生效值
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param configKey 配置键
     * @return 生效值结果
     */
    public ConfigEffectService.EffectiveConfig getEffectiveValue(ConfigScopeType scopeType, String scopeId, String configKey) {
        return getEffectiveValue(scopeType, scopeId, configKey, null);
    }

    /**
     * 获取单个配置项的生效值
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param configKey 配置键
     * @param targetId 目标 ID（如 MEMBER 时为 memberId/characterId）
     * @return 生效值结果
     */
    public ConfigEffectService.EffectiveConfig getEffectiveValue(ConfigScopeType scopeType, String scopeId, String configKey, String targetId) {
        return configEffectService.getEffectiveConfig(scopeType, scopeId, configKey, targetId);
    }

    /**
     * 设置配置值
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param configKey 配置键
     * @param value 配置值
     */
    public void setConfigValue(ConfigScopeType scopeType, String scopeId, String configKey, Object value) {
        setConfigValue(scopeType, scopeId, configKey, null, value);
    }

    /**
     * 设置配置值
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param configKey 配置键
     * @param targetId 目标 ID（如 MEMBER 时为 memberId/characterId）
     * @param value 配置值
     */
    public void setConfigValue(ConfigScopeType scopeType, String scopeId, String configKey, String targetId, Object value) {
        ConfigKey key = buildConfigKey(scopeType, scopeId, configKey, targetId);
        configStore.set(key, value);
        log.info("[ConfigApplication] Set config: {} -> {}", key.toFullKey(), value);
    }

    /**
     * 删除配置
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param configKey 配置键
     */
    public void deleteConfigValue(ConfigScopeType scopeType, String scopeId, String configKey) {
        deleteConfigValue(scopeType, scopeId, configKey, null);
    }

    /**
     * 删除配置
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param configKey 配置键
     * @param targetId 目标 ID
     */
    public void deleteConfigValue(ConfigScopeType scopeType, String scopeId, String configKey, String targetId) {
        ConfigKey key = buildConfigKey(scopeType, scopeId, configKey, targetId);
        configStore.delete(key);
        log.info("[ConfigApplication] Deleted config: {}", key.toFullKey());
    }

    /**
     * 构建完整配置键
     */
    private ConfigKey buildConfigKey(ConfigScopeType scopeType, String scopeId, String configKey, String targetId) {
        return switch (scopeType) {
            case GLOBAL -> ConfigKey.global(configKey);
            case STUDIO -> ConfigKey.studio(scopeId, configKey);
            case WORKSPACE -> ConfigKey.workspace(scopeId, configKey);
            case CHARACTER -> ConfigKey.character(scopeId, configKey);
            case MEMORY -> ConfigKey.memory(scopeId, "WORKSPACE", scopeId, configKey);
            case OBSERVER -> ConfigKey.observer(scopeId, configKey);
            case SKILL -> ConfigKey.skill(scopeId, configKey);
            case TOOL -> ConfigKey.tool(scopeId, configKey);
            case MEMBER -> ConfigKey.workspaceMember(scopeId, targetId != null ? targetId : "-", configKey);
            case WORKSPACE_REF_OVERRIDE -> ConfigKey.workspaceRefOverride(scopeId, targetId != null ? targetId : "CHARACTER", "-", configKey);
        };
    }

    /**
     * 构建完整配置键字符串
     */
    private String buildFullKey(ConfigScopeType scopeType, String scopeId, String configKey, String targetId) {
        return buildConfigKey(scopeType, scopeId, configKey, targetId).toFullKey();
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
}