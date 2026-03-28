package org.dragon.store;

import org.dragon.store.config.StoreProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Store 工厂类
 * 根据 StoreType 获取对应实现
 */
@Component
public class StoreFactory {

    private final ApplicationContext context;
    private final StoreProperties storeProperties;
    private final Map<Class<? extends Store>, Map<StoreType, Store>> registry = new java.util.HashMap<>();

    public StoreFactory(ApplicationContext context, StoreProperties storeProperties) {
        this.context = context;
        this.storeProperties = storeProperties;
    }

    /**
     * 获取默认类型的实现
     */
    public <T extends Store> T get(Class<T> storeInterface) {
        return get(storeInterface, storeProperties.getType());
    }

    /**
     * 获取指定类型的实现
     */
    @SuppressWarnings("unchecked")
    public <T extends Store> T get(Class<T> storeInterface, StoreType type) {
        // 优先从本地注册表查找（StoreRegistry @PostConstruct 后已有值）
        Map<StoreType, Store> typeMap = registry.get(storeInterface);
        if (typeMap != null) {
            Store store = typeMap.get(type);
            if (store != null) {
                return (T) store;
            }
        }

        // 如果注册表没有，通过 ApplicationContext 查找对应类型的 Bean
        // 这样可以在 @PostConstruct 之前提供服务
        Map<String, ?> beans = context.getBeansWithAnnotation(StoreTypeAnn.class);
        for (Object bean : beans.values()) {
            StoreTypeAnn annotation = bean.getClass().getAnnotation(StoreTypeAnn.class);
            if (annotation.value() == type) {
                // 检查是否实现了目标接口
                for (Class<?> iface : bean.getClass().getInterfaces()) {
                    if (storeInterface.isAssignableFrom(iface)) {
                        return (T) bean;
                    }
                }
            }
        }

        throw new IllegalStateException("No " + type + " implementation for: " + storeInterface.getName());
    }

    /**
     * 注册实现（内部使用）
     */
    public <T extends Store> void register(Class<T> storeInterface, StoreType type, T store) {
        registry.computeIfAbsent(storeInterface, k -> new EnumMap<>(StoreType.class)).put(type, store);
    }
}
