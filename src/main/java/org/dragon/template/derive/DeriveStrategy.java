package org.dragon.template.derive;

import org.dragon.permission.enums.ResourceType;

/**
 * DeriveStrategy 模板派生策略接口
 *
 * <p>每种资产类型有独立的派生策略，负责：
 * <ul>
 *   <li>从模板资产派生创建新资产</li>
 *   <li>白板创建模板时创建草稿资产</li>
 * </ul>
 *
 * @author yijunw
 */
public interface DeriveStrategy {

    /**
     * 获取该策略支持的资源类型
     *
     * @return 资源类型
     */
    ResourceType getSupportedType();

    /**
     * 从模板资产派生创建新资产
     *
     * @param templateAsset 模板资产对象
     * @param request       派生请求
     * @return 创建的新资产
     */
    Object derive(Object templateAsset, DeriveContext context);

    /**
     * 创建草稿资产（白板创建模板时调用）
     *
     * @param request 创建模板请求
     * @return 创建的草稿资产
     */
    Object createDraft(CreateContext request);
}
