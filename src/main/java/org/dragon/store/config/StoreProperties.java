package org.dragon.store.config;

import org.dragon.store.StoreType;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Store 配置属性
 */
@ConfigurationProperties(prefix = "dragon.store")
public class StoreProperties {

    private boolean enabled = true;
    private StoreType type = StoreType.MYSQL;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public StoreType getType() {
        return type;
    }

    public void setType(StoreType type) {
        this.type = type;
    }
}
