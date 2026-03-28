package org.dragon.store.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Store 模块自动配置类
 */
@AutoConfiguration
@EnableConfigurationProperties(StoreProperties.class)
@ConditionalOnProperty(prefix = "dragon.store", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StoreAutoConfiguration {
    // StoreFactory 和 StoreRegistry 通过 @Component 注解由 Spring 自动创建
    // 此配置类仅用于启用配置属性绑定和条件开关
}
