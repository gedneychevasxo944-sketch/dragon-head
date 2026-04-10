package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.asset.dto.AssetMemberDTO;
import org.dragon.asset.dto.CollaboratorDTO;
import org.dragon.asset.service.AssetMemberService;
import org.dragon.asset.service.CollaboratorService;
import org.dragon.permission.dto.InvitationDTO;
import org.dragon.permission.enums.ResourceType;
import org.dragon.user.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * CollaboratorController 协作者管理 API
 *
 * <p>提供协作者邀请、接受、拒绝、移除等接口。
 * Base URL: /api/v1/collaborators
 */
@Tag(name = "Collaborator", description = "协作者管理")
@RestController
@RequestMapping("/api/v1/collaborators")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CollaboratorController {

    private final CollaboratorService collaboratorService;
    private final AssetMemberService assetMemberService;

    /**
     * 获取我收到的邀请列表
     * GET /api/v1/collaborators/invitations
     */
    @Operation(summary = "获取我收到的邀请列表")
    @GetMapping("/invitations")
    public ApiResponse<List<InvitationDTO>> getMyInvitations(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<InvitationDTO> invitations = assetMemberService.getPendingInvitations(principal.getUserId());
        return ApiResponse.success(invitations);
    }

    /**
     * 获取我的资产列表
     * GET /api/v1/collaborators/my-assets
     */
    @Operation(summary = "获取我的资产列表")
    @GetMapping("/my-assets")
    public ApiResponse<List<AssetMemberDTO>> getMyAssets(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<AssetMemberDTO> assets = assetMemberService.getMyAssets(principal.getUserId());
        return ApiResponse.success(assets);
    }

    /**
     * 接受邀请
     * POST /api/v1/collaborators/invitations/{id}/accept
     */
    @Operation(summary = "接受邀请")
    @PostMapping("/invitations/{id}/accept")
    public ApiResponse<Map<String, Object>> acceptInvitation(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        // ID format: resourceType_resourceId_userId
        String[] parts = id.split("_");
        if (parts.length != 3) {
            return ApiResponse.error(400, "无效的邀请ID格式");
        }
        assetMemberService.acceptInvitation(principal.getUserId(),
                ResourceType.valueOf(parts[0]), parts[1] + "_" + parts[2]);
        return ApiResponse.success(Map.of("success", true, "message", "邀请已接受"));
    }

    /**
     * 拒绝邀请
     * POST /api/v1/collaborators/invitations/{id}/reject
     */
    @Operation(summary = "拒绝邀请")
    @PostMapping("/invitations/{id}/reject")
    public ApiResponse<Map<String, Object>> rejectInvitation(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        // ID format: resourceType_resourceId_userId
        String[] parts = id.split("_");
        if (parts.length != 3) {
            return ApiResponse.error(400, "无效的邀请ID格式");
        }
        assetMemberService.rejectInvitation(principal.getUserId(),
                ResourceType.valueOf(parts[0]), parts[1] + "_" + parts[2]);
        return ApiResponse.success(Map.of("success", true, "message", "邀请已拒绝"));
    }

    /**
     * 获取资产的协作者列表
     * GET /api/v1/assets/{type}/{id}/collaborators
     */
    @Operation(summary = "获取资产的协作者列表")
    @GetMapping("/assets/{type}/{id}/collaborators")
    public ApiResponse<List<CollaboratorDTO>> getCollaborators(
            @PathVariable String type,
            @PathVariable String id) {
        ResourceType resourceType = ResourceType.valueOf(type.toUpperCase());
        List<CollaboratorDTO> collaborators = assetMemberService.getCollaborators(resourceType, id);
        return ApiResponse.success(collaborators);
    }

    /**
     * 邀请协作者
     * POST /api/v1/assets/{type}/{id}/collaborators
     */
    @Operation(summary = "邀请协作者")
    @PostMapping("/assets/{type}/{id}/collaborators")
    public ApiResponse<Map<String, Object>> inviteCollaborator(
            @PathVariable String type,
            @PathVariable String id,
            @RequestBody CollaboratorInviteRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ResourceType resourceType = ResourceType.valueOf(type.toUpperCase());
        String reason = request.getReason() != null ? request.getReason() : "邀请用户";
        collaboratorService.inviteCollaborator(resourceType, id, principal.getUserId(), request.getCollaboratorId(), reason);
        return ApiResponse.success(Map.of("success", true, "message", "协作者邀请已发送"));
    }

    /**
     * 移除协作者
     * DELETE /api/v1/assets/{type}/{id}/collaborators/{collaboratorId}
     */
    @Operation(summary = "移除协作者")
    @DeleteMapping("/assets/{type}/{id}/collaborators/{collaboratorId}")
    public ApiResponse<Map<String, Object>> removeCollaborator(
            @PathVariable String type,
            @PathVariable String id,
            @PathVariable Long collaboratorId,
            @AuthenticationPrincipal UserPrincipal principal) {
        ResourceType resourceType = ResourceType.valueOf(type.toUpperCase());
        collaboratorService.removeCollaborator(resourceType, id, principal.getUserId(), collaboratorId);
        return ApiResponse.success(Map.of("success", true, "message", "协作者已移除"));
    }

    /**
     * 邀请请求
     */
    public static class CollaboratorInviteRequest {
        private Long collaboratorId;
        private String reason;

        public Long getCollaboratorId() {
            return collaboratorId;
        }

        public void setCollaboratorId(Long collaboratorId) {
            this.collaboratorId = collaboratorId;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}