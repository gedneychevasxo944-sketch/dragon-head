package org.dragon.asset.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dragon.approval.enums.ApprovalStatus;
import org.dragon.approval.store.ApprovalStore;
import org.dragon.asset.dto.AssetMemberDTO;
import org.dragon.asset.dto.CollaboratorDTO;
import org.dragon.asset.store.AssetMemberStore;
import org.dragon.asset.store.AssetPublishStatusStore;
import org.dragon.character.CharacterRegistry;
import org.dragon.datasource.entity.ApprovalRequestEntity;
import org.dragon.datasource.entity.AssetMemberEntity;
import org.dragon.permission.dto.InvitationDTO;
import org.dragon.permission.enums.ResourceType;
import org.dragon.permission.enums.Role;
import org.dragon.store.StoreFactory;
import org.dragon.trait.store.TraitStore;
import org.dragon.user.store.UserStore;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * AssetMemberService 资产成员服务
 * 负责管理资产的 owner 和 collaborator 基础 CRUD 操作
 */
@Slf4j
@Service
public class AssetMemberService {

    private final AssetMemberStore assetMemberStore;
    private final UserStore userStore;
    private final AssetPublishStatusStore publishStatusStore;
    private final TraitStore traitStore;
    private final CharacterRegistry characterRegistry;
    private final ApprovalStore approvalStore;

    public AssetMemberService(StoreFactory storeFactory, CharacterRegistry characterRegistry) {
        this.assetMemberStore = storeFactory.get(AssetMemberStore.class);
        this.userStore = storeFactory.get(UserStore.class);
        this.publishStatusStore = storeFactory.get(AssetPublishStatusStore.class);
        this.traitStore = storeFactory.get(TraitStore.class);
        this.characterRegistry = characterRegistry;
        this.approvalStore = storeFactory.get(ApprovalStore.class);
    }

    /**
     * 获取用户的所有资产成员关系（包含 owner 和 collaborator）
     */
    public List<AssetMemberDTO> getMyAssets(Long userId) {
        List<AssetMemberEntity> members = assetMemberStore.findByUserId(userId);
        if (members.isEmpty()) {
            return List.of();
        }

        // 批量获取资产名称
        Map<ResourceType, Map<String, String>> namesByType = batchLoadResourceNames(members);

        // 批量获取待审批信息
        PendingApprovalInfo pendingInfo = batchLoadPendingApprovalInfo(userId);

        return members.stream()
                .map(m -> toAssetMemberDTOWithNames(m, namesByType, pendingInfo))
                .collect(Collectors.toList());
    }

    /**
     * 批量加载待审批信息
     */
    private PendingApprovalInfo batchLoadPendingApprovalInfo(Long userId) {
        // 查询用户作为申请人提交的待审批请求
        List<ApprovalRequestEntity> myRequests = approvalStore.findByRequester(userId).stream()
                .filter(r -> r.getStatus() == ApprovalStatus.PENDING)
                .collect(Collectors.toList());

        // 查询需要用户审批的待审批请求
        List<ApprovalRequestEntity> pendingApprovals = approvalStore.findPendingByApprover(userId).stream()
                .filter(r -> r.getStatus() == ApprovalStatus.PENDING)
                .collect(Collectors.toList());

        // 构建资源标识 -> 是否是我提交的标识
        java.util.Set<String> myPendingSet = myRequests.stream()
                .map(r -> r.getResourceType().name() + ":" + r.getResourceId())
                .collect(Collectors.toSet());

        // 构建资源标识 -> 是否是需要我审批的标识
        java.util.Set<String> needsMyApprovalSet = pendingApprovals.stream()
                .map(r -> r.getResourceType().name() + ":" + r.getResourceId())
                .collect(Collectors.toSet());

        return new PendingApprovalInfo(myPendingSet, needsMyApprovalSet);
    }

    private static class PendingApprovalInfo {
        private final java.util.Set<String> myPendingApprovals;
        private final java.util.Set<String> needsMyApproval;

        PendingApprovalInfo(java.util.Set<String> myPendingApprovals, java.util.Set<String> needsMyApproval) {
            this.myPendingApprovals = myPendingApprovals;
            this.needsMyApproval = needsMyApproval;
        }
    }

    /**
     * 批量加载资源名称
     */
    private Map<ResourceType, Map<String, String>> batchLoadResourceNames(List<AssetMemberEntity> members) {
        Map<ResourceType, Map<String, String>> result = new java.util.HashMap<>();

        // 按类型分组
        Map<ResourceType, List<String>> idsByType = members.stream()
                .collect(Collectors.groupingBy(
                        AssetMemberEntity::getResourceType,
                        Collectors.mapping(AssetMemberEntity::getResourceId, Collectors.toList())
                ));

        // 批量查询各类型资源名称
        for (Map.Entry<ResourceType, List<String>> entry : idsByType.entrySet()) {
            ResourceType type = entry.getKey();
            List<String> ids = entry.getValue();
            Map<String, String> nameMap = new java.util.HashMap<>();

            switch (type) {
                case TRAIT:
                    List<Long> traitIds = ids.stream().map(Long::parseLong).collect(Collectors.toList());
                    List<org.dragon.datasource.entity.TraitEntity> traits = traitStore.findByIds(traitIds);
                    for (org.dragon.datasource.entity.TraitEntity t : traits) {
                        nameMap.put(String.valueOf(t.getId()), t.getName());
                    }
                    break;
                case CHARACTER:
                    List<org.dragon.character.Character> characters = characterRegistry.findByIds(ids);
                    for (org.dragon.character.Character c : characters) {
                        nameMap.put(c.getId(), c.getName());
                    }
                    break;
                default:
                    break;
            }
            result.put(type, nameMap);
        }
        return result;
    }

    /**
     * 获取用户作为成员（owner/collaborator）的指定类型资产的 ID 列表
     *
     * @param resourceType 资源类型
     * @param userId      用户 ID
     * @return 用户作为成员的资产 ID 列表
     */
    public List<String> getMemberAssetIds(ResourceType resourceType, Long userId) {
        return assetMemberStore.findByUserId(userId).stream()
                .filter(m -> m.getResourceType() == resourceType && Boolean.TRUE.equals(m.getAccepted()))
                .map(m -> m.getResourceId())
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
     * 确认协作者邀请（审批通过后调用）
     * 将 pending 状态的邀请确认为已接受
     * @throws IllegalStateException 如果邀请记录不存在
     */
    public void acceptInvitationDirectly(Long userId, ResourceType type, String resourceId) {
        AssetMemberEntity member = assetMemberStore.findByResourceAndUser(type, resourceId, userId)
                .orElseThrow(() -> new IllegalStateException(
                        "邀请记录不存在，无法确认: userId=" + userId + ", type=" + type + ", resourceId=" + resourceId));
        member.setAccepted(true);
        member.setAcceptedAt(LocalDateTime.now());
        assetMemberStore.update(member);
        log.info("[AssetMemberService] Accepted invitation directly (approval): type={}, resourceId={}, userId={}", type, resourceId, userId);
    }

    /**
     * 拒绝协作者邀请（审批拒绝后调用）
     * 删除 pending 状态的邀请记录
     * @throws IllegalStateException 如果邀请记录不存在
     */
    public void rejectInvitationDirectly(Long userId, ResourceType type, String resourceId) {
        AssetMemberEntity member = assetMemberStore.findByResourceAndUser(type, resourceId, userId)
                .orElseThrow(() -> new IllegalStateException(
                        "邀请记录不存在，无法拒绝: userId=" + userId + ", type=" + type + ", resourceId=" + resourceId));
        assetMemberStore.delete(member.getId());
        log.info("[AssetMemberService] Rejected invitation directly (approval): type={}, resourceId={}, userId={}", type, resourceId, userId);
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

    /**
     * 安全解析 inviterId，避免 NPE 和空字符串问题
     */
    private Long parseInviterId(String invitedBy) {
        if (invitedBy == null || invitedBy.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(invitedBy);
        } catch (NumberFormatException e) {
            log.warn("[AssetMemberService] Invalid inviterId format: {}", invitedBy);
            return null;
        }
    }

    private InvitationDTO toInvitationDTO(AssetMemberEntity member) {
        Long inviterId = parseInviterId(member.getInvitedBy());
        String inviterName = inviterId != null
                ? userStore.findById(inviterId)
                        .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                        .orElse("Unknown")
                : "Unknown";

        // 构建邀请ID，格式: resourceType_resourceId_userId
        String invitationId = member.getResourceType().name() + "_" + member.getResourceId() + "_" + member.getUserId();

        return InvitationDTO.builder()
                .id(invitationId)  // 使用复合ID格式，供 accept/reject API 使用
                .resourceType(member.getResourceType())
                .resourceId(member.getResourceId())
                .inviterId(inviterId)
                .inviterName(inviterName)
                .invitedAt(member.getInvitedAt())
                .accepted(member.getAccepted())
                .build();
    }

    /**
     * 使用预加载的名称映射将 AssetMemberEntity 转换为 DTO
     */
    private AssetMemberDTO toAssetMemberDTOWithNames(AssetMemberEntity member, Map<ResourceType, Map<String, String>> namesByType, PendingApprovalInfo pendingInfo) {
        // 获取发布状态
        String publishStatus = publishStatusStore
                .findByResource(member.getResourceType().name(), member.getResourceId())
                .map(entity -> {
                    String status = entity.getStatus();
                    if ("PUBLISHED".equals(status)) {
                        return "published";
                    }
                    return "unpublished";
                })
                .orElse("unpublished");

        // 从预加载的名称映射中获取资产名称
        Map<String, String> typeNames = namesByType.get(member.getResourceType());
        String resourceName = typeNames != null ? typeNames.get(member.getResourceId()) : null;

        // 检查待审批状态
        String resourceKey = member.getResourceType().name() + ":" + member.getResourceId();
        boolean hasPendingApproval = pendingInfo.myPendingApprovals.contains(resourceKey);
        boolean needsMyApproval = pendingInfo.needsMyApproval.contains(resourceKey);

        return AssetMemberDTO.builder()
                .id(member.getId())
                .resourceType(member.getResourceType())
                .resourceId(member.getResourceId())
                .resourceName(resourceName)
                .role(member.getRole())
                .publishStatus(publishStatus)
                .invitedBy(member.getInvitedBy())
                .invitedAt(member.getInvitedAt())
                .acceptedAt(member.getAcceptedAt())
                .accepted(member.getAccepted())
                .createdAt(member.getCreatedAt())
                .hasPendingApproval(hasPendingApproval)
                .needsMyApproval(needsMyApproval)
                .build();
    }

    /**
     * 根据资源类型和ID解析资产名称
     */
    private String resolveResourceName(ResourceType type, String resourceId) {
        return switch (type) {
            case TRAIT -> traitStore.findById(Long.valueOf(resourceId))
                    .map(t -> t.getName())
                    .orElse(null);
            case CHARACTER -> characterRegistry.get(resourceId)
                    .map(c -> c.getName())
                    .orElse(null);
            default -> null;
        };
    }
}