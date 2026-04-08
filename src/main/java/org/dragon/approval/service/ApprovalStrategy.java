package org.dragon.approval.service;

import org.dragon.approval.enums.ApprovalType;

/**
 * ApprovalStrategy 审批策略接口
 *
 * <p>定义各种审批类型的处理策略，支持原子化和可扩展的审批流程
 */
public interface ApprovalStrategy {

    /**
     * 获取审批类型
     *
     * @return 审批类型
     */
    ApprovalType getType();

    /**
     * 执行审批通过后的业务操作
     *
     * @param context 审批上下文
     */
    void onApprove(ApprovalContext context);

    /**
     * 执行审批拒绝后的业务操作
     *
     * @param context 审批上下文
     */
    void onReject(ApprovalContext context);

    /**
     * 检查是否满足自动审批条件
     *
     * @param context 审批上下文
     * @return 是否可以自动审批
     */
    default boolean canAutoApprove(ApprovalContext context) {
        return false;
    }
}