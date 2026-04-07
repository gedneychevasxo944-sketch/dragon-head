package org.dragon.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 配置项 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigItemVO {
    // ==================== 标识信息 ====================

    /**
     * 配置项 ID
     */
    private String id;

    /**
     * 作用域类型：GLOBAL, STUDIO, CHARACTER, WORKSPACE 等
     */
    private String scopeType;

    /**
     * 作用域 ID（如 workspaceId, characterId）
     */
    private String scopeId;

    // ==================== 键信息 ====================

    /**
     * 配置键
     */
    private String key;

    /**
     * 配置项名称
     */
    private String name;

    /**
     * 配置项描述
     */
    private String description;

    // ==================== 类型信息 ====================

    /**
     * 数据类型：STRING, NUMBER, BOOLEAN, ENUM, LIST, OBJECT, JSON
     */
    private String dataType;

    // ==================== 值信息 ====================

    /**
     * 默认值
     */
    private Object defaultValue;

    /**
     * 当前值
     */
    private Object currentValue;

    /**
     * 生效值
     */
    private Object effectiveValue;

    // ==================== 状态信息 ====================

    /**
     * 来源
     */
    private String source;

    /**
     * 显示状态：SET, NOT_SET, USE_DEFAULT
     */
    private String displayStatus;

    // ==================== 元数据 ====================

    /**
     * 校验规则列表
     */
    private List<ValidationRule> validationRules;

    /**
     * 枚举选项列表
     */
    private List<ConfigOption> options;

    // ==================== 审计信息 ====================

    /**
     * 最后修改时间
     */
    private String lastModified;

    /**
     * 修改人
     */
    private String modifiedBy;
}
