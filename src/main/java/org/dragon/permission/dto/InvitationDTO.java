package org.dragon.permission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.permission.enums.ResourceType;

import java.time.LocalDateTime;

/**
 * InvitationDTO 邀请信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationDTO {

    private Long id;

    private ResourceType resourceType;

    private String resourceId;

    private String resourceName;

    private Long inviterId;

    private String inviterName;

    private LocalDateTime invitedAt;

    private boolean accepted;
}
