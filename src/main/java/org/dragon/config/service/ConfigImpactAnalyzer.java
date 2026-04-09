package org.dragon.config.service;

import lombok.extern.slf4j.Slf4j;
import org.dragon.config.dto.ImpactAnalysis;
import org.dragon.config.dto.ImpactItem;
import org.dragon.config.enums.ConfigLevel;
import org.dragon.config.enums.ImpactType;
import org.dragon.config.model.InheritanceConfig;
import org.dragon.config.model.InheritanceConfig.AssetType;
import org.dragon.config.model.InheritanceConfig.Level;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置影响面分析服务
 *
 * <p>使用显式继承链路配置替代位运算来分析配置变更影响面。
 *
 * <p>分析逻辑：如果一个配置在父层级 X 设置了值，那么所有继承自 X 的子层级配置都会受影响。
 */
@Slf4j
@Service
public class ConfigImpactAnalyzer {

    /**
     * 影响面分析
     *
     * <p>示例：如果在 WORKSPACE 级别设置了配置值，则影响：
     * <ul>
     *   <li>WORKSPACE 自身</li>
     *   <li>所有 CHARACTER（继承自 WORKSPACE）</li>
     *   <li>所有 SKILL（继承自 WORKSPACE）</li>
     *   <li>所有 TOOL（继承自 WORKSPACE 或 CHARACTER）</li>
     *   <li>所有 MEMORY（继承自 WORKSPACE 或 CHARACTER）</li>
     * </ul>
     */
    public ImpactAnalysis analyzeImpact(ConfigLevel level, String workspaceId, String characterId,
                                       String toolId, String skillId, String memoryId) {
        List<ImpactItem> impacts = new ArrayList<>();
        Level targetLevel = InheritanceConfig.toLevel(level);

        // 找出所有会受到影响的子层级
        for (AssetType assetType : AssetType.values()) {
            List<Level> chain = InheritanceConfig.getChain(assetType);
            if (chain == null) continue;

            // 检查链中是否包含 targetLevel 作为父级
            int targetIndex = chain.indexOf(targetLevel);
            if (targetIndex > 0) {
                // targetLevel 在链路中，且不是第一个（不是资产自身）
                // 这个资产类型的所有子配置都会受影响
                Level assetLevel = chain.get(0); // 资产自身的层级
                String affectedAssetId = getAssetId(assetLevel, workspaceId, characterId, toolId, skillId, memoryId);

                impacts.add(ImpactItem.builder()
                        .level(assetLevel.name())
                        .assetId(affectedAssetId)
                        .impactType(ImpactType.AFFECTS)
                        .description(String.format("继承自 %s 的 %s 层级配置", targetLevel.name(), assetLevel.name()))
                        .build());
            }
        }

        return ImpactAnalysis.builder()
                .sourceLevel(level.name())
                .workspaceId(workspaceId)
                .characterId(characterId)
                .toolId(toolId)
                .skillId(skillId)
                .memoryId(memoryId)
                .impacts(impacts)
                .affectedCount(impacts.size())
                .build();
    }

    /**
     * 简化版影响分析（只有 level 和 workspaceId）
     */
    public ImpactAnalysis analyzeImpact(ConfigLevel level, String workspaceId) {
        return analyzeImpact(level, workspaceId, null, null, null, null);
    }

    /**
     * 影响分析（兼容旧 API）
     */
    public ImpactAnalysis analyzeImpact(String configKey, ConfigLevel level,
                                       String workspaceId, String characterId) {
        return analyzeImpact(level, workspaceId, characterId, null, null, null);
    }

    /**
     * 获取资产 ID
     */
    private String getAssetId(Level level, String workspaceId, String characterId,
                             String toolId, String skillId, String memoryId) {
        return switch (level) {
            case WORKSPACE -> workspaceId;
            case CHARACTER -> characterId;
            case SKILL -> skillId;
            case TOOL -> toolId;
            case MEMORY -> memoryId;
            case USER, GLOBAL -> null;
        };
    }
}
