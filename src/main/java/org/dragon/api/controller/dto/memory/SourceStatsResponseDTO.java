package org.dragon.api.controller.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 数据源统计响应 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceStatsResponseDTO {
    /**
     * 数据源类型分布
     */
    private Map<String, Integer> sourceTypeDistribution;

    /**
     * 状态分布
     */
    private Map<String, Integer> statusDistribution;

    /**
     * 健康状态分布
     */
    private Map<String, Integer> healthDistribution;

    /**
     * 总文件数
     */
    private int totalFiles;

    /**
     * 总片段数
     */
    private int totalChunks;

    /**
     * 平均每文件片段数
     */
    private double averageChunksPerFile;
}
