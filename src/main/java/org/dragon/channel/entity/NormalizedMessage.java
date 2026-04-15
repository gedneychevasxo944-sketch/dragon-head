package org.dragon.channel.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dragon.channel.enums.ChatType;
import org.dragon.channel.enums.ChannelType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Description: 客观发生的事实上下文,记录发生了什么
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/13 23:07
 * Update Date Time:
 *
 */
@Getter
@Setter
@NoArgsConstructor
public class NormalizedMessage {
    // ================= 1. 身份与路由 (Identity & Routing) =================
    private ChannelType channel;     // 来源渠道
    private String senderId;    // 发送者全局唯一标识
    private String chatId;      // 会话ID
    private ChatType chatType;    // 会话类型
    private String messageId;   // 原始消息 ID (用于引用回复)
    private String workspaceId; // 工作空间ID，为null表示未绑定Workspace，Gateway将走单Character Fallback路径
    // ================= 2. 引用与线程 (Reference & Threading) =================
    private String quoteMessageId;   // 被引用消息 ID（用于判断是否回复某消息）
    private String threadId;         // 线程 ID（同一线程的消息属于同一会话）
    private String parentMessageId;   // 父消息 ID（用于嵌套回复）
    private LocalDateTime messageTime; // 消息时间
    // ================= 3. 路由上下文 (Routing Context) =================
    private Map<String, Object> routingContext; // 扩展路由信息
    // ================= 4. 基础载荷 (Payload) =================
    private String textContent;             // 清洗后的纯文本/语音转录文本
    private List<NormalizedFile> normalizedFiles;   // 附件列表 (图片、文件等)
    // ================= 5. 元数据信封 (Metadata) =================
    private Map<String, Object> metadata;   // 包含时间戳、原始环境信息等
}
