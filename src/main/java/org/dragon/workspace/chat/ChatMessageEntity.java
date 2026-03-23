package org.dragon.workspace.chat;

import io.ebean.annotation.DbJson;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ChatMessageEntity 聊天消息实体
 * 映射数据库 chat_message 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_message", indexes = {
    @Index(name = "idx_workspace_id", columnList = "workspace_id"),
    @Index(name = "idx_sender_id", columnList = "sender_id"),
    @Index(name = "idx_receiver_id", columnList = "receiver_id"),
    @Index(name = "idx_session_id", columnList = "session_id"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
public class ChatMessageEntity {

    @Id
    private String id;

    private String workspaceId;

    private String senderId;

    private String receiverId;

    private String content;

    private String messageType;

    private String sessionId;

    private LocalDateTime timestamp;

    private boolean read;

    @DbJson
    private Map<String, Object> metadata;

    /**
     * 转换为ChatMessage
     */
    public ChatMessage toChatMessage() {
        return ChatMessage.builder()
                .id(this.id)
                .workspaceId(this.workspaceId)
                .senderId(this.senderId)
                .receiverId(this.receiverId)
                .content(this.content)
                .messageType(ChatMessage.MessageType.valueOf(this.messageType))
                .sessionId(this.sessionId)
                .timestamp(this.timestamp)
                .read(this.read)
                .metadata(this.metadata)
                .build();
    }

    /**
     * 从ChatMessage创建Entity
     */
    public static ChatMessageEntity fromChatMessage(ChatMessage message) {
        return ChatMessageEntity.builder()
                .id(message.getId())
                .workspaceId(message.getWorkspaceId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .content(message.getContent())
                .messageType(message.getMessageType().name())
                .sessionId(message.getSessionId())
                .timestamp(message.getTimestamp())
                .read(message.isRead())
                .metadata(message.getMetadata())
                .build();
    }
}