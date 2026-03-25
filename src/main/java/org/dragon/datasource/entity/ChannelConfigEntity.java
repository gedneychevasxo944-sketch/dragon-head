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
import java.util.Map;

import org.dragon.channel.entity.ChannelConfig;
import java.util.Map;

/**
 * ChannelConfigEntity 渠道配置实体
 * 映射数据库 channel_config 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "channel_config")
public class ChannelConfigEntity {

    @Id
    private String id;

    private String channelType;

    private String name;

    private String description;

    private boolean enabled;

    @DbJson
    private Map<String, Object> credentials;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 转换为ChannelConfig
     */
    public ChannelConfig toChannelConfig() {
        return ChannelConfig.builder()
                .id(this.id)
                .channelType(this.channelType)
                .name(this.name)
                .description(this.description)
                .enabled(this.enabled)
                .credentials(this.credentials)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    /**
     * 从ChannelConfig创建Entity
     */
    public static ChannelConfigEntity fromChannelConfig(ChannelConfig config) {
        return ChannelConfigEntity.builder()
                .id(config.getId())
                .channelType(config.getChannelType())
                .name(config.getName())
                .description(config.getDescription())
                .enabled(config.isEnabled())
                .credentials(config.getCredentials())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
