package org.dragon.config.service;

import lombok.extern.slf4j.Slf4j;
import org.dragon.config.context.InheritanceContext;
import org.dragon.config.dto.AssetConfigItemVO;
import org.dragon.config.dto.AssetConfigVO;
import org.dragon.config.dto.ConfigTopologyGraphVO;
import org.dragon.config.dto.ConfigTopologyVO;
import org.dragon.config.dto.ConfigTopologyVO.ConfigChainNodeVO;
import org.dragon.config.dto.ConfigTopologyVO.TopologyNodeVO;
import org.dragon.config.enums.ConfigLevel;
import org.dragon.config.enums.ScopeBits;
import org.dragon.config.store.ConfigStore;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 配置拓扑服务
 *
 * <p>提供配置的链路拓扑查询和资产配置查询
 */
@Slf4j
@Service
public class ConfigTopologyService {

    private final ConfigStore configStore;

    public ConfigTopologyService(StoreFactory storeFactory) {
        this.configStore = storeFactory.get(ConfigStore.class);
    }

    /**
     * 获取资产配置列表
     *
     * @param assetType 资产类型（CHARACTER, WORKSPACE, TOOL, SKILL, MEMORY）
     * @param assetId 资产 ID
     * @return 资产配置信息
     */
    public AssetConfigVO getAssetConfigs(String assetType, String assetId) {
        // 根据资产类型确定对应的 ConfigLevel 和 scopeBit
        int assetBit = getAssetBit(assetType);
        ConfigLevel targetLevel = getTargetLevel(assetType);

        // 获取该资产相关的所有配置键（从元数据列表）
        List<String> configKeys = getAllConfigKeys();

        List<AssetConfigItemVO> configs = new ArrayList<>();

        for (String configKey : configKeys) {
            // 构建上下文
            InheritanceContext context = InheritanceContext.forLevel(targetLevel,
                    "WORKSPACE".equals(assetType) ? assetId : null,
                    "CHARACTER".equals(assetType) ? assetId : null,
                    "TOOL".equals(assetType) ? assetId : null,
                    "SKILL".equals(assetType) ? assetId : null,
                    "MEMORY".equals(assetType) ? assetId : null);

            // 获取生效配置
            ConfigEffectService.EffectiveConfig ec = getEffectiveConfig(configKey, context);

            // 获取继承链
            List<ConfigLevel> inheritanceChain = getInheritanceChain(targetLevel);
            List<AssetConfigItemVO.InheritanceChainNode> chainNodes = new ArrayList<>();
            for (ConfigLevel level : inheritanceChain) {
                boolean hasValue = hasConfigAtLevel(configKey, level, context);
                chainNodes.add(AssetConfigItemVO.InheritanceChainNode.builder()
                        .level(level.name())
                        .hasValue(hasValue)
                        .build());
            }

            configs.add(AssetConfigItemVO.builder()
                    .configKey(configKey)
                    .name(getConfigName(configKey))
                    .description(getConfigDescription(configKey))
                    .effectiveValue(ec.getEffectiveValue())
                    .sourceLevel(ec.getSource())
                    .sourceType(determineSourceType(ec, targetLevel))
                    .inheritanceChain(chainNodes)
                    .build());
        }

        return AssetConfigVO.builder()
                .assetType(assetType)
                .assetId(assetId)
                .scopeLevel(targetLevel.name())
                .scopeBit(targetLevel.getScopeBit())
                .configs(configs)
                .build();
    }

    /**
     * 获取单个配置的链路拓扑
     *
     * @param configKey 配置键
     * @param assetType 资产类型
     * @param assetId 资产 ID
     * @return 配置链路信息
     */
    public ConfigTopologyVO getConfigChain(String configKey, String assetType, String assetId) {
        ConfigLevel targetLevel = getTargetLevel(assetType);

        InheritanceContext context = InheritanceContext.forLevel(targetLevel,
                "WORKSPACE".equals(assetType) ? assetId : null,
                "CHARACTER".equals(assetType) ? assetId : null,
                "TOOL".equals(assetType) ? assetId : null,
                "SKILL".equals(assetType) ? assetId : null,
                "MEMORY".equals(assetType) ? assetId : null);

        // 获取继承链
        List<ConfigLevel> inheritanceChain = getInheritanceChain(targetLevel);
        List<ConfigChainNodeVO> chainNodes = new ArrayList<>();

        ConfigLevel effectiveLevel = null;
        for (ConfigLevel level : inheritanceChain) {
            if (hasConfigAtLevel(configKey, level, context)) {
                effectiveLevel = level;
                break;
            }
        }

        for (ConfigLevel level : inheritanceChain) {
            boolean hasConfig = hasConfigAtLevel(configKey, level, context);
            Object configValue = hasConfig ? getConfigValueAtLevel(configKey, level, context) : null;

            chainNodes.add(ConfigChainNodeVO.builder()
                    .level(level.name())
                    .levelName(getLevelDisplayName(level))
                    .scopeBit(level.getScopeBit())
                    .scopeId(getScopeId(level, context))
                    .scopeIdType(getScopeIdType(level))
                    .hasConfig(hasConfig)
                    .configValue(configValue)
                    .isEffective(level == effectiveLevel)
                    .build());
        }

        return ConfigTopologyVO.builder()
                .configKey(configKey)
                .chain(chainNodes)
                .build();
    }

    /**
     * 获取完整拓扑树
     *
     * @param assetType 资产类型
     * @param assetId 资产 ID
     * @return 拓扑树结构
     */
    public ConfigTopologyVO getTopology(String assetType, String assetId) {
        ConfigLevel targetLevel = getTargetLevel(assetType);

        InheritanceContext context = InheritanceContext.forLevel(targetLevel,
                "WORKSPACE".equals(assetType) ? assetId : null,
                "CHARACTER".equals(assetType) ? assetId : null,
                "TOOL".equals(assetType) ? assetId : null,
                "SKILL".equals(assetType) ? assetId : null,
                "MEMORY".equals(assetType) ? assetId : null);

        // 构建拓扑树
        TopologyNodeVO root = buildTopologyNode(ConfigLevel.GLOBAL, context);

        return ConfigTopologyVO.builder()
                .tree(root)
                .build();
    }

    /**
     * 获取配置拓扑图（用于前端图可视化）
     *
     * @param configKey 配置键
     * @param assetType 资产类型
     * @param assetId 资产 ID
     * @return 拓扑图数据（节点+边）
     */
    public ConfigTopologyGraphVO getConfigTopologyGraph(String configKey, String assetType, String assetId) {
        ConfigLevel targetLevel = getTargetLevel(assetType);
        InheritanceContext context = InheritanceContext.forLevel(targetLevel,
                "WORKSPACE".equals(assetType) ? assetId : null,
                "CHARACTER".equals(assetType) ? assetId : null,
                "TOOL".equals(assetType) ? assetId : null,
                "SKILL".equals(assetType) ? assetId : null,
                "MEMORY".equals(assetType) ? assetId : null);

        // 获取继承链（从具体到全局，逆序）
        List<ConfigLevel> inheritanceChain = getInheritanceChain(targetLevel);

        // 找到生效层级
        ConfigLevel effectiveLevel = null;
        for (ConfigLevel level : inheritanceChain) {
            if (hasConfigAtLevel(configKey, level, context)) {
                effectiveLevel = level;
                break;
            }
        }

        // 构建节点和边
        List<ConfigTopologyGraphVO.ConfigGraphNodeVO> nodes = new ArrayList<>();
        List<ConfigTopologyGraphVO.ConfigGraphEdgeVO> edges = new ArrayList<>();

        // 从左到右布局，GLOBAL 在最左
        // 所以需要反转链，让 GLOBAL 的 index 最小
        List<ConfigLevel> orderedLevels = new ArrayList<>(inheritanceChain);
        // 按 scopeBit 从小到大排序（scopeBit 越小越"全局"）
        orderedLevels.sort((a, b) -> Integer.compare(a.getScopeBit(), b.getScopeBit()));

        // 节点大小映射
        Map<String, String> sizeMap = Map.of(
                "GLOBAL", "large",
                "STUDIO", "large",
                "WORKSPACE", "medium",
                "CHARACTER", "medium",
                "TOOL", "small",
                "SKILL", "small",
                "MEMORY", "small"
        );

        for (int i = 0; i < orderedLevels.size(); i++) {
            ConfigLevel level = orderedLevels.get(i);
            boolean hasValue = hasConfigAtLevel(configKey, level, context);
            Object value = hasValue ? getConfigValueAtLevel(configKey, level, context) : null;
            boolean isEffective = level == effectiveLevel;

            // 确定层级类型
            String levelType = getLevelType(level);

            // 构建节点
            ConfigTopologyGraphVO.ConfigGraphNodeVO node = ConfigTopologyGraphVO.ConfigGraphNodeVO.builder()
                    .id(level.name())
                    .level(level.name())
                    .levelType(levelType)
                    .name(getLevelDisplayName(level))
                    .configKey(configKey)
                    .hasValue(hasValue)
                    .value(value)
                    .isEffective(isEffective)
                    .x(i * 180)  // 水平间距 180px
                    .y(150)     // 固定 y 居中
                    .size(sizeMap.getOrDefault(levelType, "medium"))
                    .status(hasValue ? "active" : "inactive")
                    .build();
            nodes.add(node);

            // 构建边（从父到子，即从左到右）
            if (i > 0) {
                ConfigLevel parent = orderedLevels.get(i - 1);
                ConfigTopologyGraphVO.ConfigGraphEdgeVO edge = ConfigTopologyGraphVO.ConfigGraphEdgeVO.builder()
                        .id(parent.name() + "->" + level.name())
                        .source(parent.name())
                        .target(level.name())
                        .type("inherits_from")
                        .label("继承")
                        .weight(hasValue ? 1.0 : 0.3)
                        .build();
                edges.add(edge);
            }
        }

        // 获取配置元数据中的名称
        String configName = getConfigName(configKey);

        return ConfigTopologyGraphVO.builder()
                .configKey(configKey)
                .configName(configName)
                .effectiveValue(effectiveLevel != null ? getConfigValueAtLevel(configKey, effectiveLevel, context) : null)
                .effectiveSource(effectiveLevel != null ? effectiveLevel.name() : null)
                .nodes(nodes)
                .edges(edges)
                .build();
    }

    private String getConfigName(String configKey) {
        List<ConfigStore.ConfigMetadata> metadata = configStore.listMetadata();
        return metadata.stream()
                .filter(m -> m.configKey().equals(configKey))
                .findFirst()
                .map(ConfigStore.ConfigMetadata::name)
                .orElse(configKey);
    }

    private String getConfigDescription(String configKey) {
        List<ConfigStore.ConfigMetadata> metadata = configStore.listMetadata();
        return metadata.stream()
                .filter(m -> m.configKey().equals(configKey))
                .findFirst()
                .map(ConfigStore.ConfigMetadata::description)
                .orElse(null);
    }

    private String getLevelType(ConfigLevel level) {
        int bit = level.getScopeBit();
        if (bit == 1) return "GLOBAL";
        if (bit == 2) return "STUDIO";
        if ((bit & ScopeBits.CHARACTER) != 0) return "CHARACTER";
        if ((bit & ScopeBits.WORKSPACE) != 0) return "WORKSPACE";
        if ((bit & ScopeBits.TOOL) != 0) return "TOOL";
        if ((bit & ScopeBits.SKILL) != 0) return "SKILL";
        if ((bit & ScopeBits.MEMORY) != 0) return "MEMORY";
        if ((bit & ScopeBits.OBSERVER) != 0) return "OBSERVER";
        return "UNKNOWN";
    }

    // ==================== 私有辅助方法 ====================

    private int getAssetBit(String assetType) {
        return switch (assetType.toUpperCase()) {
            case "CHARACTER" -> ScopeBits.CHARACTER;
            case "WORKSPACE" -> ScopeBits.WORKSPACE;
            case "TOOL" -> ScopeBits.TOOL;
            case "SKILL" -> ScopeBits.SKILL;
            case "MEMORY" -> ScopeBits.MEMORY;
            default -> 0;
        };
    }

    private ConfigLevel getTargetLevel(String assetType) {
        return switch (assetType.toUpperCase()) {
            case "CHARACTER" -> ConfigLevel.GLOBAL_CHARACTER;
            case "WORKSPACE" -> ConfigLevel.GLOBAL_WORKSPACE;
            case "TOOL" -> ConfigLevel.GLOBAL_TOOL;
            case "SKILL" -> ConfigLevel.GLOBAL_SKILL;
            case "MEMORY" -> ConfigLevel.GLOBAL_MEMORY;
            default -> ConfigLevel.GLOBAL;
        };
    }

    private List<String> getAllConfigKeys() {
        List<ConfigStore.ConfigMetadata> metadata = configStore.listMetadata();
        return metadata.stream()
                .map(ConfigStore.ConfigMetadata::configKey)
                .distinct()
                .toList();
    }

    //character,llm.temp
    private ConfigEffectService.EffectiveConfig getEffectiveConfig(String configKey, InheritanceContext context) {
        // 遍历继承链找第一个有值的
        List<ConfigLevel> chain = getInheritanceChain(context.getLevel());
        for (ConfigLevel level : chain) {
            // 根据 level 判断应该使用哪个 ID
            String workspaceId = shouldUseWorkspaceId(level) ? context.getWorkspaceId() : null;
            String characterId = shouldUseCharacterId(level) ? context.getCharacterId() : null;
            String toolId = shouldUseToolId(level) ? context.getToolId() : null;
            String skillId = shouldUseSkillId(level) ? context.getSkillId() : null;
            String memoryId = shouldUseMemoryId(level) ? context.getMemoryId() : null;

            Optional<Object> value = configStore.get(level,
                    workspaceId, characterId, toolId, skillId, memoryId,
                    configKey);
            if (value.isPresent()) {
                return ConfigEffectService.EffectiveConfig.builder()
                        .configKey(configKey)
                        .effectiveValue(value.get())
                        .source(level.name())
                        .isInherited(level != context.getLevel())
                        .build();
            }
        }
        return ConfigEffectService.EffectiveConfig.builder()
                .configKey(configKey)
                .displayStatus(ConfigEffectService.DisplayStatus.NOT_SET)
                .build();
    }

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

    //1001
    //8,4,2,1
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

    private boolean hasConfigAtLevel(String configKey, ConfigLevel level, InheritanceContext context) {
        // 根据级别获取对应的 IDs，不要传递不相关级别的 ID
        String workspaceId = shouldUseWorkspaceId(level) ? context.getWorkspaceId() : null;
        String characterId = shouldUseCharacterId(level) ? context.getCharacterId() : null;
        String toolId = shouldUseToolId(level) ? context.getToolId() : null;
        String skillId = shouldUseSkillId(level) ? context.getSkillId() : null;
        String memoryId = shouldUseMemoryId(level) ? context.getMemoryId() : null;

        Optional<Object> value = configStore.get(level,
                workspaceId, characterId, toolId, skillId, memoryId, configKey);
        return value.isPresent();
    }

    private Object getConfigValueAtLevel(String configKey, ConfigLevel level, InheritanceContext context) {
        String workspaceId = shouldUseWorkspaceId(level) ? context.getWorkspaceId() : null;
        String characterId = shouldUseCharacterId(level) ? context.getCharacterId() : null;
        String toolId = shouldUseToolId(level) ? context.getToolId() : null;
        String skillId = shouldUseSkillId(level) ? context.getSkillId() : null;
        String memoryId = shouldUseMemoryId(level) ? context.getMemoryId() : null;

        Optional<Object> value = configStore.get(level,
                workspaceId, characterId, toolId, skillId, memoryId, configKey);
        return value.orElse(null);
    }

    private boolean shouldUseWorkspaceId(ConfigLevel level) {
        return (level.getScopeBit() & ScopeBits.WORKSPACE) != 0;
    }

    private boolean shouldUseCharacterId(ConfigLevel level) {
        return (level.getScopeBit() & ScopeBits.CHARACTER) != 0;
    }

    private boolean shouldUseToolId(ConfigLevel level) {
        return (level.getScopeBit() & ScopeBits.TOOL) != 0;
    }

    private boolean shouldUseSkillId(ConfigLevel level) {
        return (level.getScopeBit() & ScopeBits.SKILL) != 0;
    }

    private boolean shouldUseMemoryId(ConfigLevel level) {
        return (level.getScopeBit() & ScopeBits.MEMORY) != 0;
    }

    private String determineSourceType(ConfigEffectService.EffectiveConfig ec, ConfigLevel targetLevel) {
        if (ec.getSource() == null) {
            return "NOT_SET";
        }
        return ec.getSource().equals(targetLevel.name()) ? "OVERRIDDEN" : "INHERITED";
    }

    private String getLevelDisplayName(ConfigLevel level) {
        int bit = level.getScopeBit();
        if ((bit & ScopeBits.CHARACTER) != 0) {
            return "角色级别";
        }
        if ((bit & ScopeBits.WORKSPACE) != 0) {
            return "工作空间级别";
        }
        if ((bit & ScopeBits.TOOL) != 0) {
            return "工具级别";
        }
        if ((bit & ScopeBits.SKILL) != 0) {
            return "技能级别";
        }
        if ((bit & ScopeBits.MEMORY) != 0) {
            return "记忆级别";
        }
        if ((bit & ScopeBits.OBSERVER) != 0) {
            return "观察者级别";
        }
        if (bit == 1) {
            return "全局级别";
        }
        return level.name();
    }

    private String getScopeId(ConfigLevel level, InheritanceContext context) {
        int bit = level.getScopeBit();
        if ((bit & ScopeBits.CHARACTER) != 0) return context.getCharacterId();
        if ((bit & ScopeBits.WORKSPACE) != 0) return context.getWorkspaceId();
        if ((bit & ScopeBits.TOOL) != 0) return context.getToolId();
        if ((bit & ScopeBits.SKILL) != 0) return context.getSkillId();
        if ((bit & ScopeBits.MEMORY) != 0) return context.getMemoryId();
        return null;
    }

    private String getScopeIdType(ConfigLevel level) {
        int bit = level.getScopeBit();
        if ((bit & ScopeBits.CHARACTER) != 0) return "CHARACTER";
        if ((bit & ScopeBits.WORKSPACE) != 0) return "WORKSPACE";
        if ((bit & ScopeBits.TOOL) != 0) return "TOOL";
        if ((bit & ScopeBits.SKILL) != 0) return "SKILL";
        if ((bit & ScopeBits.MEMORY) != 0) return "MEMORY";
        return null;
    }

    private TopologyNodeVO buildTopologyNode(ConfigLevel level, InheritanceContext context) {
        // 获取该层的配置值
        List<String> configKeys = getAllConfigKeys();
        Object configValue = null;
        boolean hasValue = false;

        // 检查是否有任何配置
        for (String configKey : configKeys) {
            Optional<Object> value = configStore.get(level,
                    context.getWorkspaceId(), context.getCharacterId(),
                    context.getToolId(), context.getSkillId(), context.getMemoryId(),
                    configKey);
            if (value.isPresent()) {
                hasValue = true;
                configValue = value.get();
                break;
            }
        }

        // 构建当前节点
        TopologyNodeVO node = TopologyNodeVO.builder()
                .level(level.name())
                .levelName(getLevelDisplayName(level))
                .scopeBit(level.getScopeBit())
                .workspaceId((level.getScopeBit() & ScopeBits.WORKSPACE) != 0 ? context.getWorkspaceId() : null)
                .characterId((level.getScopeBit() & ScopeBits.CHARACTER) != 0 ? context.getCharacterId() : null)
                .toolId((level.getScopeBit() & ScopeBits.TOOL) != 0 ? context.getToolId() : null)
                .skillId((level.getScopeBit() & ScopeBits.SKILL) != 0 ? context.getSkillId() : null)
                .memoryId((level.getScopeBit() & ScopeBits.MEMORY) != 0 ? context.getMemoryId() : null)
                .hasValue(hasValue)
                .configValue(configValue)
                .children(new ArrayList<>())
                .build();

        // 递归构建子节点
        for (ConfigLevel child : ConfigLevel.values()) {
            if (child == level) continue;
            // 检查 child 是否是 level 的直接子节点
            if (child.isDescendantOf(level) && !hasIntermediateAncestor(child, level)) {
                node.getChildren().add(buildTopologyNode(child, context));
            }
        }

        return node;
    }
}