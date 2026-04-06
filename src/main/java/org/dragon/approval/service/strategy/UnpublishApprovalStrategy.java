package org.dragon.approval.service.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.approval.enums.ApprovalType;
import org.dragon.approval.service.ApprovalContext;
import org.dragon.approval.service.ApprovalStrategy;
import org.dragon.asset.service.AssetPublishStatusService;
import org.springframework.stereotype.Component;

/**
 * UnpublishApprovalStrategy 下架审批策略
 *
 * <p>处理资产下架审批，审批通过后回退发布状态为 DRAFT
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnpublishApprovalStrategy implements ApprovalStrategy {

    private final AssetPublishStatusService publishStatusService;

    @Override
    public ApprovalType getType() {
        return ApprovalType.UNPUBLISH;
    }

    @Override
    public void onApprove(ApprovalContext context) {
        log.info("[UnpublishApprovalStrategy] Approving unpublish: resourceType={}, resourceId={}, approverId={}",
                context.getRequest().getResourceType(),
                context.getRequest().getResourceId(),
                context.getApproverId());

        publishStatusService.revertToDraft(
                context.getRequest().getResourceType(),
                context.getRequest().getResourceId(),
                String.valueOf(context.getApproverId())
        );
    }

    @Override
    public void onReject(ApprovalContext context) {
        log.info("[UnpublishApprovalStrategy] Rejecting unpublish: resourceType={}, resourceId={}, approverId={}",
                context.getRequest().getResourceType(),
                context.getRequest().getResourceId(),
                context.getApproverId());
    }
}