package org.dragon.channel.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**

 * ChannelBinding 渠道绑定实体
 * 描述一个 IM 会话（群/私聊）与一个 Workspace 的绑定关系
 * 一个 Workspace 可以绑定多个会话，一个会话只能绑定一个 Workspace
 *
 * @author zhz
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelBinding {
    /**
     * 绑定唯一标识
     * 格式: channelName_chatId
     */
    private String id;
    /**
     * 所属 Workspace ID
     */
    private String workspaceId;
    /**
     * 渠道名称（对应 ChannelAdapter.getChannelName()）
     * 例如: "Feishu", "DingTalk", "WeChat"
     */
    private String channelName;
    /**
     * 渠道内的会话 ID
     * 群聊场景: 群的 chatId（如飞书的 oc_xxx）
     * 私聊场景: 用户的 senderId（如飞书的 ou_xxx）
     */
    private String chatId;
    /**
     * 会话类型
     * "group": 群聊
     * "p2p": 私聊
     */
    private String chatType;
    /**
     * 绑定描述（可选，用于后台展示）
     */
    private String description;
    /**
     * 是否启用
     */
    @Builder.Default
    private Boolean enabled = true;
    /**
     * 扩展属性（如特定渠道的额外路由配置）
     */
    private Map<String, Object> metadata;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    /**
     * 创建复合 ID
     *
     * @param channelName 渠道名称
     * @param chatId 会话 ID
     * @return 复合 ID
     */
    public static String createId(String channelName, String chatId) {
        return channelName + "_" + chatId;
    }

    /**
     * 检查是否启用
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

}
