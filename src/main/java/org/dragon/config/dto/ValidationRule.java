package org.dragon.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置校验规则
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRule {
    /**
     * 校验类型：required, min, max, pattern, custom
     */
    private String type;

    /**
     * 校验值
     */
    private String value;

    /**
     * 错误提示信息
     */
    private String message;
}
