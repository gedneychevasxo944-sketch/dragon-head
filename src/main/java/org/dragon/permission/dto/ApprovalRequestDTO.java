package org.dragon.permission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.permission.enums.ApprovalStatus;
import org.dragon.permission.enums.ApprovalType;
import org.dragon.permission.enums.ResourceType;

import java.time.LocalDateTime;

/**
 * ApprovalRequestDTO 审批请求信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequestDTO {

    private String id;

    private ResourceType resourceType;

    private String resourceId;

    private String resourceName;

    private ApprovalType approvalType;

    private Long requesterId;

    private String requesterName;

    private Long targetUserId;

    private String targetUserName;

    private Long approverId;

    private String approverName;

    private String reason;

    private ApprovalStatus status;

    private LocalDateTime requestedAt;

    private LocalDateTime processedAt;

    private String processedComment;
}
