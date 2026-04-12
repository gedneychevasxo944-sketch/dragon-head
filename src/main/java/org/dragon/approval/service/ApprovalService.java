package org.dragon.approval.service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dragon.approval.dto.ApprovalRequestDTO;
import org.dragon.approval.enums.ApprovalStatus;
import org.dragon.approval.enums.ApprovalType;
import org.dragon.approval.store.ApprovalStore;
import org.dragon.datasource.entity.ApprovalRequestEntity;
import org.dragon.permission.enums.ResourceType;
import org.dragon.store.StoreFactory;
import org.dragon.user.store.UserStore;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * ApprovalService 审批服务
 *
 * <p>负责处理发布、取消发布、添加/移除协作者的审批流程
 * <p>使用策略模式处理不同审批类型的业务逻辑
 */
@Slf4j
@Service
public class ApprovalService {

    private final ApprovalStore approvalStore;
    private final UserStore userStore;
    private final Map<ApprovalType, ApprovalStrategy> strategies;

    /**
     * 需要审批的资源类型映射
     */
    private static final Map<ApprovalType, List<ResourceType>> APPROVAL_REQUIRED_TYPES = new EnumMap<>(ApprovalType.class);

    static {
        APPROVAL_REQUIRED_TYPES.put(ApprovalType.PUBLISH,
                List.of(ResourceType.CHARACTER, ResourceType.SKILL, ResourceType.OBSERVER,
                        ResourceType.TEMPLATE, ResourceType.TRAIT, ResourceType.WORKSPACE,
                        ResourceType.COMMONSENSE));
        APPROVAL_REQUIRED_TYPES.put(ApprovalType.UNPUBLISH, List.of(ResourceType.TEMPLATE));
        APPROVAL_REQUIRED_TYPES.put(ApprovalType.ADD_COLLABORATOR, null); // 所有资源类型
        APPROVAL_REQUIRED_TYPES.put(ApprovalType.REMOVE_COLLABORATOR, null); // 所有资源类型
    }

    public ApprovalService(StoreFactory storeFactory, @Lazy UserStore userStore,
                           List<ApprovalStrategy> strategyList) {
        this.approvalStore = storeFactory.get(ApprovalStore.class);
        this.userStore = userStore;

        // 初始化策略映射
        this.strategies = new EnumMap<>(ApprovalType.class);
        for (ApprovalStrategy strategy : strategyList) {
            this.strategies.put(strategy.getType(), strategy);
        }
    }

    /**
     * 创建审批请求
     */
    public String createApprovalRequest(ResourceType type, String assetId, ApprovalType approvalType,
                                       Long requesterId, Long approverId, String reason) {
        return createApprovalRequest(type, assetId, approvalType, requesterId, approverId, approverId, reason);
    }

    /**
     * 创建审批请求（带指定审批人）
     */
    public String createApprovalRequest(ResourceType type, String assetId, ApprovalType approvalType,
                                       Long requesterId, Long approverId, Long targetUserId, String reason) {
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
                .approverId(approverId)
                .approverId(targetUserId)
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

        // 执行审批通过后的业务操作（策略模式）
        executeStrategy(request, approverId, comment, true);

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

        // 执行审批拒绝后的业务操作（策略模式）
        executeStrategy(request, approverId, comment, false);

        log.info("[ApprovalService] Rejected request: id={}, approverId={}", requestId, approverId);
    }

    /**
     * 执行策略
     */
    private void executeStrategy(ApprovalRequestEntity request, Long approverId, String comment, boolean approved) {
        ApprovalStrategy strategy = strategies.get(request.getApprovalType());
        if (strategy == null) {
            log.warn("[ApprovalService] No strategy found for approval type: {}", request.getApprovalType());
            return;
        }

        ApprovalContext context = ApprovalContext.builder()
                .request(request)
                .approverId(approverId)
                .comment(comment)
                .build();

        if (approved) {
            strategy.onApprove(context);
        } else {
            strategy.onReject(context);
        }
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
     * 获取所有待审批请求（system管理员专用）
     */
    public List<ApprovalRequestDTO> getAllPendingApprovals() {
        return approvalStore.findAllPending().stream()
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
        List<ResourceType> requiredTypes = APPROVAL_REQUIRED_TYPES.get(approvalType);
        if (requiredTypes == null) {
            return true; // null 表示所有资源类型都需要审批
        }
        return requiredTypes.contains(type);
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