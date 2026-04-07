package org.dragon.notification.dto;

/**
 * NotificationType 通知类型枚举
 */
public enum NotificationType {
    APPROVAL_REQUEST,    // 审批请求（需要我审批）
    APPROVAL_RESULT,     // 审批结果（我申请的已处理）
    COLLABORATOR_INVITE, // 协作者邀请
    SYSTEM               // 系统通知
}
