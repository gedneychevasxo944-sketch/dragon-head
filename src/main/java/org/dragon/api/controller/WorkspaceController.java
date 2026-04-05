package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.application.WorkspaceApiApplication;
import org.dragon.api.dto.ApiResponse;
import org.dragon.api.dto.PageResponse;
import org.dragon.observer.actionlog.ActionType;
import org.dragon.observer.actionlog.ObserverActionLog;
import org.dragon.permission.checker.PermissionChecker;
import org.dragon.skill.dto.SkillBindingRequest;
import org.dragon.skill.dto.SkillBindingResponse;
import org.dragon.skill.dto.SkillBindingUpdateRequest;
import org.dragon.task.Task;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.member.WorkspaceMember;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * WorkspaceController 工作空间模块 API
 *
 * <p>对应前端 /workspaces 页面，包含 Workspace 生命周期、成员、技能绑定、
 * 记忆配置、Observer 绑定、任务、审计日志、素材、权限等接口。
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

    // ==================== 5. Workspace CRUD ====================

    /**
     * 5.1 获取 Workspace 列表
     * GET /api/v1/workspaces
     */
    @Operation(summary = "获取 Workspace 列表（分页+筛选）")
    @GetMapping
    public ApiResponse<PageResponse<Workspace>> listWorkspaces(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String teamStatus,
            @RequestParam(required = false) Boolean hasObserver) {
        PageResponse<Workspace> result = workspaceApiApplication.listWorkspaces(
                page, pageSize, search, status, teamStatus, hasObserver);
        return ApiResponse.success(result);
    }

    /**
     * 5.2 创建 Workspace
     * POST /api/v1/workspaces
     */
    @Operation(summary = "创建 Workspace")
    @PostMapping
    public ApiResponse<Workspace> createWorkspace(@RequestBody Workspace workspace) {
        Workspace created = workspaceApiApplication.createWorkspace(workspace);
        return ApiResponse.success(created);
    }

    /**
     * 5.3 获取 Workspace 详情
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
     * 5.4 更新 Workspace 设置
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
     * 5.5 删除 Workspace
     * DELETE /api/v1/workspaces/:id
     */
    @Operation(summary = "删除 Workspace")
    @DeleteMapping("/{workspaceId}")
    public ApiResponse<Map<String, Object>> deleteWorkspace(@PathVariable String workspaceId) {
        permissionChecker.checkDelete("WORKSPACE", workspaceId);
        workspaceApiApplication.deleteWorkspace(workspaceId);
        return ApiResponse.success(Map.of("success", true));
    }

    // ==================== 6. Member（团队成员）====================

    /**
     * 6.1 获取 Workspace 成员列表
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
     * 6.2 获取团队席位列表
     * GET /api/v1/workspaces/:workspaceId/team-positions
     */
    @Operation(summary = "获取团队席位列表")
    @GetMapping("/{workspaceId}/team-positions")
    public ApiResponse<List<Map<String, Object>>> listTeamPositions(@PathVariable String workspaceId) {
        // 占位：团队席位系统待实现
        return ApiResponse.success(List.of());
    }

    /**
     * 6.3 添加成员
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
     * 6.4 移除成员
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

    // ==================== 7. Skill Binding（技能绑定）====================

    /**
     * 7.1 获取已绑定技能列表
     * GET /api/v1/workspaces/:workspaceId/skills
     */
    @Operation(summary = "获取 Workspace 已绑定技能列表")
    @GetMapping("/{workspaceId}/skills")
    public ApiResponse<List<SkillBindingResponse>> listWorkspaceSkills(
            @PathVariable String workspaceId) {
        List<SkillBindingResponse> list = workspaceApiApplication.listWorkspaceSkills(workspaceId);
        return ApiResponse.success(list);
    }

    /**
     * 7.2 绑定技能
     * POST /api/v1/workspaces/:workspaceId/skills
     */
    @Operation(summary = "为 Workspace 绑定技能")
    @PostMapping("/{workspaceId}/skills")
    public ApiResponse<SkillBindingResponse> bindSkill(
            @PathVariable String workspaceId,
            @RequestBody SkillBindingRequest request) {
        SkillBindingResponse response = workspaceApiApplication.bindSkill(workspaceId, request);
        return ApiResponse.success(response);
    }

    /**
     * 7.3 解绑技能
     * DELETE /api/v1/workspaces/:workspaceId/skills/:skillId
     */
    @Operation(summary = "解绑 Workspace 技能")
    @DeleteMapping("/{workspaceId}/skills/{skillId}")
    public ApiResponse<Map<String, Object>> unbindSkill(
            @PathVariable String workspaceId,
            @PathVariable Long skillId) {
        workspaceApiApplication.unbindSkill(workspaceId, skillId);
        return ApiResponse.success(Map.of("success", true));
    }

    /**
     * 7.4 更新技能绑定配置
     * PUT /api/v1/workspaces/:workspaceId/skills/:skillId
     */
    @Operation(summary = "更新 Workspace 技能绑定配置")
    @PutMapping("/{workspaceId}/skills/{skillId}")
    public ApiResponse<SkillBindingResponse> updateSkillBinding(
            @PathVariable String workspaceId,
            @PathVariable Long skillId,
            @RequestBody SkillBindingUpdateRequest request) {
        SkillBindingResponse response = workspaceApiApplication.updateSkillBinding(workspaceId, skillId, request);
        return ApiResponse.success(response);
    }

    // ==================== 8. Memory（记忆配置）====================

    /**
     * 8.1 获取 Workspace 记忆配置信息
     * GET /api/v1/workspaces/:workspaceId/memory
     */
    @Operation(summary = "获取 Workspace 记忆配置信息")
    @GetMapping("/{workspaceId}/memory")
    public ApiResponse<Map<String, Object>> getMemoryInfo(@PathVariable String workspaceId) {
        Map<String, Object> info = workspaceApiApplication.getMemoryInfo(workspaceId);
        return ApiResponse.success(info);
    }

    /**
     * 8.2 触发记忆同步
     * POST /api/v1/workspaces/:workspaceId/memory/sync
     */
    @Operation(summary = "触发 Workspace 记忆同步")
    @PostMapping("/{workspaceId}/memory/sync")
    public ApiResponse<Map<String, Object>> triggerMemorySync(@PathVariable String workspaceId) {
        Map<String, Object> result = workspaceApiApplication.triggerMemorySync(workspaceId);
        return ApiResponse.success(result);
    }

    // ==================== 9. Observer（观测者绑定）====================

    /**
     * 9.1 获取 Workspace Observer 信息
     * GET /api/v1/workspaces/:workspaceId/observer
     */
    @Operation(summary = "获取 Workspace Observer 绑定信息")
    @GetMapping("/{workspaceId}/observer")
    public ApiResponse<Map<String, Object>> getObserverInfo(@PathVariable String workspaceId) {
        permissionChecker.checkView("WORKSPACE", workspaceId);
        Map<String, Object> info = workspaceApiApplication.getObserverInfo(workspaceId);
        return ApiResponse.success(info);
    }

    /**
     * 9.2 绑定 Observer
     * POST /api/v1/workspaces/:workspaceId/observer
     */
    @Operation(summary = "为 Workspace 绑定 Observer")
    @PostMapping("/{workspaceId}/observer")
    public ApiResponse<Map<String, Object>> bindObserver(
            @PathVariable String workspaceId,
            @RequestBody BindObserverRequest request) {
        permissionChecker.checkManage("WORKSPACE", workspaceId);
        workspaceApiApplication.bindObserver(workspaceId, request.getObserverId(),
                request.getEvaluationMode(), request.getAutoOptimization());
        return ApiResponse.success(Map.of("success", true));
    }

    /**
     * 9.3 解绑 Observer
     * DELETE /api/v1/workspaces/:workspaceId/observer
     */
    @Operation(summary = "解绑 Workspace Observer")
    @DeleteMapping("/{workspaceId}/observer")
    public ApiResponse<Map<String, Object>> unbindObserver(@PathVariable String workspaceId) {
        permissionChecker.checkManage("WORKSPACE", workspaceId);
        workspaceApiApplication.unbindObserver(workspaceId);
        return ApiResponse.success(Map.of("success", true));
    }

    // ==================== 10. Task（任务）====================

    /**
     * 10.1 获取 Workspace 任务列表
     * GET /api/v1/workspaces/:workspaceId/tasks
     */
    @Operation(summary = "获取 Workspace 任务列表")
    @GetMapping("/{workspaceId}/tasks")
    public ApiResponse<PageResponse<Task>> listTasks(
            @PathVariable String workspaceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<Task> result = workspaceApiApplication.listTasks(workspaceId, status, page, pageSize);
        return ApiResponse.success(result);
    }

    // ==================== 11. Audit Log（审计日志）====================

    /**
     * 11.1 获取 Workspace 审计日志
     * GET /api/v1/workspaces/:workspaceId/audit-logs
     */
    @Operation(summary = "获取 Workspace 审计日志")
    @GetMapping("/{workspaceId}/audit-logs")
    public ApiResponse<PageResponse<Map<String, Object>>> getAuditLogs(
            @PathVariable String workspaceId,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) ActionType actionType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        List<ObserverActionLog> logs = workspaceApiApplication.getAuditLogs(workspaceId, targetType, actionType);
        // 转换为 Map 格式并分页
        List<Map<String, Object>> logMaps = logs.stream()
                .map(l -> {
                    java.util.Map<String, Object> item = new java.util.HashMap<>();
                    item.put("id", l.getId());
                    item.put("targetType", l.getTargetType());
                    item.put("targetId", l.getTargetId());
                    item.put("actionType", l.getActionType() != null ? l.getActionType().name() : "");
                    item.put("operator", l.getOperator());
                    item.put("createdAt", l.getCreatedAt() != null ? l.getCreatedAt().toString() : "");
                    item.put("detailsSummary", l.getDetails() != null ? l.getDetails().toString() : "");
                    return item;
                })
                .collect(java.util.stream.Collectors.toList());

        long total = logMaps.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, logMaps.size());
        List<Map<String, Object>> pageData = fromIndex >= logMaps.size() ? List.of() : logMaps.subList(fromIndex, toIndex);
        return ApiResponse.success(PageResponse.of(pageData, total, page, pageSize));
    }

    // ==================== 12. Material（素材）====================

    /**
     * 12.1 获取素材列表
     * GET /api/v1/workspaces/:workspaceId/materials
     */
    @Operation(summary = "获取 Workspace 素材列表")
    @GetMapping("/{workspaceId}/materials")
    public ApiResponse<List<Map<String, Object>>> listMaterials(@PathVariable String workspaceId) {
        List<org.dragon.workspace.material.Material> materials = workspaceApiApplication.listMaterials(workspaceId);
        List<Map<String, Object>> result = materials.stream()
                .map(m -> {
                    java.util.Map<String, Object> item = new java.util.HashMap<>();
                    item.put("materialId", m.getId());
                    item.put("filename", m.getName());
                    item.put("type", m.getType() != null ? m.getType() : "other");
                    item.put("size", m.getSize());
                    item.put("uploader", m.getUploader());
                    item.put("uploadTime", m.getUploadedAt() != null ? m.getUploadedAt().toString() : "");
                    return item;
                })
                .collect(java.util.stream.Collectors.toList());
        return ApiResponse.success(result);
    }

    /**
     * 12.2 上传素材
     * POST /api/v1/workspaces/:workspaceId/materials (multipart/form-data)
     */
    @Operation(summary = "上传 Workspace 素材")
    @PostMapping(value = "/{workspaceId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> uploadMaterial(
            @PathVariable String workspaceId,
            @RequestParam("file") MultipartFile file) throws IOException {
        org.dragon.workspace.material.Material material = workspaceApiApplication.uploadMaterial(
                workspaceId, file, "api-user");
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("materialId", material.getId());
        result.put("filename", material.getName());
        result.put("type", material.getType() != null ? material.getType() : "other");
        result.put("size", material.getSize());
        result.put("uploader", material.getUploader());
        result.put("uploadTime", material.getUploadedAt() != null ? material.getUploadedAt().toString() : "");
        return ApiResponse.success(result);
    }

    /**
     * 12.3 删除素材
     * DELETE /api/v1/workspaces/:workspaceId/materials/:materialId
     */
    @Operation(summary = "删除 Workspace 素材")
    @DeleteMapping("/{workspaceId}/materials/{materialId}")
    public ApiResponse<Map<String, Object>> deleteMaterial(
            @PathVariable String workspaceId,
            @PathVariable String materialId) {
        workspaceApiApplication.deleteMaterial(workspaceId, materialId);
        return ApiResponse.success(Map.of("success", true));
    }

    // ==================== 13. Permission（权限）====================

    /**
     * 13.1 获取权限成员列表
     * GET /api/v1/workspaces/:workspaceId/permissions
     */
    @Operation(summary = "获取 Workspace 权限成员列表")
    @GetMapping("/{workspaceId}/permissions")
    public ApiResponse<List<Map<String, Object>>> listPermissions(@PathVariable String workspaceId) {
        permissionChecker.checkManage("WORKSPACE", workspaceId);
        List<Map<String, Object>> permissions = workspaceApiApplication.listPermissions(workspaceId);
        return ApiResponse.success(permissions);
    }

    /**
     * 13.2 添加成员权限
     * POST /api/v1/workspaces/:workspaceId/permissions
     */
    @Operation(summary = "添加 Workspace 成员权限")
    @PostMapping("/{workspaceId}/permissions")
    public ApiResponse<Map<String, Object>> addPermission(
            @PathVariable String workspaceId,
            @RequestBody PermissionRequest request) {
        permissionChecker.checkManage("WORKSPACE", workspaceId);
        workspaceApiApplication.addPermission(workspaceId, request.getUserId(), request.getRole());
        return ApiResponse.success(Map.of("success", true));
    }

    /**
     * 13.3 更新成员权限
     * PUT /api/v1/workspaces/:workspaceId/permissions/:userId
     */
    @Operation(summary = "更新 Workspace 成员权限")
    @PutMapping("/{workspaceId}/permissions/{userId}")
    public ApiResponse<Map<String, Object>> updatePermission(
            @PathVariable String workspaceId,
            @PathVariable String userId,
            @RequestBody PermissionRequest request) {
        permissionChecker.checkManage("WORKSPACE", workspaceId);
        workspaceApiApplication.updatePermission(workspaceId, userId, request.getRole());
        return ApiResponse.success(Map.of("success", true));
    }

    /**
     * 13.4 移除成员权限
     * DELETE /api/v1/workspaces/:workspaceId/permissions/:userId
     */
    @Operation(summary = "移除 Workspace 成员权限")
    @DeleteMapping("/{workspaceId}/permissions/{userId}")
    public ApiResponse<Map<String, Object>> removePermission(
            @PathVariable String workspaceId,
            @PathVariable String userId) {
        permissionChecker.checkManage("WORKSPACE", workspaceId);
        workspaceApiApplication.removePermission(workspaceId, userId);
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

    /** 绑定 Observer 请求 */
    @Data
    public static class BindObserverRequest {
        private String observerId;
        private String evaluationMode;
        private Boolean autoOptimization;
    }

    /** 权限请求 */
    @Data
    public static class PermissionRequest {
        private String userId;
        private String role;
    }
}