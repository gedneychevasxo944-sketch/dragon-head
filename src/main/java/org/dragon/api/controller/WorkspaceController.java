package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.api.controller.dto.TeamPositionResponse;
import org.dragon.application.WorkspaceApiApplication;
import org.dragon.permission.checker.PermissionChecker;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.member.WorkspaceMember;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * WorkspaceController 工作空间模块 API
 *
 * <p>对应前端 /workspaces 页面，包含 Workspace 生命周期、成员、岗位、任务等接口。
 * Base URL: /api/v1/workspaces
 *
 * @author zhz
 * @version 1.0
 */
@Tag(name = "Workspace", description = "工作空间模块")
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceApiApplication workspaceApiApplication;
    private final PermissionChecker permissionChecker;

    // ==================== Workspace CRUD ====================

    /**
     * 创建 Workspace
     * POST /api/v1/workspaces
     */
    @Operation(summary = "创建 Workspace")
    @PostMapping
    public ApiResponse<Workspace> createWorkspace(@RequestBody Workspace workspace) {
        Workspace created = workspaceApiApplication.createWorkspace(workspace);
        return ApiResponse.success(created);
    }

    /**
     * 获取 Workspace 详情
     * GET /api/v1/workspaces/:id
     */
    @Operation(summary = "获取 Workspace 详情")
    @GetMapping("/{workspaceId}")
    public ApiResponse<Workspace> getWorkspace(@PathVariable String workspaceId) {
        permissionChecker.checkView("WORKSPACE", workspaceId);
        return workspaceApiApplication.getWorkspace(workspaceId)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Workspace not found: " + workspaceId));
    }

    /**
     * 更新 Workspace 设置
     * PUT /api/v1/workspaces/:id/settings
     */
    @Operation(summary = "更新 Workspace 设置")
    @PutMapping("/{workspaceId}/settings")
    public ApiResponse<Workspace> updateWorkspace(
            @PathVariable String workspaceId,
            @RequestBody Workspace workspace) {
        permissionChecker.checkEdit("WORKSPACE", workspaceId);
        Workspace updated = workspaceApiApplication.updateWorkspace(workspaceId, workspace);
        return ApiResponse.success(updated);
    }

    /**
     * 删除 Workspace
     * DELETE /api/v1/workspaces/:id
     */
    @Operation(summary = "删除 Workspace")
    @DeleteMapping("/{workspaceId}")
    public ApiResponse<Map<String, Object>> deleteWorkspace(@PathVariable String workspaceId) {
        permissionChecker.checkDelete("WORKSPACE", workspaceId);
        workspaceApiApplication.deleteWorkspace(workspaceId);
        return ApiResponse.success(Map.of("success", true));
    }

    // ==================== Member（团队成员）====================

    /**
     * 获取 Workspace 成员列表
     * GET /api/v1/workspaces/:workspaceId/members
     */
    @Operation(summary = "获取 Workspace AI 团队成员列表")
    @GetMapping("/{workspaceId}/members")
    public ApiResponse<List<WorkspaceMember>> listMembers(@PathVariable String workspaceId) {
        permissionChecker.checkView("WORKSPACE", workspaceId);
        List<WorkspaceMember> members = workspaceApiApplication.listMembers(workspaceId);
        return ApiResponse.success(members);
    }

    /**
     * 添加成员
     * POST /api/v1/workspaces/:workspaceId/members
     */
    @Operation(summary = "添加 Workspace 成员")
    @PostMapping("/{workspaceId}/members")
    public ApiResponse<WorkspaceMember> addMember(
            @PathVariable String workspaceId,
            @RequestBody AddMemberRequest request) {
        permissionChecker.checkManage("WORKSPACE", workspaceId);
        WorkspaceMember member = workspaceApiApplication.addMember(
                workspaceId,
                request.getCharacterId(),
                request.getRole(),
                request.getPosition(),
                request.getLevel(),
                request.getSourceType());
        return ApiResponse.success(member);
    }

    /**
     * 移除成员
     * DELETE /api/v1/workspaces/:workspaceId/members/:memberId
     */
    @Operation(summary = "移除 Workspace 成员")
    @DeleteMapping("/{workspaceId}/members/{memberId}")
    public ApiResponse<Map<String, Object>> removeMember(
            @PathVariable String workspaceId,
            @PathVariable String memberId) {
        permissionChecker.checkManage("WORKSPACE", workspaceId);
        workspaceApiApplication.removeMember(workspaceId, memberId);
        return ApiResponse.success(Map.of("success", true));
    }

    // ==================== Team Position（团队岗位）====================

    /**
     * 获取团队岗位列表
     * GET /api/v1/workspaces/:workspaceId/team-positions
     */
    @Operation(summary = "获取团队席位列表")
    @GetMapping("/{workspaceId}/team-positions")
    public ApiResponse<List<TeamPositionResponse>> listTeamPositions(@PathVariable String workspaceId) {
        List<TeamPositionResponse> positions = workspaceApiApplication.listTeamPositions(workspaceId);
        return ApiResponse.success(positions);
    }

    /**
     * 添加岗位
     * POST /api/v1/workspaces/:workspaceId/team-positions
     */
    @Operation(summary = "添加团队岗位")
    @PostMapping("/{workspaceId}/team-positions")
    public ApiResponse<TeamPositionResponse> addTeamPosition(
            @PathVariable String workspaceId,
            @RequestBody AddPositionRequest request) {
        permissionChecker.checkManage("WORKSPACE", workspaceId);
        TeamPositionResponse position = workspaceApiApplication.addTeamPosition(workspaceId, request);
        return ApiResponse.success(position);
    }

    /**
     * 更新岗位
     * PUT /api/v1/workspaces/:workspaceId/team-positions/:positionId
     */
    @Operation(summary = "更新团队岗位")
    @PutMapping("/{workspaceId}/team-positions/{positionId}")
    public ApiResponse<TeamPositionResponse> updateTeamPosition(
            @PathVariable String workspaceId,
            @PathVariable String positionId,
            @RequestBody UpdatePositionRequest request) {
        permissionChecker.checkManage("WORKSPACE", workspaceId);
        TeamPositionResponse position = workspaceApiApplication.updateTeamPosition(workspaceId, positionId, request);
        return ApiResponse.success(position);
    }

    /**
     * 删除岗位
     * DELETE /api/v1/workspaces/:workspaceId/team-positions/:positionId
     */
    @Operation(summary = "删除团队岗位")
    @DeleteMapping("/{workspaceId}/team-positions/{positionId}")
    public ApiResponse<Map<String, Object>> deleteTeamPosition(
            @PathVariable String workspaceId,
            @PathVariable String positionId) {
        permissionChecker.checkManage("WORKSPACE", workspaceId);
        workspaceApiApplication.deleteTeamPosition(workspaceId, positionId);
        return ApiResponse.success(Map.of("success", true));
    }

    // ==================== 请求体 DTO ====================

    /** 添加成员请求 */
    @Data
    public static class AddMemberRequest {
        private String characterId;
        private String role;
        private String position;
        private Integer level;
        private String sourceType;
    }

    /** 添加岗位请求 */
    @Data
    public static class AddPositionRequest {
        private String roleName;
        private String rolePackage;
        private String purpose;
        private String scope;
    }

    /** 更新岗位请求 */
    @Data
    public static class UpdatePositionRequest {
        private String assignedCharacterId;
        private String assignedBuiltinType;
        private Boolean enabled;
    }
}