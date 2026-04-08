package org.dragon.approval.service.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.approval.enums.ApprovalType;
import org.dragon.approval.service.ApprovalContext;
import org.dragon.approval.service.ApprovalStrategy;
import org.dragon.asset.service.AssetMemberService;
import org.springframework.stereotype.Component;

/**
 * RemoveCollaboratorStrategy 移除协作者审批策略
 *
 * <p>处理移除协作者审批，审批通过后执行移除协作者操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoveCollaboratorStrategy implements ApprovalStrategy {

    private final AssetMemberService assetMemberService;

    @Override
    public ApprovalType getType() {
        return ApprovalType.REMOVE_COLLABORATOR;
    }

    @Override
    public void onApprove(ApprovalContext context) {
        log.info("[RemoveCollaboratorStrategy] Approving remove collaborator: resourceType={}, resourceId={}, targetUserId={}",
                context.getRequest().getResourceType(),
                context.getRequest().getResourceId(),
                context.getRequest().getTargetUserId());

        if (context.getRequest().getTargetUserId() != null) {
            assetMemberService.removeMemberDirectly(
                    context.getRequest().getResourceType(),
                    context.getRequest().getResourceId(),
                    context.getRequest().getTargetUserId()
            );
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