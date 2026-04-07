package org.dragon.config.service;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dragon.config.context.InheritanceContext;
import org.dragon.config.enums.ConfigLevel;
import org.dragon.config.store.ConfigStore;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ConfigEffectService 配置生效值服务
 *
 * <p>负责配置的继承链计算和生效值查询。
 *
 * <p>继承链计算：
 * <ul>
 *   <li>从具体粒度到全局粒度遍历</li>
 *   <li>使用 ConfigLevel.isDescendantOf() 检查继承关系</li>
 *   <li>第一个匹配的配置值即为生效值</li>
 * </ul>
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
        if (context == null || context.getLevel() == null) {
            return buildNotSet(configKey);
        }

        ConfigLevel targetLevel = context.getLevel();

        // 获取继承链（从具体到全局）
        List<ConfigLevel> chain = getInheritanceChain(targetLevel);
        log.debug("[ConfigEffectService] Inheritance chain for {}: {}", targetLevel, chain);

        // 遍历继承链查找配置
        for (ConfigLevel level : chain) {
            Optional<Object> storeValue = getStoreValue(configKey, level, context);
            if (storeValue.isPresent()) {
                log.debug("[ConfigEffectService] Found value at level {}: {}", level, storeValue.get());
                return EffectiveConfig.builder()
                        .configKey(configKey)
                        .effectiveValue(storeValue.get())
                        .valueType(determineValueType(storeValue.get()))
                        .source(level.name())
                        .isInherited(level != targetLevel)
                        .displayStatus(DisplayStatus.SET)
                        .build();
            }
        }

        // 未找到，返回 NOT_SET
        log.debug("[ConfigEffectService] No value found for key: {}", configKey);
        return buildNotSet(configKey);
    }

    /**
     * 获取继承链（从具体到全局）
     *
     * <p>通过 AND 运算检查继承关系
     */
    private List<ConfigLevel> getInheritanceChain(ConfigLevel targetLevel) {
        List<ConfigLevel> chain = new ArrayList<>();
        chain.add(targetLevel);

        // 找到所有祖先层级
        for (ConfigLevel candidate : ConfigLevel.values()) {
            if (candidate == targetLevel) {
                continue;
            }
            // candidate 是 targetLevel 的祖先当且仅当：
            // 1. targetLevel 继承 candidate
            // 2. candidate 不继承任何其他更远的祖先（避免重复添加）
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
            // 如果 level 继承 other，other 继承 candidate，说明 other 是中间祖先
            if (level.isDescendantOf(other) && other.isDescendantOf(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从存储中获取配置值
     */
    private Optional<Object> getStoreValue(String configKey, ConfigLevel level, InheritanceContext context) {
        return configStore.get(
                level,
                context.getWorkspaceId(),
                context.getCharacterId(),
                context.getToolId(),
                context.getSkillId(),
                context.getMemoryId(),
                configKey
        );
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
        /** 值来源 */
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