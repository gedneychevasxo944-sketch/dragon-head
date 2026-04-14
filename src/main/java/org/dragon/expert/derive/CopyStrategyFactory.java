package org.dragon.expert.derive;

import org.dragon.permission.enums.ResourceType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * CopyStrategyFactory 复制策略工厂
 *
 * @author yijunw
 */
@Component
public class CopyStrategyFactory {

    private final Map<ResourceType, CopyStrategy> strategies = new EnumMap<>(ResourceType.class);

    public CopyStrategyFactory(List<CopyStrategy> strategyList) {
        for (CopyStrategy strategy : strategyList) {
            strategies.put(strategy.getSupportedType(), strategy);
        }
    }

    /**
     * 注册策略
     *
     * @param strategy 复制策略
     */
    public void register(CopyStrategy strategy) {
        strategies.put(strategy.getSupportedType(), strategy);
    }

    /**
     * 获取指定资源类型的复制策略
     *
     * @param resourceType 资源类型
     * @return 复制策略
     * @throws UnsupportedOperationException 如果不支持该类型
     */
    public CopyStrategy getStrategy(ResourceType resourceType) {
        CopyStrategy strategy = strategies.get(resourceType);
        if (strategy == null) {
            throw new UnsupportedOperationException("No CopyStrategy for resource type: " + resourceType);
        }
        return strategy;
    }
}