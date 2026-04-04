package org.dragon.permission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.permission.enums.Role;
import org.dragon.permission.enums.ResourceType;

import java.time.LocalDateTime;

/**
 * CollaboratorDTO 协作者信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaboratorDTO {

    private Long id;

    private ResourceType resourceType;

    private String resourceId;

    private String resourceName;

    private Long userId;

    private String userName;

    private Role role;

    private String invitedBy;

    private LocalDateTime invitedAt;

    private LocalDateTime acceptedAt;

    private boolean accepted;
}
