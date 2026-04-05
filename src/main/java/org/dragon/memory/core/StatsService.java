package org.dragon.memory.core;

import org.dragon.api.controller.dto.MemoryOverviewStatsDTO;
import org.dragon.api.controller.dto.SourceStatsResponseDTO;

/**
 * 统计服务接口
 *
 * @author binarytom
 * @version 1.0
 */
public interface StatsService {
    /**
     * 获取全局统计信息
     *
     * @return 全局统计信息
     */
    MemoryOverviewStatsDTO getOverviewStats();

    /**
     * 获取数据源统计信息
     *
     * @return 数据源统计信息
     */
    SourceStatsResponseDTO getSourceStats();
}
