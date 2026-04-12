package org.dragon.asset.service;

import lombok.extern.slf4j.Slf4j;
import org.dragon.approval.enums.ApprovalType;
import org.dragon.asset.store.AssetMemberStore;
import org.dragon.notification.service.NotificationService;
import org.dragon.permission.enums.ResourceType;
import org.dragon.store.StoreFactory;
import org.dragon.approval.service.ApprovalService;
import org.dragon.user.store.UserStore;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * CollaboratorService 协作者服务
 * 负责协作者的邀请和移除（包含审批流程）
 */
@Slf4j
@Service
public class CollaboratorService {

    private final AssetMemberStore assetMemberStore;
    private final ApprovalService approvalService;
    private final AssetMemberService assetMemberService;
    private final NotificationService notificationService;
    private final UserStore userStore;

    public CollaboratorService(StoreFactory storeFactory,
                              @Lazy ApprovalService approvalService,
                              @Lazy AssetMemberService assetMemberService,
                              NotificationService notificationService,
                              UserStore userStore) {
        this.assetMemberStore = storeFactory.get(AssetMemberStore.class);
        this.approvalService = approvalService;
        this.assetMemberService = assetMemberService;
        this.notificationService = notificationService;
        this.userStore = userStore;
    }

    /**
     * 邀请协作者
     */
    public void inviteCollaborator(ResourceType type, String assetId, Long ownerId, Long collaboratorId, String reason) {
        if (assetMemberStore.exists(type, assetId, collaboratorId)) {
            throw new IllegalArgumentException("该用户已是协作者");
        }

        // 获取邀请人名称
        String inviterName = userStore.findById(ownerId)
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                .orElse("Unknown");

        if (approvalService.requiresApproval(type, ApprovalType.ADD_COLLABORATOR)) {
            // 需要审批时，先创建 pending 状态的 asset_member 记录（让被邀请人能查到邀请）
            assetMemberService.addMemberDirectly(type, assetId, ownerId, collaboratorId);
            try {
                // 然后创建审批请求
                approvalService.createApprovalRequest(type, assetId, ApprovalType.ADD_COLLABORATOR, ownerId, ownerId, collaboratorId, reason);
            } catch (IllegalArgumentException e) {
                // 如果审批请求创建失败，回滚 asset_member 记录
                assetMemberService.removeMemberDirectly(type, assetId, collaboratorId);
                throw e;
            }
            // 发送邀请通知给被邀请人
            notificationService.notifyCollaboratorInvite(
                    collaboratorId,
                    inviterName,
                    type.name() + ":" + assetId,
                    type.name(),
                    assetId,
                    null
            );
        } else {
            assetMemberService.addMemberDirectly(type, assetId, ownerId, collaboratorId);
        }
    }

    /**
     * 移除协作者
     * OWNER 或 ADMIN 可以直接移除协作者（无需审批）
     * 注意：不能移除 OWNER
     */
    public void removeCollaborator(ResourceType type, String assetId, Long operatorId, Long collaboratorId) {
        // 检查不能移除 OWNER
        if (assetMemberService.isOwner(type, assetId, collaboratorId)) {
            throw new IllegalArgumentException("不能移除资产所有者（OWNER）");
        }

        // 检查操作者是否是 OWNER 或 ADMIN，如果是则直接移除，否则需要审批
        boolean isOwnerOrAdmin = assetMemberService.isOwner(type, assetId, operatorId)
                || assetMemberStore.findByResourceAndUser(type, assetId, operatorId)
                        .map(m -> m.getRole() == org.dragon.permission.enums.Role.ADMIN)
                        .orElse(false);

        if (isOwnerOrAdmin) {
            // OWNER 或 ADMIN 直接移除，无需审批
            assetMemberService.removeMemberDirectly(type, assetId, collaboratorId);
        } else if (approvalService.requiresApproval(type, ApprovalType.REMOVE_COLLABORATOR)) {
            approvalService.createApprovalRequest(type, assetId, ApprovalType.REMOVE_COLLABORATOR, operatorId, operatorId, collaboratorId, "移除协作者");
        } else {
            assetMemberService.removeMemberDirectly(type, assetId, collaboratorId);
        }
    }
}