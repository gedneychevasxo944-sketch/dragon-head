package org.dragon.config.service;

import lombok.extern.slf4j.Slf4j;
import org.dragon.config.dto.AssetConfigItemVO;
import org.dragon.config.dto.AssetConfigVO;
import org.dragon.config.dto.ConfigTopologyVO;
import org.dragon.config.dto.ConfigTopologyVO.ConfigChainNodeVO;
import org.dragon.config.enums.ConfigLevel;
import org.dragon.config.model.InheritanceConfig;
import org.dragon.config.model.InheritanceConfig.AssetType;
import org.dragon.config.model.InheritanceConfig.Level;
import org.dragon.config.store.ConfigStore;
import org.dragon.config.store.ConfigStore.ConfigStoreItem;
import org.dragon.config.store.ConfigStore.ConfigMetadata;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 配置拓扑服务
 *
 * <p>使用显式继承链路配置替代位运算
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
     * <p>同一 configKey 的不同资产组合配置汇总在 comboValues 列表中返回
     *
     * @param assetType 资产类型（CHARACTER, WORKSPACE, TOOL, SKILL, MEMORY）
     * @param assetId 资产 ID
     * @param parentLevelName 父级层级名称（可选，如 WORKSPACE）
     * @return 资产配置信息
     */
    public AssetConfigVO getAssetConfigs(String assetType, String assetId, String parentLevelName) {
        log.info("[ConfigTopologyService] getAssetConfigs assetType={} assetId={} parentLevelName={}", assetType, assetId, parentLevelName);
        AssetType type = AssetType.valueOf(assetType.toUpperCase());
        Level parentLevel = parentLevelName != null ? Level.valueOf(parentLevelName.toUpperCase()) : null;

        // 1. 构建继承链
        List<Level> chain = InheritanceConfig.buildChain(type, parentLevel);

        // 2. 收集所有配置项（按 configKey 聚合）
        Map<String, List<ComboValueEntry>> configMap = new LinkedHashMap<>();

        for (Level level : chain) {
            // 获取该层级对应的所有 ConfigLevel
            List<ConfigLevel> configLevels = getConfigLevelsForLevel(level, type, parentLevel);

            for (ConfigLevel configLevel : configLevels) {
                List<ConfigStoreItem> items = configStore.listByLevel(configLevel);
                for (ConfigStoreItem item : items) {
                    // 检查该配置项是否与当前资产相关
                    ComboValueEntry entry = buildComboValueEntry(item, level, type, assetId, parentLevel);
                    if (entry != null) {
                        configMap.computeIfAbsent(item.configKey(), k -> new ArrayList<>()).add(entry);
                    }
                }
            }
        }

        // 3. 转换为 VO，按优先级排序
        List<AssetConfigItemVO> configs = new ArrayList<>();
        for (Map.Entry<String, List<ComboValueEntry>> entry : configMap.entrySet()) {
            String configKey = entry.getKey();
            List<ComboValueEntry> entries = entry.getValue();

            // 按优先级排序：直接配置(OVERRIDDEN)优先，然后按层级
            entries.sort((a, b) -> {
                if ("OVERRIDDEN".equals(a.sourceType) && !"OVERRIDDEN".equals(b.sourceType)) return -1;
                if (!"OVERRIDDEN".equals(a.sourceType) && "OVERRIDDEN".equals(b.sourceType)) return 1;
                return a.level.compareTo(b.level);
            });

            // 检查是否存在多个相同优先级的 OVERRIDDEN 配置（模糊情况）
            long overriddenCount = entries.stream().filter(e -> "OVERRIDDEN".equals(e.sourceType)).count();
            boolean ambiguous = overriddenCount > 1;

            // 构建 comboValues
            List<AssetConfigItemVO.ComboValue> comboValues = new ArrayList<>();
            for (ComboValueEntry e : entries) {
                comboValues.add(AssetConfigItemVO.ComboValue.builder()
                        .workspaceId(e.workspaceId)
                        .characterId(e.characterId)
                        .toolId(e.toolId)
                        .skillId(e.skillId)
                        .memoryId(e.memoryId)
                        .value(e.value)
                        .sourceLevel(e.level.name())
                        .sourceType(e.sourceType)
                        .effective(false)
                        .build());
            }

            // 构建候选值列表
            List<AssetConfigItemVO.CandidateValue> candidates = new ArrayList<>();
            int priority = 0;
            for (ComboValueEntry e : entries) {
                String assetCombo = buildAssetCombo(e.workspaceId, e.characterId, e.toolId, e.skillId, e.memoryId);
                candidates.add(AssetConfigItemVO.CandidateValue.builder()
                        .value(e.value)
                        .sourceLevel(e.level.name())
                        .sourceType(e.sourceType)
                        .assetCombo(assetCombo)
                        .priority(priority++)
                        .build());
            }

            // 确定生效值和来源信息
            Object effectiveValue = null;
            String sourceLevel = null;
            String sourceType = null;
            Integer scopeBit = null;
            if (ambiguous) {
                // 模糊情况：effectiveValue 为 null，候选值在 candidates 中
            } else {
                ComboValueEntry effectiveEntry = entries.get(0);
                effectiveValue = effectiveEntry.value;
                sourceLevel = effectiveEntry.level.name();
                sourceType = effectiveEntry.sourceType;
                scopeBit = effectiveEntry.scopeBit;
                // 标记有效的 comboValue
                for (AssetConfigItemVO.ComboValue cv : comboValues) {
                    if (cv.getSourceLevel().equals(effectiveEntry.level.name())
                            && Objects.equals(cv.getWorkspaceId(), effectiveEntry.workspaceId)
                            && Objects.equals(cv.getCharacterId(), effectiveEntry.characterId)) {
                        cv.setEffective(true);
                        break;
                    }
                }
            }

            // 获取 GLOBAL 级别的元数据（name, description）
            ConfigMetadata metadata = configStore.listMetadata().stream()
                    .filter(m -> m.configKey().equals(configKey))
                    .findFirst()
                    .orElse(null);

            configs.add(AssetConfigItemVO.builder()
                    .configKey(configKey)
                    .name(metadata != null ? metadata.name() : null)
                    .description(metadata != null ? metadata.description() : null)
                    .effectiveValue(effectiveValue)
                    .sourceLevel(sourceLevel)
                    .sourceType(sourceType)
                    .scopeBit(scopeBit)
                    .ambiguous(ambiguous)
                    .effectiveValueCandidates(ambiguous ? candidates : null)
                    .comboValues(comboValues)
                    .build());
        }

        return AssetConfigVO.builder()
                .assetType(assetType)
                .assetId(assetId)
                .scopeLevel(chain.get(0).name())
                .configs(configs)
                .build();
    }

    /**
     * 获取配置继承链路
     */
    public ConfigTopologyVO getConfigChain(String configKey, String assetType, String assetId, String parentLevelName, String workspaceId) {
        AssetType type = AssetType.valueOf(assetType.toUpperCase());
        Level parentLevel = parentLevelName != null ? Level.valueOf(parentLevelName.toUpperCase()) : null;

        // 构建继承链
        List<Level> chain = InheritanceConfig.buildChain(type, parentLevel);

        // 构建链路节点
        List<ConfigChainNodeVO> nodes = new ArrayList<>();
        Level effectiveLevel = null;

        for (Level level : chain) {
            Object value = getConfigValueAtLevel(level, parentLevel, configKey, assetId, type);
            boolean hasConfig = value != null;
            if (hasConfig && effectiveLevel == null) {
                effectiveLevel = level;
            }
            nodes.add(ConfigChainNodeVO.builder()
                    .level(level.name())
                    .hasConfig(hasConfig)
                    .configValue(value)
                    .isEffective(level == effectiveLevel)
                    .build());
        }

        return ConfigTopologyVO.builder()
                .configKey(configKey)
                .chain(nodes)
                .build();
    }

    /**
     * 获取配置继承链路（兼容旧接口）
     */
    public ConfigTopologyVO getConfigChain(String configKey, String assetType, String assetId, String parentLevelName) {
        return getConfigChain(configKey, assetType, assetId, parentLevelName, null);
    }

    /**
     * 获取资产元数据列表
     */
    public List<ConfigMetadata> listMetadata() {
        return configStore.listMetadata();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取指定层级对应的所有 ConfigLevel
     *
     * <p>注意：对于资产自身层级，会同时查询带工作空间和不带工作空间的配置，
     * 以便收集该资产在所有资产组合下的配置
     */
    private List<ConfigLevel> getConfigLevelsForLevel(Level level, AssetType assetType, Level parentLevel) {
        List<ConfigLevel> levels = new ArrayList<>();

        boolean hasWorkspace = parentLevel == Level.WORKSPACE;

        switch (level) {
            case GLOBAL -> levels.add(ConfigLevel.GLOBAL);
            case USER -> levels.add(ConfigLevel.STUDIO);
            case WORKSPACE -> {
                levels.add(hasWorkspace ? ConfigLevel.GLOBAL_WORKSPACE : ConfigLevel.STUDIO_WORKSPACE);
            }
            case CHARACTER -> {
                // 始终添加 GLOBAL_WS_CHAR 以获取该角色在所有工作空间下的配置
                levels.add(ConfigLevel.GLOBAL_WS_CHAR);
                levels.add(ConfigLevel.GLOBAL_CHARACTER);
            }
            case SKILL -> {
                // 始终添加 GLOBAL_WS_SKILL 以获取该技能在所有工作空间下的配置
                levels.add(ConfigLevel.GLOBAL_WS_SKILL);
                levels.add(ConfigLevel.GLOBAL_SKILL);
            }
            case TOOL -> {
                // 始终添加 GLOBAL_WS_TOOL 以获取该工具在所有工作空间下的配置
                levels.add(ConfigLevel.GLOBAL_WS_TOOL);
                levels.add(ConfigLevel.GLOBAL_CHAR_TOOL);
                levels.add(ConfigLevel.GLOBAL_TOOL);
            }
            case MEMORY -> {
                // 始终添加 GLOBAL_WS_MEMORY 以获取该记忆在所有工作空间下的配置
                levels.add(ConfigLevel.GLOBAL_WS_MEMORY);
                levels.add(ConfigLevel.GLOBAL_CHAR_MEMORY);
                levels.add(ConfigLevel.GLOBAL_MEMORY);
            }
        }

        return levels;
    }

    private Object getConfigValueAtLevel(Level level, Level parentLevel, String configKey, String assetId, AssetType assetType) {
        ConfigLevel configLevel = toConfigLevelForAsset(level, assetType, parentLevel);
        String wid = getWorkspaceIdForLevel(level, parentLevel);
        String cid = getCharacterIdForLevel(level, assetId, assetType, parentLevel);
        String tid = getToolIdForLevel(level, assetId, assetType);
        String sid = getSkillIdForLevel(level, assetId, assetType);
        String mid = getMemoryIdForLevel(level, assetId, assetType);
        Optional<Object> value = configStore.get(configLevel, wid, cid, tid, sid, mid, configKey);
        return value.orElse(null);
    }

    private ConfigLevel toConfigLevelForAsset(Level level, AssetType assetType, Level parentLevel) {
        boolean hasWorkspace = parentLevel == Level.WORKSPACE;

        return switch (level) {
            case GLOBAL -> ConfigLevel.GLOBAL;
            case USER -> ConfigLevel.STUDIO;
            case WORKSPACE -> hasWorkspace ? ConfigLevel.GLOBAL_WORKSPACE : ConfigLevel.STUDIO_WORKSPACE;
            case CHARACTER -> {
                if (hasWorkspace) {
                    yield ConfigLevel.GLOBAL_WS_CHAR;
                }
                yield ConfigLevel.GLOBAL_CHARACTER;
            }
            case SKILL -> {
                if (hasWorkspace) {
                    yield ConfigLevel.GLOBAL_WS_SKILL;
                }
                yield ConfigLevel.GLOBAL_SKILL;
            }
            case TOOL -> {
                if (hasWorkspace && parentLevel == Level.CHARACTER) {
                    yield ConfigLevel.GLOBAL_WS_CHAR_TOOL;
                }
                if (hasWorkspace) {
                    yield ConfigLevel.GLOBAL_WS_TOOL;
                }
                if (parentLevel == Level.CHARACTER) {
                    yield ConfigLevel.GLOBAL_CHAR_TOOL;
                }
                yield ConfigLevel.GLOBAL_TOOL;
            }
            case MEMORY -> {
                if (hasWorkspace) {
                    yield ConfigLevel.GLOBAL_WS_MEMORY;
                }
                if (parentLevel == Level.CHARACTER) {
                    yield ConfigLevel.GLOBAL_CHAR_MEMORY;
                }
                yield ConfigLevel.GLOBAL_MEMORY;
            }
        };
    }

    private String getWorkspaceIdForLevel(Level level, Level parentLevel) {
        return switch (level) {
            case GLOBAL, USER -> null;
            case WORKSPACE, CHARACTER, SKILL, TOOL, MEMORY -> {
                if (parentLevel == Level.WORKSPACE) {
                    yield null; // 工作空间ID由数据库记录中的workspaceId提供
                }
                yield null;
            }
        };
    }

    private String getCharacterIdForLevel(Level level, String assetId, AssetType assetType, Level parentLevel) {
        return switch (level) {
            case GLOBAL, USER, WORKSPACE, SKILL, TOOL, MEMORY -> null;
            case CHARACTER -> assetType == AssetType.CHARACTER ? assetId : null;
        };
    }

    private String getToolIdForLevel(Level level, String assetId, AssetType assetType) {
        return switch (level) {
            case GLOBAL, USER, WORKSPACE, CHARACTER, SKILL, MEMORY -> null;
            case TOOL -> assetType == AssetType.TOOL ? assetId : null;
        };
    }

    private String getSkillIdForLevel(Level level, String assetId, AssetType assetType) {
        return switch (level) {
            case GLOBAL, USER, WORKSPACE, CHARACTER, TOOL, MEMORY -> null;
            case SKILL -> assetType == AssetType.SKILL ? assetId : null;
        };
    }

    private String getMemoryIdForLevel(Level level, String assetId, AssetType assetType) {
        return switch (level) {
            case GLOBAL, USER, WORKSPACE, CHARACTER, SKILL, TOOL -> null;
            case MEMORY -> assetType == AssetType.MEMORY ? assetId : null;
        };
    }

    /**
     * 构建 ComboValueEntry，检查配置项是否与当前资产相关
     */
    private ComboValueEntry buildComboValueEntry(ConfigStoreItem item, Level level, AssetType assetType, String assetId, Level parentLevel) {
        // 父级层级不限制具体资产ID
        if (!isParentLevel(level)) {
            // 资产自身层级需要严格匹配资产ID
            if (!matchesAssetId(item, level, assetType, assetId)) {
                return null;
            }
        }

        String sourceType = isParentLevel(level) ? "INHERITED" : "OVERRIDDEN";

        // 获取该配置项在当前层级和资产上下文下的值
        Object value = getConfigValueAtLevel(level, parentLevel, item.configKey(), assetId, assetType);
        if (value == null) {
            return null;
        }

        String wid = item.workspaceId();
        String cid = item.characterId();
        String tid = item.toolId();
        String sid = item.skillId();
        String mid = item.memoryId();
        Integer scopeBit = item.level().getScopeBit();

        return new ComboValueEntry(item.configKey(), value, level, sourceType, wid, cid, tid, sid, mid, scopeBit);
    }

    /**
     * 检查配置项的资产ID是否匹配目标资产
     */
    private boolean matchesAssetId(ConfigStoreItem item, Level level, AssetType assetType, String assetId) {
        return switch (assetType) {
            // CHARACTER 只匹配 characterId，不限制 workspaceId（收集所有工作空间下的配置）
            case CHARACTER -> assetId.equals(item.characterId());
            case WORKSPACE -> assetId.equals(item.workspaceId());
            case TOOL -> assetId.equals(item.toolId());
            case SKILL -> assetId.equals(item.skillId());
            case MEMORY -> assetId.equals(item.memoryId());
        };
    }

    private boolean isParentLevel(Level level) {
        return level == Level.GLOBAL || level == Level.USER || level == Level.WORKSPACE;
    }

    /**
     * 构建资产组合描述字符串
     */
    private String buildAssetCombo(String workspaceId, String characterId, String toolId, String skillId, String memoryId) {
        StringBuilder sb = new StringBuilder();
        if (workspaceId != null && !workspaceId.isEmpty()) {
            sb.append(workspaceId);
        }
        if (characterId != null && !characterId.isEmpty()) {
            if (sb.length() > 0) sb.append("/");
            sb.append(characterId);
        }
        if (toolId != null && !toolId.isEmpty()) {
            if (sb.length() > 0) sb.append("/");
            sb.append(toolId);
        }
        if (skillId != null && !skillId.isEmpty()) {
            if (sb.length() > 0) sb.append("/");
            sb.append(skillId);
        }
        if (memoryId != null && !memoryId.isEmpty()) {
            if (sb.length() > 0) sb.append("/");
            sb.append(memoryId);
        }
        return sb.length() > 0 ? sb.toString() : "global";
    }

    /**
     * 内部类： ComboValueEntry
     */
    private static class ComboValueEntry {
        final String configKey;
        final Object value;
        final Level level;
        final String sourceType;
        final String workspaceId;
        final String characterId;
        final String toolId;
        final String skillId;
        final String memoryId;
        final Integer scopeBit;

        ComboValueEntry(String configKey, Object value, Level level, String sourceType,
                        String workspaceId, String characterId, String toolId, String skillId, String memoryId, Integer scopeBit) {
            this.configKey = configKey;
            this.value = value;
            this.level = level;
            this.sourceType = sourceType;
            this.workspaceId = workspaceId;
            this.characterId = characterId;
            this.toolId = toolId;
            this.skillId = skillId;
            this.memoryId = memoryId;
            this.scopeBit = scopeBit;
        }
    }
}