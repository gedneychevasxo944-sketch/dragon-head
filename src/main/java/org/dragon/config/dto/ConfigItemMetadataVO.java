package org.dragon.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 配置项元数据 VO
 *
 * <p>用于 list 接口返回配置项的元数据信息（名称、描述、默认值、校验规则等）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigItemMetadataVO {

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置项名称
     */
    private String name;

    /**
     * 配置项描述
     */
    private String description;

    /**
     * 数据类型：STRING, NUMBER, BOOLEAN, ENUM, LIST, OBJECT, JSON
     */
    private String dataType;

    /**
     * 默认值
     */
    private Object defaultValue;

    /**
     * 配置类别（如 MODEL, CHANNEL, SECURITY 等）
     */
    private String category;

    /**
     * 校验规则列表
     */
    private List<ValidationRule> validationRules;

    /**
     * 枚举选项列表
     */
    private List<ConfigOption> options;
}