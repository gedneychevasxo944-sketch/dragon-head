package org.dragon.observer.evaluation;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ObservationFinding 观测发现
 * 记录评价引擎发现的单个问题或优化点
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationFinding {

    /**
     * 优化维度
     */
    private String dimension;

    /**
     * 严重程度 (HIGH, MEDIUM, LOW)
     */
    private String severity;

    /**
     * 发现摘要
     */
    private String summary;

    /**
     * 详细描述
     */
    private String details;

    /**
     * 目标类型
     */
    private String targetType;

    /**
     * 目标 ID
     */
    private String targetId;

    /**
     * 建议的动作类型
     */
    private String suggestedActionType;

    /**
     * 置信度 (0-1)
     */
    private Double confidence;

    /**
     * 是否不安全
     */
    private boolean unsafe;

    /**
     * 证据
     */
    private Map<String, Object> evidence;
}
