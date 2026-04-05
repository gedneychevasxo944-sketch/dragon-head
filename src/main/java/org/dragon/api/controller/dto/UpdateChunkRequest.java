package org.dragon.api.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 更新记忆片段请求 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChunkRequest {
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
     * 源文件位置信息
     */
    private SourceLocationDTO sourceLocation;
}
