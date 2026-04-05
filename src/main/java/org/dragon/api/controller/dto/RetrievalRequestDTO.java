package org.dragon.api.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 检索请求 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalRequestDTO {
    /**
     * 查询内容
     */
    private String query;

    /**
     * 检索范围：all/character/workspace
     */
    private String scope;

    /**
     * 目标 ID（scope 为 character 或 workspace 时必填）
     */
    private String targetId;

    /**
     * 最大结果数
     */
    private int maxResults;

    /**
     * 最小分数
     */
    private double minScore;

    /**
     * 是否包含会话记录
     */
    private boolean includeSessionRecords;

    /**
     * 来源类型过滤
     */
    private List<String> sourceTypes;

    /**
     * 标签过滤
     */
    private List<String> tags;
}
