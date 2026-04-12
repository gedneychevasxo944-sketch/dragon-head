package org.dragon.api.controller;

import java.util.List;
import java.util.Map;

import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.approval.dto.ApprovalRequestDTO;
import org.dragon.approval.enums.ApprovalType;
import org.dragon.approval.service.ApprovalService;
import org.dragon.permission.enums.ResourceType;
import org.dragon.user.security.UserPrincipal;
import org.dragon.user.store.UserStore;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * ApprovalController 审批管理 API
 *
 * <p>提供审批请求的查询、批准、拒绝等接口。
 * Base URL: /api/v1/approvals
 */
@Tag(name = "Approval", description = "审批管理")
@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ApprovalController {

    private final ApprovalService approvalService;
    private final UserStore userStore;

    private static final String SYSTEM_USERNAME = "system";

    /**
     * 获取我发出的审批请求
     * GET /api/v1/approvals/my-requests
     */
    @Operation(summary = "获取我发出的审批请求")
    @GetMapping("/my-requests")
    public ApiResponse<List<ApprovalRequestDTO>> getMyRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ApprovalRequestDTO> requests = approvalService.getRequestsByRequester(principal.getUserId());
        return ApiResponse.success(requests);
    }

    /**
     * 获取需要我审批的待处理请求
     * GET /api/v1/approvals/pending
     * <p>system 用户会看到所有待审批请求，其他用户只看分配给自己的
     */
    @Operation(summary = "获取需要我审批的待处理请求")
    @GetMapping("/pending")
    public ApiResponse<List<ApprovalRequestDTO>> getPendingApprovals(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ApprovalRequestDTO> requests;
        if (SYSTEM_USERNAME.equals(principal.getUsername())) {
            // system 用户获取所有待审批请求
            requests = approvalService.getAllPendingApprovals();
        } else {
            requests = approvalService.getPendingApprovals(principal.getUserId());
        }
        return ApiResponse.success(requests);
    }

    /**
     * 获取资产的审批历史
     * GET /api/v1/approvals/assets/{type}/{id}
     */
    @Operation(summary = "获取资产的审批历史")
    @GetMapping("/assets/{type}/{id}")
    public ApiResponse<List<ApprovalRequestDTO>> getApprovalHistory(
            @PathVariable String type,
            @PathVariable String id) {
        ResourceType resourceType = ResourceType.valueOf(type.toUpperCase());
        List<ApprovalRequestDTO> history = approvalService.getApprovalHistory(resourceType, id);
        return ApiResponse.success(history);
    }

    /**
     * 审批通过
     * POST /api/v1/approvals/{id}/approve
     */
    @Operation(summary = "审批通过")
    @PostMapping("/{id}/approve")
    public ApiResponse<Map<String, Object>> approve(
            @PathVariable String id,
            @RequestBody(required = false) ApprovalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String comment = request != null ? request.getComment() : null;
        approvalService.approve(id, principal.getUserId(), comment);
        return ApiResponse.success(Map.of("success", true, "message", "审批已通过"));
    }

    /**
     * 审批拒绝
     * POST /api/v1/approvals/{id}/reject
     */
    @Operation(summary = "审批拒绝")
    @PostMapping("/{id}/reject")
    public ApiResponse<Map<String, Object>> reject(
            @PathVariable String id,
            @RequestBody(required = false) ApprovalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String comment = request != null ? request.getComment() : null;
        approvalService.reject(id, principal.getUserId(), comment);
        return ApiResponse.success(Map.of("success", true, "message", "审批已拒绝"));
    }

    /**
     * 创建发布审批申请
     * POST /api/v1/approvals/publish
     */
    @Operation(summary = "创建发布审批申请")
    @PostMapping("/publish")
    public ApiResponse<Map<String, Object>> createPublishRequest(
            @RequestBody CreatePublishRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        // 获取 system 用户 ID 作为审批人
        Long systemUserId = userStore.findByUsername(SYSTEM_USERNAME)
                .map(u -> u.getId())
                .orElseThrow(() -> new IllegalStateException("system 用户不存在，请确保 V2 migration 已执行"));

        String requestId = approvalService.createApprovalRequest(
                request.getResourceType(),
                request.getResourceId(),
                ApprovalType.PUBLISH,
                principal.getUserId(),
                systemUserId,
                request.getReason());
        return ApiResponse.success(Map.of("requestId", requestId, "success", true, "message", "发布申请已提交"));
    }

    /**
     * 获取所有待审批请求（system管理员专用）
     * GET /api/v1/approvals/system-pending
     */
    @Operation(summary = "获取所有待审批请求（system管理员专用）")
    @GetMapping("/system-pending")
    public ApiResponse<List<ApprovalRequestDTO>> getSystemPendingApprovals(
            @AuthenticationPrincipal UserPrincipal principal) {
        // 验证调用者是 system 用户
        if (!SYSTEM_USERNAME.equals(principal.getUsername())) {
            return ApiResponse.error(403, "仅允许 system 账号访问");
        }
        Long systemUserId = userStore.findByUsername(SYSTEM_USERNAME)
            .map(u -> u.getId())
            .orElseThrow(() -> new IllegalStateException("system 用户不存在，请确保 V2 migration 已执行"));

        List<ApprovalRequestDTO> requests = approvalService.getPendingApprovals(systemUserId);
        return ApiResponse.success(requests);
    }

    /**
     * 审批请求
     */
    public static class ApprovalRequest {
        private String comment;

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }

    /**
     * 创建发布申请请求
     */
    public static class CreatePublishRequest {
        private ResourceType resourceType;
        private String resourceId;
        private String reason;

        public ResourceType getResourceType() {
            return resourceType;
        }

        public void setResourceType(ResourceType resourceType) {
            this.resourceType = resourceType;
        }

        public String getResourceId() {
            return resourceId;
        }

        public void setResourceId(String resourceId) {
            this.resourceId = resourceId;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}