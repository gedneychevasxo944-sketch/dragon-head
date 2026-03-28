package org.dragon.schedule.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 调度模块自动配置类
 */
@AutoConfiguration
@EnableConfigurationProperties(ScheduleProperties.class)
@ConditionalOnProperty(prefix = "dragon.schedule", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ScheduleAutoConfiguration {
}
