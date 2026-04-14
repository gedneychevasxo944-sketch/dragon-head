package org.dragon.expert.derive;

import lombok.Builder;
import lombok.Data;

/**
 * CopyContext 复制上下文
 *
 * @author yijunw
 */
@Data
@Builder
public class CopyContext {

    /**
     * 源资产对象
     */
    private Object sourceAsset;

    /**
     * 操作人 ID
     */
    private Long operatorId;

    /**
     * 是否创建 Expert 标记（创建 Expert 时为 true，从 Expert 派生时为 false）
     */
    private boolean markAsExpert;

    /**
     * 专家分类（markAsExpert 为 true 时使用）
     */
    private String category;

    /**
     * 预览文本（markAsExpert 为 true 时使用）
     */
    private String preview;

    /**
     * 目标用户群体（markAsExpert 为 true 时使用）
     */
    private String targetAudience;
}