package org.dragon.notification.dto;

import org.dragon.datasource.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * NotificationDTO 通知数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    private String id;

    private Long userId;

    private NotificationType type;

    private String title;

    private String content;

    private String link;

    private String sourceType;

    private String sourceId;

    private Boolean isRead;

    private LocalDateTime createdAt;
}
