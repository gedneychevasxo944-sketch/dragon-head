package org.dragon.approval.service.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.approval.enums.ApprovalType;
import org.dragon.approval.service.ApprovalContext;
import org.dragon.approval.service.ApprovalStrategy;
import org.dragon.asset.service.AssetPublishStatusService;
import org.springframework.stereotype.Component;

/**
 * PublishApprovalStrategy 发布审批策略
 *
 * <p>处理资产发布审批，审批通过后更新发布状态为 PUBLISHED
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublishApprovalStrategy implements ApprovalStrategy {

    private final AssetPublishStatusService publishStatusService;

    @Override
    public ApprovalType getType() {
        return ApprovalType.PUBLISH;
    }

    @Override
    public void onApprove(ApprovalContext context) {
        log.info("[PublishApprovalStrategy] Approving publish: resourceType={}, resourceId={}, approverId={}",
                context.getRequest().getResourceType(),
                context.getRequest().getResourceId(),
                context.getApproverId());

        publishStatusService.publish(
                context.getRequest().getResourceType(),
                context.getRequest().getResourceId(),
                String.valueOf(context.getApproverId()),
                null
        );
    }

    @Override
    public void onReject(ApprovalContext context) {
        log.info("[PublishApprovalStrategy] Rejecting publish: resourceType={}, resourceId={}, approverId={}",
                context.getRequest().getResourceType(),
                context.getRequest().getResourceId(),
                context.getApproverId());
    }
}