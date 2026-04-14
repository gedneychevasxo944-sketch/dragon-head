package org.dragon.expert.derive;

import org.dragon.permission.enums.ResourceType;

/**
 * CopyStrategy 资产复制策略接口
 *
 * <p>每种资产类型有独立的复制策略，负责全量复制资产及其关联。
 *
 * @author yijunw
 */
public interface CopyStrategy {

    /**
     * 获取该策略支持的资源类型
     *
     * @return 资源类型
     */
    ResourceType getSupportedType();

    /**
     * 全量复制资产（包括关联的资产）
     *
     * @param sourceAsset 源资产对象
     * @param context     复制上下文
     * @return 副本资产
     */
    Object copy(Object sourceAsset, CopyContext context);
}