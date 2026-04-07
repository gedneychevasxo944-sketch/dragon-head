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

    private String resourceName;

    private Role role;

    private String invitedBy;

    private LocalDateTime invitedAt;

    private LocalDateTime acceptedAt;

    private boolean accepted;

    private LocalDateTime createdAt;
}
