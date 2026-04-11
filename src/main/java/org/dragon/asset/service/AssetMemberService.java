package org.dragon.asset.service;

import lombok.extern.slf4j.Slf4j;

import org.dragon.asset.dto.AssetMemberDTO;
import org.dragon.asset.dto.CollaboratorDTO;
import org.dragon.asset.store.AssetMemberStore;
import org.dragon.datasource.entity.AssetMemberEntity;
import org.dragon.permission.dto.InvitationDTO;
import org.dragon.permission.enums.Role;
import org.dragon.permission.enums.ResourceType;
import org.dragon.store.StoreFactory;
import org.dragon.user.store.UserStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AssetMemberService 资产成员服务
 * 负责管理资产的 owner 和 collaborator 基础 CRUD 操作
 */
@Slf4j
@Service
public class AssetMemberService {

    private final AssetMemberStore assetMemberStore;
    private final UserStore userStore;

    public AssetMemberService(StoreFactory storeFactory) {
        this.assetMemberStore = storeFactory.get(AssetMemberStore.class);
        this.userStore = storeFactory.get(UserStore.class);
    }

    /**
     * 获取用户的所有资产成员关系（包含 owner 和 collaborator）
     */
    public List<AssetMemberDTO> getMyAssets(Long userId) {
        return assetMemberStore.findByUserId(userId).stream()
                .map(this::toAssetMemberDTO)
                .collect(Collectors.toList());
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
        log.info("[AssetMemberService] Added owner: type={}, assetId={}, ownerId={}", type, assetId, ownerId);
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
        log.info("[AssetMemberService] Added collaborator: type={}, assetId={}, collaboratorId={}", type, assetId, collaboratorId);
    }

    /**
     * 直接移除协作者（审批通过后调用）
     */
    public void removeMemberDirectly(ResourceType type, String assetId, Long collaboratorId) {
        assetMemberStore.findByResourceAndUser(type, assetId, collaboratorId)
                .ifPresent(m -> {
                    assetMemberStore.delete(m.getId());
                    log.info("[AssetMemberService] Removed collaborator: type={}, assetId={}, collaboratorId={}", type, assetId, collaboratorId);
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
                    log.info("[AssetMemberService] Accepted invitation: type={}, resourceId={}, userId={}", type, resourceId, userId);
                });
    }

    /**
     * 拒绝邀请
     */
    public void rejectInvitation(Long userId, ResourceType type, String resourceId) {
        assetMemberStore.findByResourceAndUser(type, resourceId, userId)
                .ifPresent(m -> {
                    assetMemberStore.delete(m.getId());
                    log.info("[AssetMemberService] Rejected invitation: type={}, resourceId={}, userId={}", type, resourceId, userId);
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

    /**
     * 检查用户是否是所有者
     */
    public boolean isOwner(ResourceType type, String assetId, Long userId) {
        return assetMemberStore.findByResourceAndUser(type, assetId, userId)
                .map(m -> m.getRole() == Role.OWNER && Boolean.TRUE.equals(m.getAccepted()))
                .orElse(false);
    }

    /**
     * 更新成员角色
     */
    public void updateRole(ResourceType type, String assetId, Long userId, Role newRole) {
        assetMemberStore.findByResourceAndUser(type, assetId, userId)
                .ifPresent(m -> {
                    m.setRole(newRole);
                    m.setUpdatedAt(LocalDateTime.now());
                    assetMemberStore.update(m);
                    log.info("[AssetMemberService] Updated role: type={}, assetId={}, userId={}, newRole={}", type, assetId, userId, newRole);
                });
    }

    /**
     * 转让所有权
     */
    public void transferOwner(ResourceType type, String assetId, Long currentOwnerId, Long newOwnerId) {
        // 验证当前用户是否是所有者
        if (!isOwner(type, assetId, currentOwnerId)) {
            throw new IllegalStateException("只有所有者才能转让所有权");
        }

        LocalDateTime now = LocalDateTime.now();

        // 将新所有者更新为 OWNER
        assetMemberStore.findByResourceAndUser(type, assetId, newOwnerId)
                .ifPresent(m -> {
                    m.setRole(Role.OWNER);
                    m.setUpdatedAt(now);
                    assetMemberStore.update(m);
                });

        // 将原所有者降级为 ADMIN
        assetMemberStore.findByResourceAndUser(type, assetId, currentOwnerId)
                .ifPresent(m -> {
                    m.setRole(Role.ADMIN);
                    m.setUpdatedAt(now);
                    assetMemberStore.update(m);
                });

        log.info("[AssetMemberService] Transferred ownership: type={}, assetId={}, from={}, to={}", type, assetId, currentOwnerId, newOwnerId);
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

    private AssetMemberDTO toAssetMemberDTO(AssetMemberEntity member) {
        return AssetMemberDTO.builder()
                .id(member.getId())
                .resourceType(member.getResourceType())
                .resourceId(member.getResourceId())
                .role(member.getRole())
                .invitedBy(member.getInvitedBy())
                .invitedAt(member.getInvitedAt())
                .acceptedAt(member.getAcceptedAt())
                .accepted(member.getAccepted())
                .createdAt(member.getCreatedAt())
                .build();
    }
}