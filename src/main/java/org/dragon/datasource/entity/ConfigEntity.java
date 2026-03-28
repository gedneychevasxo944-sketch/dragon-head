package org.dragon.datasource.entity;

import io.ebean.annotation.DbJson;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import org.dragon.config.store.ConfigKey;

/**
 * ConfigEntity 配置存储实体
 * 映射 config_store 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "config_store")
public class ConfigEntity {

    @Id
    private String id;

    private String workspace;

    private String entityType;

    private String entityId;

    private String configKey;

    @DbJson
    private Object configValue;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 转换为 ConfigKey
     */
    public ConfigKey toConfigKey() {
        if (workspace != null && entityType != null && entityId != null) {
            return ConfigKey.of(workspace, entityType, entityId, configKey);
        } else if (entityId != null) {
            return ConfigKey.of(entityId, configKey);
        } else {
            return ConfigKey.of(configKey);
        }
    }

    /**
     * 从 ConfigKey 创建 Entity
     */
    public static ConfigEntity fromConfigKey(ConfigKey configKey, Object value) {
        return ConfigEntity.builder()
                .workspace(configKey.getWorkspace())
                .entityType(configKey.getEntityType())
                .entityId(configKey.getEntityId())
                .configKey(configKey.getKey())
                .configValue(value)
                .build();
    }
}
