package org.dragon.api.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 融合来源 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FusedFromDTO {
    /**
     * 原始片段 ID
     */
    private String chunkId;

    /**
     * 原始片段标题
     */
    private String chunkTitle;

    /**
     * 来源名称
     */
    private String sourceName;

    /**
     * 融合时间
     */
    private Instant fusedAt;
}
