package org.dragon.config.service;

import lombok.extern.slf4j.Slf4j;
import org.dragon.config.dto.ImpactAnalysis;
import org.dragon.config.dto.ImpactItem;
import org.dragon.config.enums.ConfigLevel;
import org.dragon.config.enums.ImpactType;
import org.dragon.config.enums.ScopeBits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置影响面分析服务
 *
 * <p>分析逻辑：如果一个配置存在于粗粒度 X，且 ID 为 Y，那么所有继承自 X 且同样使用 ID Y 的细粒度配置都会被影响。
 */
@Slf4j
@Service
public class ConfigImpactAnalyzer {

    /**
     * 影响面分析
     *
     * <p>示例：如果 GLOBAL_WORKSPACE (ws-001) 存在，则影响：
     * <ul>
     *   <li>GLOBAL_WORKSPACE (ws-001) - 自身</li>
     *   <li>GLOBAL_WS_CHAR (ws-001, char-*) - 子链路</li>
     *   <li>GLOBAL_WS_SKILL (ws-001, skill-*) - 子链路</li>
     *   <li>GLOBAL_WS_TOOL (ws-001, tool-*) - 子链路</li>
     *   <li>GLOBAL_WS_CHAR_TOOL (ws-001, char-*, tool-*) - 子链路</li>
     * </ul>
     */
    public ImpactAnalysis analyzeImpact(ConfigLevel level, String workspaceId, String characterId,
                                       String toolId, String skillId, String memoryId) {
        List<ImpactItem> impacts = new ArrayList<>();

        // 找出所有是当前粒度祖先的粒度（粗粒度）
        for (ConfigLevel candidate : ConfigLevel.values()) {
            if (candidate == level) {
                continue;
            }
            // candidate 是 level 的祖先当且仅当 level.isDescendantOf(candidate) = true
            if (level.isDescendantOf(candidate)) {
                // 检查 ID 是否匹配
                if (idsMatch(candidate, workspaceId, characterId, toolId, skillId, memoryId)) {
                    impacts.add(ImpactItem.builder()
                            .level(candidate)
                            .impactType(ImpactType.AFFECTS)
                            .description("配置变更将影响 " + candidate.getDescription() + " 级别的配置")
                            .build());
                }
            }
        }

        return ImpactAnalysis.builder()
                .sourceLevel(level)
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
     * 检查 ID 是否匹配
     *
     * <p>规则：
     * <ul>
     *   <li>如果粗粒度有 WORKSPACE bit，workspaceId 必须匹配</li>
     *   <li>如果粗粒度有 CHARACTER bit，characterId 必须匹配</li>
     *   <li>如果粗粒度有 TOOL bit，toolId 必须匹配</li>
     *   <li>如果粗粒度有 SKILL bit，skillId 必须匹配</li>
     *   <li>如果粗粒度有 MEMORY bit，memoryId 必须匹配</li>
     * </ul>
     */
    private boolean idsMatch(ConfigLevel coarseLevel,
                            String workspaceId, String characterId, String toolId,
                            String skillId, String memoryId) {
        // WORKSPACE 检查
        if ((coarseLevel.getScopeBit() & ScopeBits.WORKSPACE) != 0) {
            if (workspaceId == null) {
                return false;
            }
        }

        // CHARACTER 检查
        if ((coarseLevel.getScopeBit() & ScopeBits.CHARACTER) != 0) {
            if (characterId == null) {
                return false;
            }
        }

        // TOOL 检查
        if ((coarseLevel.getScopeBit() & ScopeBits.TOOL) != 0) {
            if (toolId == null) {
                return false;
            }
        }

        // SKILL 检查
        if ((coarseLevel.getScopeBit() & ScopeBits.SKILL) != 0) {
            if (skillId == null) {
                return false;
            }
        }

        // MEMORY 检查
        if ((coarseLevel.getScopeBit() & ScopeBits.MEMORY) != 0) {
            if (memoryId == null) {
                return false;
            }
        }

        return true;
    }
}