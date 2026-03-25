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

import org.dragon.channel.entity.ChannelBinding;
import java.util.Map;

/**
 * ChannelBindingEntity 渠道绑定实体
 * 映射数据库 channel_binding 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "channel_binding")
public class ChannelBindingEntity {

    @Id
    private String id;

    private String workspaceId;

    private String channelName;

    private String chatId;

    private String chatType;

    private String description;

    private boolean enabled;

    @DbJson
    private Map<String, Object> metadata;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 转换为ChannelBinding
     */
    public ChannelBinding toChannelBinding() {
        return ChannelBinding.builder()
                .id(this.id)
                .workspaceId(this.workspaceId)
                .channelName(this.channelName)
                .chatId(this.chatId)
                .chatType(this.chatType)
                .description(this.description)
                .enabled(this.enabled)
                .metadata(this.metadata)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    /**
     * 从ChannelBinding创建Entity
     */
    public static ChannelBindingEntity fromChannelBinding(ChannelBinding binding) {
        return ChannelBindingEntity.builder()
                .id(binding.getId())
                .workspaceId(binding.getWorkspaceId())
                .channelName(binding.getChannelName())
                .chatId(binding.getChatId())
                .chatType(binding.getChatType())
                .description(binding.getDescription())
                .enabled(binding.isEnabled())
                .metadata(binding.getMetadata())
                .createdAt(binding.getCreatedAt())
                .updatedAt(binding.getUpdatedAt())
                .build();
    }
}
