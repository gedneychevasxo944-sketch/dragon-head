package org.dragon.config.dto;

import lombok.Builder;
import lombok.Data;
import org.dragon.config.enums.ConfigLevel;

import java.util.List;

/**
 * 影响分析结果
 */
@Data
@Builder
public class ImpactAnalysis {
    private ConfigLevel sourceLevel;
    private String workspaceId;
    private String characterId;
    private String toolId;
    private String skillId;
    private String memoryId;
    private List<ImpactItem> impacts;
    private int affectedCount;
}