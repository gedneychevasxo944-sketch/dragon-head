package org.dragon.api.controller.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检索结果 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalResultDTO {
    /**
     * 结果唯一标识符
     */
    private String id;

    /**
     * 文件路径
     */
    private String path;

    /**
     * 来源名称
     */
    private String source;

    /**
     * 匹配分数
     */
    private double score;

    /**
     * 起始行号
     */
    private int startLine;

    /**
     * 结束行号
     */
    private int endLine;

    /**
     * 片段内容
     */
    private String snippet;

    /**
     * 引用
     */
    private String citation;

    /**
     * 文件 ID
     */
    private String fileId;

    /**
     * 片段 ID
     */
    private String chunkId;
}
