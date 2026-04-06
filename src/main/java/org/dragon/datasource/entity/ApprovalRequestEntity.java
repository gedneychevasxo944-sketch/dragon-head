package org.dragon.datasource.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.permission.enums.ApprovalStatus;
import org.dragon.permission.enums.ApprovalType;
import org.dragon.permission.enums.ResourceType;

import java.time.LocalDateTime;

/**
 * ApprovalRequestEntity 审批请求表
 * 用于存储发布、取消发布、添加/移除协作者等需要审批的请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "approval_request")
public class ApprovalRequestEntity {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 32)
    private ResourceType resourceType;

    @Column(name = "resource_id", nullable = false, length = 64)
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_type", nullable = false, length = 32)
    private ApprovalType approvalType;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "requester_name")
    private String requesterName;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "approver_id")
    private Long approverId;

    @Column(name = "approver_name")
    private String approverName;

    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApprovalStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processed_comment")
    private String processedComment;

    @PrePersist
    protected void onCreate() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ApprovalStatus.PENDING;
        }
    }
}