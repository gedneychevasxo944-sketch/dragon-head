package org.dragon.approval.store;

import org.dragon.approval.enums.ApprovalStatus;
import org.dragon.approval.enums.ApprovalType;
import org.dragon.datasource.entity.ApprovalRequestEntity;
import org.dragon.permission.enums.ResourceType;
import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * ApprovalStore 审批请求存储接口
 */
public interface ApprovalStore extends Store {

    /**
     * 保存审批请求
     */
    void save(ApprovalRequestEntity request);

    /**
     * 更新审批请求
     */
    void update(ApprovalRequestEntity request);

    /**
     * 删除审批请求
     */
    void delete(String id);

    /**
     * 根据ID查找
     */
    Optional<ApprovalRequestEntity> findById(String id);

    /**
     * 根据资源查找审批历史
     */
    List<ApprovalRequestEntity> findByResource(ResourceType resourceType, String resourceId);

    /**
     * 根据审批类型和状态查找
     */
    List<ApprovalRequestEntity> findByTypeAndStatus(ApprovalType approvalType, ApprovalStatus status);

    /**
     * 根据审批人查找待审批请求
     */
    List<ApprovalRequestEntity> findPendingByApprover(Long approverId);

    /**
     * 根据请求人查找
     */
    List<ApprovalRequestEntity> findByRequester(Long requesterId);

    /**
     * 检查是否存在待处理的审批请求
     */
    boolean existsPendingRequest(ResourceType resourceType, String resourceId, ApprovalType approvalType);

    /**
     * 获取所有审批请求
     */
    List<ApprovalRequestEntity> findAll();
}