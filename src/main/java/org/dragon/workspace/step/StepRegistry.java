package org.dragon.workspace.step;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Step 注册表
 *
 * <p>按名称注册 Step，支持工厂方法动态创建。
 *
 * @author yijunw
 */
@Component
public class StepRegistry {

    private final Map<String, StepFactory> factories = new ConcurrentHashMap<>();

    /**
     * 注册 Step 工厂
     */
    public void register(String name, StepFactory factory) {
        factories.put(name, factory);
    }

    /**
     * 注册 Step 实例（便捷方法，直接注册单例）
     */
    public void register(String name, Step step) {
        factories.put(name, ctx -> step);
    }

    /**
     * 根据名称和参数创建 Step 实例
     */
    public Step create(String name, Map<String, Object> params) {
        StepFactory factory = factories.get(name);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown step: " + name);
        }
        return factory.create(params);
    }

    /**
     * 根据名称获取已注册的 Step
     */
    public Step get(String name) {
        StepFactory factory = factories.get(name);
        if (factory == null) {
            return null;
        }
        return factory.create(java.util.Map.of());
    }

    /**
     * 获取所有已注册的 Step 名称
     */
    public List<String> getRegisteredNames() {
        return List.copyOf(factories.keySet());
    }

    /**
     * Step 工厂接口
     */
    @FunctionalInterface
    public interface StepFactory {
        /**
         * 根据参数创建 Step 实例
         * @param params 创建参数
         * @return Step 实例
         */
        Step create(Map<String, Object> params);
    }
}
