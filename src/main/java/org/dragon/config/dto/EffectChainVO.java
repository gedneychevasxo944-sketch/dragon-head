package org.dragon.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置生效链 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffectChainVO {
    /**
     * 作用域类型：GLOBAL, STUDIO, CHARACTER, WORKSPACE 等
     */
    private String scopeType;

    /**
     * 作用域 ID（如 workspaceId）
     */
    private String scopeId;

    /**
     * 作用域名称
     */
    private String scopeName;

    /**
     * 配置值
     */
    private Object value;

    /**
     * 是否为覆盖值
     */
    private Boolean isOverride;

    /**
     * 是否为空
     */
    private Boolean isEmpty;

    /**
     * 展示顺序
     */
    private Integer order;
}
