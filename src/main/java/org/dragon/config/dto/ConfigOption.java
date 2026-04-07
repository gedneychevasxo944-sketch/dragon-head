package org.dragon.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置枚举选项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigOption {
    /**
     * 选项显示标签
     */
    private String label;

    /**
     * 选项值
     */
    private String value;
}
