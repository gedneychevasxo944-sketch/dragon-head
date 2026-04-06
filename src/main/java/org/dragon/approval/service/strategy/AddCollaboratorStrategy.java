package org.dragon.approval.service.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.approval.enums.ApprovalType;
import org.dragon.approval.service.ApprovalContext;
import org.dragon.approval.service.ApprovalStrategy;
import org.dragon.permission.service.CollaboratorService;
import org.springframework.stereotype.Component;

/**
 * AddCollaboratorStrategy 添加协作者审批策略
 *
 * <p>处理添加协作者审批，审批通过后执行添加协作者操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AddCollaboratorStrategy implements ApprovalStrategy {

    private final CollaboratorService collaboratorService;

    @Override
    public ApprovalType getType() {
        return ApprovalType.ADD_COLLABORATOR;
    }

    @Override
    public void onApprove(ApprovalContext context) {
        log.info("[AddCollaboratorStrategy] Approving add collaborator: resourceType={}, resourceId={}, requesterId={}, targetUserId={}",
                context.getRequest().getResourceType(),
                context.getRequest().getResourceId(),
                context.getRequest().getRequesterId(),
                context.getRequest().getTargetUserId());

        if (context.getRequest().getTargetUserId() != null) {
            collaboratorService.addMemberDirectly(
                    context.getRequest().getResourceType(),
                    context.getRequest().getResourceId(),
                    context.getRequest().getRequesterId(),
                    context.getRequest().getTargetUserId()
            );
        }
    }

    @Override
    public void onReject(ApprovalContext context) {
        log.info("[AddCollaboratorStrategy] Rejecting add collaborator: resourceType={}, resourceId={}, approverId={}",
                context.getRequest().getResourceType(),
                context.getRequest().getResourceId(),
                context.getApproverId());
    }
}