package org.dragon.channel.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ChannelConfig 渠道配置实体
 * 存储一个 IM Bot 的凭证和接入配置，支持同一渠道类型的多个 Bot 实例
 * 例如：多个飞书机器人，每个机器人负责不同的 Workspace
 *
 * @author zhz
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelConfig {
    /**
     * 配置唯一标识（后台自定义命名，如 "feishu-dev-bot"）
     */
    private String id;
    /**
     * 渠道类型（对应 ChannelAdapter 的类型标识）
     * 例如: "Feishu", "DingTalk", "WeChat"
     */
    private String channelType;
    /**
     * 配置显示名称
     */
    private String name;
    /**
     * 描述
     */
    private String description;
    /**
     * 是否启用
     */
    @Builder.Default
    private Boolean enabled = true;
    /**
     * 渠道凭证配置（各渠道不同）
     * 飞书示例:
     * appId: "cli_xxx"
     * appSecret: "xxx"
     * robotOpenId: "ou_xxx"
     * wakeWord: "help"
     * whitelistEnabled: false
     * whitelist: ["ou_xxx"]
     */
    private Map<String, Object> credentials;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;



    /**
     * 检查是否为关键级别
     */
    public boolean isEnabled() {
        return BooleanUtils.isTrue(enabled);
    }

}
