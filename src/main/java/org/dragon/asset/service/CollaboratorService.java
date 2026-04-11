package org.dragon.asset.service;

import lombok.extern.slf4j.Slf4j;
import org.dragon.approval.enums.ApprovalType;
import org.dragon.asset.store.AssetMemberStore;
import org.dragon.permission.enums.ResourceType;
import org.dragon.store.StoreFactory;
import org.dragon.approval.service.ApprovalService;
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

    public CollaboratorService(StoreFactory storeFactory,
                              @Lazy ApprovalService approvalService,
                              @Lazy AssetMemberService assetMemberService) {
        this.assetMemberStore = storeFactory.get(AssetMemberStore.class);
        this.approvalService = approvalService;
        this.assetMemberService = assetMemberService;
    }

    /**
     * 邀请协作者
     */
    public void inviteCollaborator(ResourceType type, String assetId, Long ownerId, Long collaboratorId, String reason) {
        if (assetMemberStore.exists(type, assetId, collaboratorId)) {
            throw new IllegalArgumentException("该用户已是协作者");
        }

        if (approvalService.requiresApproval(type, ApprovalType.ADD_COLLABORATOR)) {
            approvalService.createApprovalRequest(type, assetId, ApprovalType.ADD_COLLABORATOR, ownerId, ownerId, collaboratorId, reason);
        } else {
            assetMemberService.addMemberDirectly(type, assetId, ownerId, collaboratorId);
        }
    }

    /**
     * 移除协作者
     */
    public void removeCollaborator(ResourceType type, String assetId, Long ownerId, Long collaboratorId) {
        if (approvalService.requiresApproval(type, ApprovalType.REMOVE_COLLABORATOR)) {
            approvalService.createApprovalRequest(type, assetId, ApprovalType.REMOVE_COLLABORATOR, ownerId, ownerId, collaboratorId, "移除协作者");
        } else {
            assetMemberService.removeMemberDirectly(type, assetId, collaboratorId);
        }
    }
}