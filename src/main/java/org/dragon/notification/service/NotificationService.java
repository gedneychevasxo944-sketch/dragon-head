package org.dragon.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.dragon.notification.dto.NotificationDTO;
import org.dragon.datasource.entity.NotificationEntity;
import org.dragon.datasource.entity.NotificationType;
import org.dragon.notification.store.NotificationStore;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * NotificationService 通知服务
 */
@Slf4j
@Service
public class NotificationService {

    private final NotificationStore notificationStore;

    public NotificationService(StoreFactory storeFactory) {
        this.notificationStore = storeFactory.get(NotificationStore.class);
    }

    /**
     * 发送通知
     */
    public String sendNotification(Long userId, NotificationType type, String title, String content,
                                    String link, String sourceType, String sourceId) {
        NotificationEntity notification = NotificationEntity.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .type(type)
                .title(title)
                .content(content)
                .link(link)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationStore.save(notification);
        log.info("[NotificationService] Sent notification: userId={}, type={}, title={}", userId, type, title);
        return notification.getId();
    }

    /**
     * 获取用户通知列表
     */
    public List<NotificationDTO> getNotifications(Long userId, int limit) {
        List<NotificationEntity> notifications = notificationStore.findByUserId(userId);
        return notifications.stream()
                .limit(limit)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户未读通知数量
     */
    public long getUnreadCount(Long userId) {
        return notificationStore.countUnreadByUserId(userId);
    }

    /**
     * 标记单条通知为已读
     */
    public void markAsRead(String notificationId) {
        notificationStore.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationStore.update(n);
            log.info("[NotificationService] Marked notification as read: id={}", notificationId);
        });
    }

    /**
     * 标记所有通知为已读
     */
    public void markAllAsRead(Long userId) {
        notificationStore.markAllAsReadByUserId(userId);
        log.info("[NotificationService] Marked all notifications as read: userId={}", userId);
    }

    /**
     * 删除通知
     */
    public void deleteNotification(String notificationId) {
        notificationStore.delete(notificationId);
        log.info("[NotificationService] Deleted notification: id={}", notificationId);
    }

    /**
     * 发送审批请求通知（给审批人）
     */
    public void notifyApprovalRequest(Long approverId, String resourceName, String requesterName,
                                      String approvalId, String link) {
        sendNotification(
                approverId,
                NotificationType.APPROVAL_REQUEST,
                "新的审批请求",
                requesterName + " 申请发布 " + resourceName,
                link,
                "APPROVAL",
                approvalId
        );
    }

    /**
     * 发送审批结果通知（给申请人）
     */
    public void notifyApprovalResult(Long requesterId, String resourceName, boolean approved,
                                     String approverName, String approvalId, String link) {
        String title = approved ? "发布申请已通过" : "发布申请被驳回";
        String content = approverName + (approved ? " 通过了您的 " : " 驳回了您的 ") + resourceName + " 发布申请";
        sendNotification(
                requesterId,
                NotificationType.APPROVAL_RESULT,
                title,
                content,
                link,
                "APPROVAL",
                approvalId
        );
    }

    /**
     * 发送协作者邀请通知
     */
    public void notifyCollaboratorInvite(Long inviteeId, String inviterName, String resourceName,
                                         String resourceType, String resourceId, String link) {
        sendNotification(
                inviteeId,
                NotificationType.COLLABORATOR_INVITE,
                "新的协作者邀请",
                inviterName + " 邀请您成为 " + resourceName + " 的协作者",
                link,
                resourceType,
                resourceId
        );
    }

    private NotificationDTO toDTO(NotificationEntity entity) {
        return NotificationDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .type(entity.getType())
                .title(entity.getTitle())
                .content(entity.getContent())
                .link(entity.getLink())
                .sourceType(entity.getSourceType())
                .sourceId(entity.getSourceId())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
