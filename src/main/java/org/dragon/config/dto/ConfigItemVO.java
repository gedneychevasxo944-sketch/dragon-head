package org.dragon.config.dto;

import lombok.Builder;
import lombok.Data;
import org.dragon.config.enums.ConfigLevel;

/**
 * 配置项 VO
 */
@Data
@Builder
public class ConfigItemVO {
    private String configKey;
    private ConfigLevel level;
    private Object effectiveValue;
    private Object storeValue;
    private String displayStatus;
    private String source;
    private String valueType;
}