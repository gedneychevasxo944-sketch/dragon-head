package org.dragon.observer.optimization.plan;

import lombok.Builder;
import lombok.Data;

/**
 * SuggestionRequest 建议请求
 * 用于向 ObserverAdvisorCharacter 请求优化建议
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
public class SuggestionRequest {

    /**
     * 评价记录 ID
     */
    private String evaluationId;

    /**
     * 用户透传的 prompt（可选）
     * 如果为空，使用 PromptManager 中的默认配置
     */
    private String userPrompt;

    /**
     * 目标类型
     */
    private OptimizationAction.TargetType targetType;

    /**
     * 目标 ID
     */
    private String targetId;

    /**
     * 收集最近多少天的数据
     */
    @Builder.Default
    private Integer recentTasksDays = 7;

    /**
     * 最大建议数量
     */
    @Builder.Default
    private Integer maxSuggestions = 5;
}