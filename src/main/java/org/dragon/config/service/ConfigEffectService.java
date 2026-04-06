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

import java.util.Optional;

/**
 * ConfigEffectService 配置生效值服务
 *
 * <p>负责配置的继承链计算和生效值查询
 *
 * <p>继承链顺序（从低到高）：
 * GLOBAL &lt; STUDIO &lt; WORKSPACE &lt; CHARACTER &lt; TOOL &lt; SKILL
 *
 * <p>配置查找算法：
 * <ol>
 *   <li>获取继承链（从具体到全局）</li>
 *   <li>遍历继承链，在 config_store 中查找</li>
 *   <li>若找到，返回 storeValue，source = 查到的 scope</li>
 *   <li>若未找到，返回 NOT_SET（默认值查找 TODO: 实现）</li>
 * </ol>
 */
@Slf4j
@Service
public class ConfigEffectService {

    private final ConfigStore configStore;

    public ConfigEffectService(StoreFactory storeFactory) {
        this.configStore = storeFactory.get(ConfigStore.class);
    }

    /**
     * 获取配置的生效值
     *
     * @param configKey 配置键
     * @param context 继承上下文
     * @return 生效值结果
     */
    public EffectiveConfig getEffectiveConfig(String configKey, InheritanceContext context) {
        if (context == null || context.getScopes() == null || context.getScopes().isEmpty()) {
            return buildNotSet(configKey);
        }

        // 遍历继承链（从具体到全局）
        for (ContextScope scope : context.getInheritanceChain()) {
            Optional<Object> storeValue = getStoreValue(configKey, scope, context);
            if (storeValue.isPresent()) {
                return EffectiveConfig.builder()
                        .configKey(configKey)
                        .effectiveValue(storeValue.get())
                        .valueType(determineValueType(storeValue.get()))
                        .source(scope.getScopeType().name())
                        .isInherited(scope != context.getMostSpecificScope())
                        .displayStatus(DisplayStatus.SET)
                        .build();
            }
        }

        // 查找默认值（Phase 4 实现）
        return findDefault(configKey);
    }

    /**
     * 从存储中获取配置值
     */
    private Optional<Object> getStoreValue(String configKey, ContextScope scope, InheritanceContext context) {
        int scopeBits = ScopeBits.of(scope.getScopeType());
        String workspaceId = extractScopeId(context, ConfigScopeType.WORKSPACE);
        String characterId = extractScopeId(context, ConfigScopeType.CHARACTER);
        String toolId = extractScopeId(context, ConfigScopeType.TOOL);
        String skillId = extractScopeId(context, ConfigScopeType.SKILL);

        return configStore.get(configKey, scopeBits, workspaceId, characterId, toolId, skillId);
    }

    /**
     * 从上下文中提取指定类型的 scopeId
     */
    private String extractScopeId(InheritanceContext context, ConfigScopeType type) {
        if (context == null || context.getScopes() == null) {
            return null;
        }
        for (ContextScope scope : context.getScopes()) {
            if (scope.getScopeType() == type) {
                return scope.getScopeId();
            }
        }
        return null;
    }

    /**
     * 查找默认值（Phase 4 实现）
     */
    private EffectiveConfig findDefault(String configKey) {
        // TODO: Phase 4 实现默认值查找
        return buildNotSet(configKey);
    }

    private EffectiveConfig buildNotSet(String configKey) {
        return EffectiveConfig.builder()
                .configKey(configKey)
                .effectiveValue(null)
                .valueType("STRING")
                .source(null)
                .isInherited(false)
                .displayStatus(DisplayStatus.NOT_SET)
                .build();
    }

    /**
     * 判断值的类型
     */
    private String determineValueType(Object value) {
        if (value == null) return "STRING";
        if (value instanceof Number) return "NUMBER";
        if (value instanceof Boolean) return "BOOLEAN";
        if (value instanceof java.util.List) return "LIST";
        if (value instanceof java.util.Map) return "OBJECT";
        return "STRING";
    }

    /**
     * 生效值结果
     */
    @Data
    @Builder
    public static class EffectiveConfig {
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
}