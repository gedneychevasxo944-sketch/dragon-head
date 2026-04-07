package org.dragon.config.dto;

import lombok.Builder;
import lombok.Data;
import org.dragon.config.enums.ConfigLevel;
import org.dragon.config.enums.ImpactType;

/**
 * 影响项
 */
@Data
@Builder
public class ImpactItem {
    private ConfigLevel level;
    private ImpactType impactType;
    private String description;
}