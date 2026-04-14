package org.dragon.template.derive;

import lombok.Builder;
import lombok.Data;

/**
 * DeriveContext 派生上下文
 *
 * @author yijunw
 */
@Data
@Builder
public class DeriveContext {

    /**
     * 模板资产对象
     */
    private Object templateAsset;

    /**
     * 派生请求
     */
    private DeriveTemplateRequest request;

    /**
     * 操作人 ID
     */
    private Long operatorId;
}
