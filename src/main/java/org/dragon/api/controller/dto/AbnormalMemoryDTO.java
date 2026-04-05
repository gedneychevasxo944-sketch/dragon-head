package org.dragon.api.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 异常记忆 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbnormalMemoryDTO {
    /**
     * 异常记忆 ID
     */
    private String id;

    /**
     * 类型：character/workspace
     */
    private String type;

    /**
     * 名称
     */
    private String name;

    /**
     * 异常原因
     */
    private String reason;
}
