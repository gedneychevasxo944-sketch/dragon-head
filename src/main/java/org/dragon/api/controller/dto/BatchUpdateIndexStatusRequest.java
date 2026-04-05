package org.dragon.api.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量更新记忆片段索引状态请求 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdateIndexStatusRequest {
    /**
     * 要更新的片段 ID 列表
     */
    private List<String> chunkIds;

    /**
     * 目标索引状态：indexed/pending/failed
     */
    private String indexedStatus;
}
