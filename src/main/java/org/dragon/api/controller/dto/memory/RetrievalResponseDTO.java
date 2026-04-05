package org.dragon.api.controller.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 检索响应 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalResponseDTO {
    /**
     * 检索结果列表
     */
    private List<RetrievalResultDTO> results;

    /**
     * 总数量
     */
    private long total;

    /**
     * 检索延迟（毫秒）
     */
    private long latency;
}
