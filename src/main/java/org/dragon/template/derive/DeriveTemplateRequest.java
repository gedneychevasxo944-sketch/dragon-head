package org.dragon.template.derive;

import lombok.Data;

/**
 * DeriveTemplateRequest 派生模板请求
 *
 * @author yijunw
 */
@Data
public class DeriveTemplateRequest {

    /**
     * 资产名称（可选，不填则使用模板的名称 + _copy）
     */
    private String name;

    /**
     * 资产描述（可选）
     */
    private String description;
}
