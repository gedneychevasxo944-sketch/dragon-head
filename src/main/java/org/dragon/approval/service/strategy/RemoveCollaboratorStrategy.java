package org.dragon.approval.service.strategy;

import lombok.extern.slf4j.Slf4j;
import org.dragon.approval.enums.ApprovalType;
import org.dragon.approval.service.ApprovalContext;
import org.dragon.approval.service.ApprovalStrategy;
import org.dragon.asset.service.AssetMemberService;
import org.dragon.notification.service.NotificationService;
import org.dragon.permission.enums.ResourceType;
import org.springframework.stereotype.Component;

/**
 * RemoveCollaboratorStrategy 移除协作者审批策略
 *
 * <p>处理移除协作者审批，审批通过后执行移除协作者操作
 */
@Slf4j
@Component
public class RemoveCollaboratorStrategy implements ApprovalStrategy {

    private final AssetMemberService assetMemberService;
    private final NotificationService notificationService;

    public RemoveCollaboratorStrategy(AssetMemberService assetMemberService, NotificationService notificationService) {
        this.assetMemberService = assetMemberService;
        this.notificationService = notificationService;
    }

    @Override
    public ApprovalType getType() {
        return ApprovalType.REMOVE_COLLABORATOR;
    }

    @Override
    public void onApprove(ApprovalContext context) {
        Long targetUserId = context.getRequest().getTargetUserId();
        ResourceType resourceType = context.getRequest().getResourceType();
        String resourceId = context.getRequest().getResourceId();

        log.info("[RemoveCollaboratorStrategy] Approving remove collaborator: resourceType={}, resourceId={}, targetUserId={}",
                resourceType, resourceId, targetUserId);

        if (targetUserId != null) {
            // 检查不能移除 OWNER
            if (assetMemberService.isOwner(resourceType, resourceId, targetUserId)) {
                log.warn("[RemoveCollaboratorStrategy] Cannot remove OWNER: resourceType={}, resourceId={}, targetUserId={}",
                        resourceType, resourceId, targetUserId);
                throw new IllegalStateException("不能移除资产所有者（OWNER）");
            }

            // 通知被移除的协作者
            String resourceName = resourceType.name() + ":" + resourceId;
            notificationService.sendNotification(
                    targetUserId,
                    org.dragon.datasource.entity.NotificationType.SYSTEM,
                    "已被移除",
                    "您已被从资产 " + resourceName + " 中移除",
                    null,
                    resourceType.name(),
                    resourceId
            );

            assetMemberService.removeMemberDirectly(resourceType, resourceId, targetUserId);
        }
    }

    @Override
    public void onReject(ApprovalContext context) {
        log.info("[RemoveCollaboratorStrategy] Rejecting remove collaborator: resourceType={}, resourceId={}, approverId={}",
                context.getRequest().getResourceType(),
                context.getRequest().getResourceId(),
                context.getApproverId());
    }
}