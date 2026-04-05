package org.dragon.permission.service;

import lombok.extern.slf4j.Slf4j;
import org.dragon.permission.dto.ApprovalRequestDTO;
import org.dragon.datasource.entity.ApprovalRequestEntity;
import org.dragon.permission.enums.ApprovalStatus;
import org.dragon.permission.enums.ApprovalType;
import org.dragon.permission.store.ApprovalStore;
import org.dragon.store.StoreFactory;
import org.dragon.permission.enums.ResourceType;
import org.dragon.user.store.UserStore;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ApprovalService 审批服务
 * 负责处理发布、取消发布、添加/移除协作者的审批流程
 */
@Slf4j
@Service
public class ApprovalService {

    private final ApprovalStore approvalStore;
    private final UserStore userStore;
    private final CollaboratorService collaboratorService;

    public ApprovalService(StoreFactory storeFactory, @Lazy CollaboratorService collaboratorService) {
        this.approvalStore = storeFactory.get(ApprovalStore.class);
        this.userStore = storeFactory.get(UserStore.class);
        this.collaboratorService = collaboratorService;
    }

    /**
     * 创建审批请求
     */
    public String createApprovalRequest(ResourceType type, String assetId, ApprovalType approvalType,
                                       Long requesterId, Long targetUserId, String reason) {
        if (approvalStore.existsPendingRequest(type, assetId, approvalType)) {
            throw new IllegalArgumentException("已存在待处理的审批请求");
        }

        String requesterName = userStore.findById(requesterId)
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                .orElse("Unknown");

        ApprovalRequestEntity request = ApprovalRequestEntity.builder()
                .id(UUID.randomUUID().toString())
                .resourceType(type)
                .resourceId(assetId)
                .approvalType(approvalType)
                .requesterId(requesterId)
                .requesterName(requesterName)
                .targetUserId(targetUserId)
                .reason(reason)
                .status(ApprovalStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        approvalStore.save(request);
        log.info("[ApprovalService] Created approval request: type={}, assetId={}, approvalType={}, requesterId={}",
                type, assetId, approvalType, requesterId);

        return request.getId();
    }

    /**
     * 审批通过
     */
    public void approve(String requestId, Long approverId, String comment) {
        ApprovalRequestEntity request = approvalStore.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("审批请求不存在"));

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalArgumentException("审批请求已被处理");
        }

        String approverName = userStore.findById(approverId)
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                .orElse("Unknown");

        request.setStatus(ApprovalStatus.APPROVED);
        request.setApproverId(approverId);
        request.setApproverName(approverName);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedComment(comment);
        approvalStore.update(request);

        executeAfterApproval(request);

        log.info("[ApprovalService] Approved request: id={}, approverId={}", requestId, approverId);
    }

    /**
     * 审批拒绝
     */
    public void reject(String requestId, Long approverId, String comment) {
        ApprovalRequestEntity request = approvalStore.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("审批请求不存在"));

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalArgumentException("审批请求已被处理");
        }

        String approverName = userStore.findById(approverId)
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                .orElse("Unknown");

        request.setStatus(ApprovalStatus.REJECTED);
        request.setApproverId(approverId);
        request.setApproverName(approverName);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedComment(comment);
        approvalStore.update(request);

        log.info("[ApprovalService] Rejected request: id={}, approverId={}", requestId, approverId);
    }

    /**
     * 获取用户发出的审批请求
     */
    public List<ApprovalRequestDTO> getRequestsByRequester(Long requesterId) {
        return approvalStore.findByRequester(requesterId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取需要审批人处理的审批请求
     */
    public List<ApprovalRequestDTO> getPendingApprovals(Long approverId) {
        return approvalStore.findPendingByApprover(approverId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取资源的审批历史
     */
    public List<ApprovalRequestDTO> getApprovalHistory(ResourceType type, String assetId) {
        return approvalStore.findByResource(type, assetId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 检查是否需要审批
     */
    public boolean requiresApproval(ResourceType type, ApprovalType approvalType) {
        switch (approvalType) {
            case PUBLISH:
                return type == ResourceType.SKILL || type == ResourceType.TEMPLATE;
            case UNPUBLISH:
                return type == ResourceType.TEMPLATE;
            case ADD_COLLABORATOR:
            case REMOVE_COLLABORATOR:
                return true;
            default:
                return false;
        }
    }

    /**
     * 获取待审批数量
     * 用于健康统计
     *
     * @return 待审批数量
     */
    public long countPendingApprovals() {
        return approvalStore.findAll().stream()
                .filter(r -> r.getStatus() == ApprovalStatus.PENDING)
                .count();
    }

    /**
     * 执行审批通过后的操作
     */
    private void executeAfterApproval(ApprovalRequestEntity request) {
        switch (request.getApprovalType()) {
            case ADD_COLLABORATOR:
                if (request.getTargetUserId() != null) {
                    collaboratorService.addMemberDirectly(
                            request.getResourceType(),
                            request.getResourceId(),
                            request.getRequesterId(),
                            request.getTargetUserId()
                    );
                }
                break;
            case REMOVE_COLLABORATOR:
                if (request.getTargetUserId() != null) {
                    collaboratorService.removeMemberDirectly(
                            request.getResourceType(),
                            request.getResourceId(),
                            request.getTargetUserId()
                    );
                }
                break;
            case PUBLISH:
            case UNPUBLISH:
                // TODO: 调用可见性服务修改发布状态
                log.info("[ApprovalService] Publish/Unpublish approved for: type={}, assetId={}",
                        request.getResourceType(), request.getResourceId());
                break;
            default:
                log.warn("[ApprovalService] Unknown approval type: {}", request.getApprovalType());
        }
    }

    private ApprovalRequestDTO toDTO(ApprovalRequestEntity entity) {
        String requesterName = userStore.findById(entity.getRequesterId())
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                .orElse("Unknown");

        String approverName = entity.getApproverId() != null
                ? userStore.findById(entity.getApproverId())
                        .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                        .orElse("Unknown")
                : null;

        String targetUserName = entity.getTargetUserId() != null
                ? userStore.findById(entity.getTargetUserId())
                        .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                        .orElse("Unknown")
                : null;

        return ApprovalRequestDTO.builder()
                .id(entity.getId())
                .resourceType(entity.getResourceType())
                .resourceId(entity.getResourceId())
                .approvalType(entity.getApprovalType())
                .requesterId(entity.getRequesterId())
                .requesterName(requesterName)
                .targetUserId(entity.getTargetUserId())
                .targetUserName(targetUserName)
                .approverId(entity.getApproverId())
                .approverName(approverName)
                .reason(entity.getReason())
                .status(entity.getStatus())
                .requestedAt(entity.getRequestedAt())
                .processedAt(entity.getProcessedAt())
                .processedComment(entity.getProcessedComment())
                .build();
    }
}
