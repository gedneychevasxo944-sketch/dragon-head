package org.dragon.store;

import jakarta.annotation.PostConstruct;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Store 注册器
 * 启动时扫描并注册所有 Store 实现
 */
@Component
public class StoreRegistry {

    private final ApplicationContext context;
    private final StoreFactory storeFactory;

    public StoreRegistry(ApplicationContext context, StoreFactory storeFactory) {
        this.context = context;
        this.storeFactory = storeFactory;
    }

    @PostConstruct
    public void registerAllStores() {
        Map<String, Object> beans = context.getBeansWithAnnotation(StoreTypeAnn.class);
        for (Object bean : beans.values()) {
            Class<?> beanClass = bean.getClass();
            StoreTypeAnn annotation = beanClass.getAnnotation(StoreTypeAnn.class);
            StoreType type = annotation.value();

            // 找到该 Bean 实现的 Store 接口
            for (Class<?> iface : beanClass.getInterfaces()) {
                if (Store.class.isAssignableFrom(iface)) {
                    registerStore(iface, type, bean);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Store> void registerStore(Class<?> iface, StoreType type, Object bean) {
        storeFactory.register((Class<T>) iface, type, (T) bean);
    }
}
