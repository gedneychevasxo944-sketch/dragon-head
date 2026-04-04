package org.dragon.datasource.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    private ResourceType resourceType;

    private String resourceId;

    @Enumerated(EnumType.STRING)
    private ApprovalType approvalType;

    private Long requesterId;

    private String requesterName;

    private Long approverId;

    private String approverName;

    private String reason;

    private String status;

    private LocalDateTime requestedAt;

    private LocalDateTime processedAt;

    private String processedComment;
}