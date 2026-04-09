package org.dragon.config.service;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dragon.config.context.InheritanceContext;
import org.dragon.config.enums.ConfigLevel;
import org.dragon.config.model.InheritanceConfig;
import org.dragon.config.model.InheritanceConfig.AssetType;
import org.dragon.config.model.InheritanceConfig.Level;
import org.dragon.config.store.ConfigStore;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * ConfigEffectService 配置生效值服务
 *
 * <p>负责配置的继承链计算和生效值查询。
 * 使用 InheritanceConfig 的显式链路替代位运算。
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
        List<Level> chain = buildInheritanceChain(targetLevel);
        log.debug("[ConfigEffectService] Inheritance chain for {}: {}", targetLevel, chain);

        // 遍历继承链查找配置
        for (Level level : chain) {
            Optional<Object> storeValue = getStoreValue(configKey, level, targetLevel, context);
            if (storeValue.isPresent()) {
                log.debug("[ConfigEffectService] Found value at level {}: {}", level, storeValue.get());
                return EffectiveConfig.builder()
                        .configKey(configKey)
                        .effectiveValue(storeValue.get())
                        .valueType(determineValueType(storeValue.get()))
                        .source(level.name())
                        .isInherited(level != chain.get(0))
                        .displayStatus(DisplayStatus.SET)
                        .build();
            }
        }

        // 未找到，返回 NOT_SET
        log.debug("[ConfigEffectService] No value found for key: {}", configKey);
        return buildNotSet(configKey);
    }

    /**
     * 根据 ConfigLevel 构建继承链（从具体到全局）
     */
    private List<Level> buildInheritanceChain(ConfigLevel configLevel) {
        Level level = InheritanceConfig.toLevel(configLevel);
        boolean hasWorkspaceParent = InheritanceConfig.hasWorkspaceParent(configLevel);

        // 根据是否需要 workspace 父级来构建链路
        AssetType assetType = toAssetType(level);
        if (assetType == null) {
            // GLOBAL 或 USER，使用简化的链路
            return switch (level) {
                case GLOBAL -> List.of(Level.GLOBAL);
                case USER -> List.of(Level.USER, Level.GLOBAL);
                default -> List.of(level, Level.USER, Level.GLOBAL);
            };
        }

        // 获取父级
        Level parentLevel = hasWorkspaceParent ? Level.WORKSPACE : null;
        return InheritanceConfig.buildChain(assetType, parentLevel);
    }

    /**
     * 将简化层级转换为资产类型
     */
    private AssetType toAssetType(Level level) {
        return switch (level) {
            case WORKSPACE -> AssetType.WORKSPACE;
            case CHARACTER -> AssetType.CHARACTER;
            case SKILL -> AssetType.SKILL;
            case TOOL -> AssetType.TOOL;
            case MEMORY -> AssetType.MEMORY;
            case USER, GLOBAL -> null;
        };
    }

    /**
     * 从存储中获取配置值
     */
    private Optional<Object> getStoreValue(String configKey, Level level, ConfigLevel targetLevel, InheritanceContext context) {
        // 根据层级确定要查询的 ConfigLevel
        ConfigLevel queryLevel = toConfigLevel(level, context);

        // 根据层级确定要传递的 ID
        String workspaceId = getIdForLevel(level, context, InheritanceContext::getWorkspaceId);
        String characterId = getIdForLevel(level, context, InheritanceContext::getCharacterId);
        String toolId = getIdForLevel(level, context, InheritanceContext::getToolId);
        String skillId = getIdForLevel(level, context, InheritanceContext::getSkillId);
        String memoryId = getIdForLevel(level, context, InheritanceContext::getMemoryId);

        return configStore.get(queryLevel, workspaceId, characterId, toolId, skillId, memoryId, configKey);
    }

    /**
     * 根据层级和上下文获取对应的 ConfigLevel
     */
    private ConfigLevel toConfigLevel(Level level, InheritanceContext context) {
        return switch (level) {
            case GLOBAL -> ConfigLevel.GLOBAL;
            case USER -> ConfigLevel.STUDIO;
            case WORKSPACE -> ConfigLevel.STUDIO_WORKSPACE;
            case CHARACTER -> {
                if (context.getWorkspaceId() != null && !context.getWorkspaceId().isEmpty()) {
                    yield ConfigLevel.GLOBAL_WS_CHAR;
                }
                yield ConfigLevel.GLOBAL_CHARACTER;
            }
            case SKILL -> {
                if (context.getWorkspaceId() != null && !context.getWorkspaceId().isEmpty()) {
                    yield ConfigLevel.GLOBAL_WS_SKILL;
                }
                yield ConfigLevel.GLOBAL_SKILL;
            }
            case TOOL -> {
                if (context.getWorkspaceId() != null && !context.getWorkspaceId().isEmpty()) {
                    yield ConfigLevel.GLOBAL_WS_TOOL;
                }
                if (context.getCharacterId() != null && !context.getCharacterId().isEmpty()) {
                    yield ConfigLevel.GLOBAL_CHAR_TOOL;
                }
                yield ConfigLevel.GLOBAL_TOOL;
            }
            case MEMORY -> {
                if (context.getWorkspaceId() != null && !context.getWorkspaceId().isEmpty()) {
                    yield ConfigLevel.GLOBAL_WS_MEMORY;
                }
                if (context.getCharacterId() != null && !context.getCharacterId().isEmpty()) {
                    yield ConfigLevel.GLOBAL_CHAR_MEMORY;
                }
                yield ConfigLevel.GLOBAL_MEMORY;
            }
        };
    }

    /**
     * 获取指定层级对应的 ID
     */
    private String getIdForLevel(Level level, InheritanceContext context, java.util.function.Function<InheritanceContext, String> getter) {
        return switch (level) {
            case GLOBAL, USER -> null;
            default -> getter.apply(context);
        };
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
        private String configKey;
        private Object effectiveValue;
        private String valueType;
        private String source;
        private boolean isInherited;
        private DisplayStatus displayStatus;
    }

    /**
     * 显示状态枚举
     */
    public enum DisplayStatus {
        SET,
        USE_DEFAULT,
        NOT_SET
    }
}
