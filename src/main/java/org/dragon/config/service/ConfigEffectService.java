package org.dragon.config.service;

import lombok.Builder;
import lombok.Data;
import org.dragon.config.enums.ConfigScopeType;
import org.dragon.config.store.ConfigDefinitionStore;
import org.dragon.config.store.ConfigStore;
import org.dragon.datasource.entity.ConfigDefinition;
import org.dragon.store.StoreFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ConfigEffectService 配置生效值服务
 *
 * <p>负责配置的继承链计算和生效值查询
 *
 * <p>继承链顺序（从低到高）：
 * <pre>
 * GLOBAL &lt; STUDIO &lt; WORKSPACE &lt; ASSET &lt; workspace_ref_override
 * </pre>
 *
 * <p>配置查找算法：
 * <ol>
 *   <li>构建继承链列表</li>
 *   <li>遍历继承链，在 config_store 中查找</li>
 *   <li>若找到，返回 storeValue，source = 查到的 scope</li>
 *   <li>若未找到，在 config_definitions 中查找 default_value</li>
 *   <li>若有默认值，返回 defaultValue，source = DEFAULT</li>
 *   <li>若均无，返回 null</li>
 * </ol>
 */
@Slf4j
@Service
public class ConfigEffectService {

    private final ConfigStore configStore;
    private final ConfigDefinitionStore configDefinitionStore;

    public ConfigEffectService(StoreFactory storeFactory) {
        this.configStore = storeFactory.get(ConfigStore.class);
        this.configDefinitionStore = storeFactory.get(ConfigDefinitionStore.class);
    }

    /**
     * 获取配置的生效值
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param configKey 配置键
     * @return 生效值结果
     */
    public EffectiveConfig getEffectiveConfig(ConfigScopeType scopeType, String scopeId, String configKey) {
        return getEffectiveConfig(scopeType, scopeId, configKey, null);
    }

    /**
     * 获取配置的生效值
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param configKey 配置键
     * @param targetId 目标 ID（如 MEMBER 时为 memberId/characterId，WORKSPACE_REF_OVERRIDE 时为被引用资产 ID）
     * @return 生效值结果
     */
    public EffectiveConfig getEffectiveConfig(ConfigScopeType scopeType, String scopeId, String configKey, String targetId) {
        return getEffectiveConfig(scopeType, scopeId, configKey, targetId, null);
    }

    /**
     * 获取配置的生效值
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param configKey 配置键
     * @param targetId 目标 ID（如 MEMBER 时为 memberId/characterId，WORKSPACE_REF_OVERRIDE 时为被引用资产 ID）
     * @param targetType 目标类型（仅 WORKSPACE_REF_OVERRIDE 时使用，如 CHARACTER, SKILL 等）
     * @return 生效值结果
     */
    public EffectiveConfig getEffectiveConfig(ConfigScopeType scopeType, String scopeId, String configKey, String targetId, String targetType) {
        // 1. 构建继承链
        List<InheritanceScope> chain = buildInheritanceChain(scopeType, scopeId, configKey, targetId, targetType);

        // 2. 遍历继承链查找配置
        for (InheritanceScope scope : chain) {
            Optional<Object> storeValue = configStore.get(scope.configKey);
            if (storeValue.isPresent()) {
                return EffectiveConfig.builder()
                        .scopeType(scopeType)
                        .scopeId(scopeId)
                        .configKey(configKey)
                        .effectiveValue(storeValue.get())
                        .valueType(determineValueType(storeValue.get()))
                        .source(scope.scopeType.name())
                        .isInherited(scope.scopeType != scopeType)
                        .displayStatus(DisplayStatus.SET)
                        .build();
            }
        }

        // 3. 查找默认值
        String definitionKey = scopeType.name() + ":" + configKey;
        Optional<ConfigDefinition> definition = configDefinitionStore.findByScopeTypeAndKey(
                scopeType.name(), definitionKey);

        if (definition.isPresent() && definition.get().getDefaultValue() != null) {
            return EffectiveConfig.builder()
                    .scopeType(scopeType)
                    .scopeId(scopeId)
                    .configKey(configKey)
                    .effectiveValue(definition.get().getDefaultValue())
                    .valueType(definition.get().getValueType())
                    .source("DEFAULT")
                    .isInherited(false)
                    .displayStatus(DisplayStatus.USE_DEFAULT)
                    .build();
        }

        // 4. 未找到配置
        return EffectiveConfig.builder()
                .scopeType(scopeType)
                .scopeId(scopeId)
                .configKey(configKey)
                .effectiveValue(null)
                .valueType("STRING")
                .source(null)
                .isInherited(false)
                .displayStatus(DisplayStatus.NOT_SET)
                .build();
    }

    /**
     * 构建配置的继承链
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param configKey 配置键
     * @param targetId 目标 ID（如 MEMBER 时为 memberId/characterId，WORKSPACE_REF_OVERRIDE 时为被引用资产 ID）
     * @param targetType 目标类型（仅 WORKSPACE_REF_OVERRIDE 时使用，如 CHARACTER, SKILL 等）
     * @return 继承链列表（从低到高）
     */
    private List<InheritanceScope> buildInheritanceChain(ConfigScopeType scopeType, String scopeId, String configKey, String targetId, String targetType) {
        List<InheritanceScope> chain = new ArrayList<>();

        switch (scopeType) {
            case CHARACTER:
                // global → studio → character
                if (scopeId != null) {
                    chain.add(new InheritanceScope(ConfigScopeType.CHARACTER, scopeId,
                            org.dragon.config.store.ConfigKey.character(scopeId, configKey)));
                }
                // fall through to add studio and global
            case STUDIO:
                if (scopeId != null) {
                    chain.add(new InheritanceScope(ConfigScopeType.STUDIO, scopeId,
                            org.dragon.config.store.ConfigKey.studio(scopeId, configKey)));
                }
                chain.add(new InheritanceScope(ConfigScopeType.GLOBAL, "-",
                        org.dragon.config.store.ConfigKey.global(configKey)));
                break;

            case MEMORY:
                // Memory 属于 Character: global → studio → character → memory
                // Memory 属于 Workspace: global → studio → workspace → memory
                // ownerType 通过 targetType 参数传入（CHARACTER 或 WORKSPACE）
                // ownerId 通过 targetId 参数传入
                if (scopeId != null) {
                    String ownerType = targetType != null ? targetType : "WORKSPACE";
                    String ownerId = targetId != null ? targetId : scopeId;
                    chain.add(new InheritanceScope(ConfigScopeType.MEMORY, scopeId,
                            org.dragon.config.store.ConfigKey.memory(scopeId, ownerType, ownerId, configKey)));
                    // 添加 owner 层级
                    if ("CHARACTER".equals(ownerType)) {
                        chain.add(new InheritanceScope(ConfigScopeType.CHARACTER, ownerId,
                                org.dragon.config.store.ConfigKey.character(ownerId, configKey)));
                    } else if ("WORKSPACE".equals(ownerType)) {
                        chain.add(new InheritanceScope(ConfigScopeType.WORKSPACE, ownerId,
                                org.dragon.config.store.ConfigKey.workspace(ownerId, configKey)));
                    }
                    chain.add(new InheritanceScope(ConfigScopeType.STUDIO, scopeId,
                            org.dragon.config.store.ConfigKey.studio(scopeId, configKey)));
                }
                chain.add(new InheritanceScope(ConfigScopeType.GLOBAL, "-",
                        org.dragon.config.store.ConfigKey.global(configKey)));
                break;

            case OBSERVER:
                // global → studio → observer
                if (scopeId != null) {
                    chain.add(new InheritanceScope(ConfigScopeType.OBSERVER, scopeId,
                            org.dragon.config.store.ConfigKey.observer(scopeId, configKey)));
                    chain.add(new InheritanceScope(ConfigScopeType.STUDIO, scopeId,
                            org.dragon.config.store.ConfigKey.studio(scopeId, configKey)));
                }
                chain.add(new InheritanceScope(ConfigScopeType.GLOBAL, "-",
                        org.dragon.config.store.ConfigKey.global(configKey)));
                break;

            case SKILL:
                // global → studio → skill
                if (scopeId != null) {
                    chain.add(new InheritanceScope(ConfigScopeType.SKILL, scopeId,
                            org.dragon.config.store.ConfigKey.skill(scopeId, configKey)));
                    chain.add(new InheritanceScope(ConfigScopeType.STUDIO, scopeId,
                            org.dragon.config.store.ConfigKey.studio(scopeId, configKey)));
                }
                chain.add(new InheritanceScope(ConfigScopeType.GLOBAL, "-",
                        org.dragon.config.store.ConfigKey.global(configKey)));
                break;

            case WORKSPACE:
                // global → studio → workspace
                if (scopeId != null) {
                    chain.add(new InheritanceScope(ConfigScopeType.WORKSPACE, scopeId,
                            org.dragon.config.store.ConfigKey.workspace(scopeId, configKey)));
                    chain.add(new InheritanceScope(ConfigScopeType.STUDIO, scopeId,
                            org.dragon.config.store.ConfigKey.studio(scopeId, configKey)));
                }
                chain.add(new InheritanceScope(ConfigScopeType.GLOBAL, "-",
                        org.dragon.config.store.ConfigKey.global(configKey)));
                break;

            case MEMBER:
                // MEMBER 配置：WORKSPACE:ws:MEMBER:memberId:key
                // 继承链: global → workspace → member
                if (scopeId != null) {
                    chain.add(new InheritanceScope(ConfigScopeType.MEMBER, scopeId,
                            org.dragon.config.store.ConfigKey.workspaceMember(scopeId, targetId != null ? targetId : "-", configKey)));
                    chain.add(new InheritanceScope(ConfigScopeType.WORKSPACE, scopeId,
                            org.dragon.config.store.ConfigKey.workspace(scopeId, configKey)));
                }
                chain.add(new InheritanceScope(ConfigScopeType.GLOBAL, "-",
                        org.dragon.config.store.ConfigKey.global(configKey)));
                break;

            case WORKSPACE_REF_OVERRIDE:
                // workspace_ref_override 优先级最高
                // key 格式: WORKSPACE_REF_OVERRIDE:workspaceId:targetType:targetId:key
                // targetType 如 CHARACTER, SKILL 等
                if (scopeId != null && targetId != null && targetType != null) {
                    chain.add(new InheritanceScope(ConfigScopeType.WORKSPACE_REF_OVERRIDE, scopeId,
                            org.dragon.config.store.ConfigKey.workspaceRefOverride(scopeId, targetType, targetId, configKey)));
                }
                break;

            case GLOBAL:
                chain.add(new InheritanceScope(ConfigScopeType.GLOBAL, "-",
                        org.dragon.config.store.ConfigKey.global(configKey)));
                break;

            default:
                // 其他类型，假设为 global → scope
                if (scopeId != null) {
                    chain.add(new InheritanceScope(scopeType, scopeId,
                            org.dragon.config.store.ConfigKey.global(configKey)));
                }
                chain.add(new InheritanceScope(ConfigScopeType.GLOBAL, "-",
                        org.dragon.config.store.ConfigKey.global(configKey)));
                break;
        }

        return chain;
    }

    /**
     * 判断值的类型
     */
    private String determineValueType(Object value) {
        if (value == null) return "STRING";
        if (value instanceof Number) return "NUMBER";
        if (value instanceof Boolean) return "BOOLEAN";
        if (value instanceof List) return "LIST";
        if (value instanceof java.util.Map) return "OBJECT";
        return "STRING";
    }

    /**
     * 继承链中的单个作用域
     */
    private static class InheritanceScope {
        final ConfigScopeType scopeType;
        final String scopeId;
        final org.dragon.config.store.ConfigKey configKey;

        InheritanceScope(ConfigScopeType scopeType, String scopeId, org.dragon.config.store.ConfigKey configKey) {
            this.scopeType = scopeType;
            this.scopeId = scopeId;
            this.configKey = configKey;
        }
    }

    /**
     * 生效值结果
     */
    @Data
    @Builder
    public static class EffectiveConfig {
        /** 原始请求的作用域类型 */
        private ConfigScopeType scopeType;
        /** 原始请求的作用域 ID */
        private String scopeId;
        /** 配置键 */
        private String configKey;
        /** 生效值 */
        private Object effectiveValue;
        /** 值类型 */
        private String valueType;
        /** 值来源：GLOBAL, STUDIO, WORKSPACE, CHARACTER, DEFAULT */
        private String source;
        /** 是否继承自父级 */
        private boolean isInherited;
        /** 显示状态 */
        private DisplayStatus displayStatus;
    }

    /**
     * 显示状态枚举
     */
    public enum DisplayStatus {
        /** 已设置（当前 scope 有 store 值） */
        SET,
        /** 未设置（使用默认值） */
        USE_DEFAULT,
        /** 未设置（无有效值） */
        NOT_SET
    }

    /**
     * 来源常量
     */
    public static final String Source = "DEFAULT";
}