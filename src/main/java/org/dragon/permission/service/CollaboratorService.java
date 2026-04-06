package org.dragon.permission.service;

import lombok.extern.slf4j.Slf4j;
import org.dragon.permission.dto.CollaboratorDTO;
import org.dragon.permission.dto.InvitationDTO;
import org.dragon.datasource.entity.AssetMemberEntity;
import org.dragon.permission.enums.ApprovalType;
import org.dragon.permission.enums.Role;
import org.dragon.permission.store.AssetMemberStore;
import org.dragon.store.StoreFactory;
import org.dragon.permission.enums.ResourceType;
import org.dragon.user.store.UserStore;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CollaboratorService 协作者服务
 * 负责管理资产的协作者关系
 */
@Slf4j
@Service
public class CollaboratorService {

    private final AssetMemberStore assetMemberStore;
    private final UserStore userStore;
    private final ApprovalService approvalService;

    public CollaboratorService(StoreFactory storeFactory, @Lazy ApprovalService approvalService) {
        this.assetMemberStore = storeFactory.get(AssetMemberStore.class);
        this.userStore = storeFactory.get(UserStore.class);
        this.approvalService = approvalService;
    }

    /**
     * 邀请协作者
     */
    public void inviteCollaborator(ResourceType type, String assetId, Long ownerId, Long collaboratorId, String reason) {
        if (assetMemberStore.exists(type, assetId, collaboratorId)) {
            throw new IllegalArgumentException("该用户已是协作者");
        }

        if (approvalService.requiresApproval(type, ApprovalType.ADD_COLLABORATOR)) {
            approvalService.createApprovalRequest(type, assetId, ApprovalType.ADD_COLLABORATOR, ownerId, collaboratorId, reason);
        } else {
            addMemberDirectly(type, assetId, ownerId, collaboratorId);
        }
    }

    /**
     * 直接添加协作者（审批通过后调用）
     */
    public void addMemberDirectly(ResourceType type, String assetId, Long ownerId, Long collaboratorId) {
        LocalDateTime now = LocalDateTime.now();
        AssetMemberEntity member = AssetMemberEntity.builder()
                .resourceType(type)
                .resourceId(assetId)
                .userId(collaboratorId)
                .role(Role.COLLABORATOR)
                .invitedBy(String.valueOf(ownerId))
                .invitedAt(now)
                .acceptedAt(now)
                .accepted(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
        assetMemberStore.save(member);
        log.info("[CollaboratorService] Added collaborator: type={}, assetId={}, collaboratorId={}", type, assetId, collaboratorId);
    }

    /**
     * 添加资产所有者（资产创建时调用）
     */
    public void addOwnerDirectly(ResourceType type, String assetId, Long ownerId) {
        LocalDateTime now = LocalDateTime.now();
        AssetMemberEntity member = AssetMemberEntity.builder()
                .resourceType(type)
                .resourceId(assetId)
                .userId(ownerId)
                .role(Role.OWNER)
                .invitedBy(String.valueOf(ownerId))
                .invitedAt(now)
                .acceptedAt(now)
                .accepted(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
        assetMemberStore.save(member);
        log.info("[CollaboratorService] Added owner: type={}, assetId={}, ownerId={}", type, assetId, ownerId);
    }

    /**
     * 移除协作者
     */
    public void removeCollaborator(ResourceType type, String assetId, Long ownerId, Long collaboratorId) {
        if (approvalService.requiresApproval(type, ApprovalType.REMOVE_COLLABORATOR)) {
            approvalService.createApprovalRequest(type, assetId, ApprovalType.REMOVE_COLLABORATOR, ownerId, collaboratorId, "移除协作者");
        } else {
            removeMemberDirectly(type, assetId, collaboratorId);
        }
    }

    /**
     * 直接移除协作者（审批通过后调用）
     */
    public void removeMemberDirectly(ResourceType type, String assetId, Long collaboratorId) {
        assetMemberStore.findByResourceAndUser(type, assetId, collaboratorId)
                .ifPresent(m -> {
                    assetMemberStore.delete(m.getId());
                    log.info("[CollaboratorService] Removed collaborator: type={}, assetId={}, collaboratorId={}", type, assetId, collaboratorId);
                });
    }

    /**
     * 接受邀请
     */
    public void acceptInvitation(Long userId, ResourceType type, String resourceId) {
        assetMemberStore.findByResourceAndUser(type, resourceId, userId)
                .ifPresent(m -> {
                    m.setAccepted(true);
                    m.setAcceptedAt(LocalDateTime.now());
                    assetMemberStore.update(m);
                    log.info("[CollaboratorService] Accepted invitation: type={}, resourceId={}, userId={}", type, resourceId, userId);
                });
    }

    /**
     * 拒绝邀请
     */
    public void rejectInvitation(Long userId, ResourceType type, String resourceId) {
        assetMemberStore.findByResourceAndUser(type, resourceId, userId)
                .ifPresent(m -> {
                    assetMemberStore.delete(m.getId());
                    log.info("[CollaboratorService] Rejected invitation: type={}, resourceId={}, userId={}", type, resourceId, userId);
                });
    }

    /**
     * 获取资产的协作者列表
     */
    public List<CollaboratorDTO> getCollaborators(ResourceType type, String assetId) {
        return assetMemberStore.findByResource(type, assetId).stream()
                .filter(m -> Boolean.TRUE.equals(m.getAccepted()))
                .map(this::toCollaboratorDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的待处理邀请
     */
    public List<InvitationDTO> getPendingInvitations(Long userId) {
        return assetMemberStore.findPendingInvitationsByUserId(userId).stream()
                .map(this::toInvitationDTO)
                .collect(Collectors.toList());
    }

    /**
     * 检查用户是否是协作者
     */
    public boolean isCollaborator(ResourceType type, String assetId, Long userId) {
        return assetMemberStore.findByResourceAndUser(type, assetId, userId)
                .map(m -> Boolean.TRUE.equals(m.getAccepted()))
                .orElse(false);
    }

    private CollaboratorDTO toCollaboratorDTO(AssetMemberEntity member) {
        String userName = userStore.findById(member.getUserId())
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                .orElse("Unknown");

        return CollaboratorDTO.builder()
                .id(member.getId())
                .resourceType(member.getResourceType())
                .resourceId(member.getResourceId())
                .userId(member.getUserId())
                .userName(userName)
                .role(member.getRole())
                .invitedBy(member.getInvitedBy())
                .invitedAt(member.getInvitedAt())
                .acceptedAt(member.getAcceptedAt())
                .accepted(member.getAccepted())
                .build();
    }

    private InvitationDTO toInvitationDTO(AssetMemberEntity member) {
        String inviterName = member.getInvitedBy() != null
                ? userStore.findById(Long.parseLong(member.getInvitedBy()))
                        .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                        .orElse("Unknown")
                : "Unknown";

        return InvitationDTO.builder()
                .id(member.getId())
                .resourceType(member.getResourceType())
                .resourceId(member.getResourceId())
                .inviterId(member.getInvitedBy() != null ? Long.parseLong(member.getInvitedBy()) : null)
                .inviterName(inviterName)
                .invitedAt(member.getInvitedAt())
                .accepted(member.getAccepted())
                .build();
    }
}
