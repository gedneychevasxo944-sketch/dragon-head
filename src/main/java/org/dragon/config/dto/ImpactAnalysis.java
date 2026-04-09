package org.dragon.config.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 影响分析结果
 */
@Data
@Builder
public class ImpactAnalysis {
    private String sourceLevel;
    private String workspaceId;
    private String characterId;
    private String toolId;
    private String skillId;
    private String memoryId;
    private List<ImpactItem> impacts;
    private int affectedCount;
}