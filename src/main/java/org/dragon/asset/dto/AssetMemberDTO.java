package org.dragon.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.permission.enums.Role;
import org.dragon.permission.enums.ResourceType;

import java.time.LocalDateTime;

/**
 * AssetMemberDTO 资产成员信息（用于我的资产列表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetMemberDTO {

    private Long id;

    private ResourceType resourceType;

    private String resourceId;

    /**
     * 资源名称（如 trait 名称、character 名称等）
     */
    private String resourceName;

    private Role role;

    /**
     * 发布状态：unpublished（私有）、pending（待发布）、published（已发布）
     */
    private String publishStatus;

    private String invitedBy;

    private LocalDateTime invitedAt;

    private LocalDateTime acceptedAt;

    private boolean accepted;

    private LocalDateTime createdAt;

    /**
     * 用户是否提交过该资产的待审批申请
     */
    private boolean hasPendingApproval;

    /**
     * 是否有需要该用户审批的申请
     */
    private boolean needsMyApproval;
}
