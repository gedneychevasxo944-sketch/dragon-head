package org.dragon.template.derive;

import org.dragon.permission.enums.ResourceType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * DeriveStrategyFactory 派生策略工厂
 *
 * @author yijunw
 */
@Component
public class DeriveStrategyFactory {

    private final Map<ResourceType, DeriveStrategy> strategies;

    public DeriveStrategyFactory(List<DeriveStrategy> deriveStrategies) {
        this.strategies = new EnumMap<>(ResourceType.class);
        for (DeriveStrategy strategy : deriveStrategies) {
            strategies.put(strategy.getSupportedType(), strategy);
        }
    }

    /**
     * 获取指定资源类型的派生策略
     *
     * @param resourceType 资源类型
     * @return 派生策略
     * @throws IllegalArgumentException 如果找不到对应的策略
     */
    public DeriveStrategy getStrategy(ResourceType resourceType) {
        DeriveStrategy strategy = strategies.get(resourceType);
        if (strategy == null) {
            throw new IllegalArgumentException("No derive strategy for type: " + resourceType);
        }
        return strategy;
    }
}
