package org.dragon.approval.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.datasource.entity.ApprovalRequestEntity;

/**
 * ApprovalContext 审批上下文
 *
 * <p>封装审批操作所需的上下文信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalContext {

    /**
     * 审批请求实体
     */
    private ApprovalRequestEntity request;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批意见/备注
     */
    private String comment;
}