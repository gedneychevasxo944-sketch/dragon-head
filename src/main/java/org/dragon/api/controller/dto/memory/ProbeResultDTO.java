package org.dragon.api.controller.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统检查结果 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProbeResultDTO {
    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 检查延迟（毫秒）
     */
    private long latency;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 详细信息
     */
    private String details;
}
