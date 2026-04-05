package org.dragon.api.controller.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 记忆片段 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryChunkDTO {
    /**
     * 片段唯一标识符
     */
    private String id;

    /**
     * 所属文件 ID
     */
    private String fileId;

    /**
     * 片段标题
     */
    private String title;

    /**
     * 片段内容
     */
    private String content;

    /**
     * 内容摘要
     */
    private String summary;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 索引状态：indexed/pending/failed
     */
    private String indexedStatus;

    /**
     * 关联片段 ID 列表
     */
    private List<String> relations;

    /**
     * 源文件位置信息
     */
    private SourceLocationDTO sourceLocation;

    /**
     * 融合来源信息（仅融合片段有）
     */
    private List<FusedFromDTO> fusedFrom;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;
}
