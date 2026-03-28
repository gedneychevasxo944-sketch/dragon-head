package org.dragon.store.config;

import org.dragon.store.StoreType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * StoreProfileInitializer 根据 dragon.store.type 自动设置 Spring Profile
 *
 * <p>这样只需在 Store 实现类上添加 @Profile 注解，即可在不同存储类型之间切换：
 * <ul>
 *   <li>dragon.store.type=MYSQL   → 激活 profile "mysql"</li>
 *   <li>dragon.store.type=MEMORY   → 激活 profile "memory"</li>
 *   <li>dragon.store.type=FILE     → 激活 profile "file"</li>
 * </ul>
 *
 * <p>在 docker-compose.yml 中设置 DRAGON_STORE_TYPE=MYSQL 即可启用 MySQL 存储。
 */
public class StoreProfileInitializer implements EnvironmentPostProcessor {

    private static final String PROPERTY_NAME = "dragon.store.type";
    private static final String PROFILE_MYSQL = "mysql";
    private static final String PROFILE_MEMORY = "memory";
    private static final String PROFILE_FILE = "file";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String storeTypeStr = environment.getProperty(PROPERTY_NAME);

        if (storeTypeStr == null || storeTypeStr.isBlank()) {
            return;
        }

        StoreType storeType;
        try {
            storeType = StoreType.valueOf(storeTypeStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return;
        }

        String profile = switch (storeType) {
            case MYSQL -> PROFILE_MYSQL;
            case MEMORY -> PROFILE_MEMORY;
            case FILE -> PROFILE_FILE;
        };

        // 只有明确指定时才添加 profile（避免覆盖用户显式设置的 profiles）
        environment.addActiveProfile(profile);
    }
}
