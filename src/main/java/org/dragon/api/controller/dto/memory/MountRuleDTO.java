package org.dragon.api.controller.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 挂载规则 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MountRuleDTO {
    /**
     * 规则唯一标识符
     */
    private String id;

    /**
     * 规则类型：tag/content/time
     */
    private String type;

    /**
     * 操作符：include/exclude
     */
    private String operator;

    /**
     * 规则值（标签名、关键词或时间表达式）
     */
    private String value;
}
