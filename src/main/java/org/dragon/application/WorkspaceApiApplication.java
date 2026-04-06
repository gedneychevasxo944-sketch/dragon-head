package org.dragon.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.api.dto.PageResponse;
import org.dragon.observer.actionlog.ActionType;
import org.dragon.observer.actionlog.ObserverActionLog;
import org.dragon.permission.enums.ResourceType;
import org.dragon.permission.service.PermissionService;
import org.dragon.skill.dto.SkillBindingRequest;
import org.dragon.skill.dto.SkillBindingResult;
import org.dragon.skill.dto.SkillBindingVO;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.util.UserUtils;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.WorkspaceApplication;
import org.dragon.workspace.WorkspaceApplicationProvider;
import org.dragon.workspace.material.Material;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.service.lifecycle.WorkspaceLifecycleService;
import org.dragon.workspace.service.member.WorkspaceMemberManagementService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * WorkspaceApiApplication Workspace 模块 API 应用服务
 *
 * <p>对应前端 /workspaces 页面，聚合 Workspace 生命周期、成员、技能绑定、
 * 记忆配置、Observer 绑定、任务、审计日志、素材、权限等业务逻辑。
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceApiApplication {

    private final WorkspaceApplicationProvider workspaceApplicationProvider;
    private final WorkspaceLifecycleService workspaceLifecycleService;
    private final WorkspaceMemberManagementService memberManagementService;
    private final PermissionService permissionService;

    private WorkspaceApplication app(String workspaceId) {
        return workspaceApplicationProvider.getApplication(workspaceId);
    }

    // ==================== Workspace CRUD ====================

    /**
     * 分页获取 Workspace 列表。
     *
     * @param page        页码
     * @param pageSize    每页数量
     * @param search      搜索关键词
     * @param status      状态筛选
     * @param teamStatus  团队状态筛选 (complete/incomplete/not_initialized)
     * @param hasObserver 是否有 Observer
     * @return 分页结果
     */
    public PageResponse<Workspace> listWorkspaces(int page, int pageSize, String search,
                                                  String status, String teamStatus, Boolean hasObserver) {
        List<Workspace> all;
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            try {
                Workspace.Status st = Workspace.Status.valueOf(status.toUpperCase());
                all = workspaceLifecycleService.listWorkspacesByStatus(st);
            } catch (IllegalArgumentException e) {
                all = workspaceLifecycleService.listWorkspaces();
            }
        } else {
            all = workspaceLifecycleService.listWorkspaces();
        }

        // 按用户可见性过滤
        Long userId = Long.parseLong(UserUtils.getUserId());
        List<String> visibleIds = permissionService.getVisibleAssets(ResourceType.WORKSPACE, userId);

        List<Workspace> filtered = all.stream()
                .filter(w -> {
                    // search 筛选
                    if (search != null && !search.isBlank()) {
                        String s = search.toLowerCase();
                        boolean nameMatch = w.getName() != null && w.getName().toLowerCase().contains(s);
                        if (!nameMatch) return false;
                    }
                    // 可见性过滤
                    if (visibleIds != null && !visibleIds.isEmpty() && !visibleIds.contains(w.getId())) {
                        return false;
                    }
                    return true;
                })
                .toList();

        long total = filtered.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<Workspace> pageData = fromIndex >= filtered.size() ? List.of() : filtered.subList(fromIndex, toIndex);
        return PageResponse.of(pageData, total, page, pageSize);
    }

    /**
     * 创建 Workspace。
     *
     * @param workspace 工作空间
     * @return 创建后的 Workspace
     */
    public Workspace createWorkspace(Workspace workspace) {
        return app("default").createWorkspace(workspace);
    }

    /**
     * 获取 Workspace 详情。
     *
     * @param workspaceId Workspace ID
     * @return Workspace
     */
    public Optional<Workspace> getWorkspace(String workspaceId) {
        return app(workspaceId).getWorkspace(workspaceId);
    }

    /**
     * 更新 Workspace 设置。
     *
     * @param workspaceId Workspace ID
     * @param workspace   更新内容
     * @return 更新后的 Workspace
     */
    public Workspace updateWorkspace(String workspaceId, Workspace workspace) {
        workspace.setId(workspaceId);
        return app(workspaceId).updateWorkspace(workspace);
    }

    /**
     * 删除 Workspace。
     *
     * @param workspaceId Workspace ID
     */
    public void deleteWorkspace(String workspaceId) {
        app(workspaceId).deleteWorkspace(workspaceId);
    }

    /**
     * 激活 Workspace。
     */
    public void activateWorkspace(String workspaceId) {
        app(workspaceId).activateWorkspace(workspaceId);
    }

    /**
     * 停用 Workspace。
     */
    public void deactivateWorkspace(String workspaceId) {
        app(workspaceId).deactivateWorkspace(workspaceId);
    }

    /**
     * 归档 Workspace。
     */
    public void archiveWorkspace(String workspaceId) {
        app(workspaceId).archiveWorkspace(workspaceId);
    }

    // ==================== 成员管理 ====================

    /**
     * 获取成员列表。
     */
    public List<WorkspaceMember> listMembers(String workspaceId) {
        return memberManagementService.listMembers(workspaceId);
    }

    /**
     * 获取指定成员。
     */
    public Optional<WorkspaceMember> getMember(String workspaceId, String characterId) {
        return memberManagementService.getMember(workspaceId, characterId);
    }

    /**
     * 添加成员。
     */
    public WorkspaceMember addMember(String workspaceId, String characterId, String role,
                                     String position, Integer level, String sourceType) {
        return app(workspaceId).addMember(workspaceId, characterId, role);
    }

    /**
     * 移除成员。
     */
    public void removeMember(String workspaceId, String memberId) {
        memberManagementService.removeMember(workspaceId, memberId);
    }

    // ==================== 技能绑定 ====================

    /**
     * 获取已绑定技能列表。
     */
    public List<SkillBindingVO> listWorkspaceSkills(String workspaceId) {
        return app(workspaceId).listWorkspaceSkills(workspaceId);
    }

    /**
     * 绑定技能。
     */
    public SkillBindingResult bindSkill(String workspaceId, SkillBindingRequest request) {
        return app(workspaceId).bindSkill(workspaceId, request);
    }

    /**
     * 解绑技能。
     */
    public void unbindSkill(String workspaceId, String skillId) {
        app(workspaceId).unbindSkill(workspaceId, skillId);
    }

    // ==================== 记忆配置 ====================

    /**
     * 获取 Workspace 记忆配置信息（占位，Memory 系统需单独集成）。
     */
    public Map<String, Object> getMemoryInfo(String workspaceId) {
        Map<String, Object> info = new HashMap<>();
        info.put("backend", "builtin");
        info.put("provider", "default");
        info.put("model", "default");
        info.put("syncStrategy", "auto");
        info.put("dirtyFlag", false);
        info.put("ftsStatus", "enabled");
        info.put("vectorStatus", "ready");
        info.put("embeddingStatus", "ready");
        return info;
    }

    /**
     * 触发记忆同步（占位）。
     */
    public Map<String, Object> triggerMemorySync(String workspaceId) {
        return Map.of("success", true, "syncId", java.util.UUID.randomUUID().toString());
    }

    // ==================== Observer 绑定 ====================

    /**
     * 获取 Workspace Observer 信息（占位）。
     */
    public Map<String, Object> getObserverInfo(String workspaceId) {
        Map<String, Object> info = new HashMap<>();
        info.put("observerId", null);
        info.put("observerName", null);
        info.put("status", "unbound");
        info.put("evaluationMode", "auto");
        info.put("autoOptimization", false);
        info.put("pendingApprovalCount", 0);
        return info;
    }

    /**
     * 绑定 Observer（占位）。
     */
    public void bindObserver(String workspaceId, String observerId, String evaluationMode,
                             Boolean autoOptimization) {
        log.info("[WorkspaceApiApplication] Bind observer {} to workspace {}", observerId, workspaceId);
    }

    /**
     * 解绑 Observer（占位）。
     */
    public void unbindObserver(String workspaceId) {
        log.info("[WorkspaceApiApplication] Unbind observer from workspace {}", workspaceId);
    }

    // ==================== 任务管理 ====================

    /**
     * 获取任务列表。
     */
    public PageResponse<Task> listTasks(String workspaceId, String status, int page, int pageSize) {
        List<Task> all;
        if (status != null && !status.isBlank()) {
            try {
                TaskStatus ts = TaskStatus.valueOf(status.toUpperCase());
                all = app(workspaceId).getWorkspaceTaskService().listTasksByStatus(workspaceId, ts);
            } catch (Exception e) {
                all = app(workspaceId).listTasks(workspaceId);
            }
        } else {
            all = app(workspaceId).listTasks(workspaceId);
        }

        long total = all.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, all.size());
        List<Task> pageData = fromIndex >= all.size() ? List.of() : all.subList(fromIndex, toIndex);
        return PageResponse.of(pageData, total, page, pageSize);
    }

    /**
     * 获取任务详情。
     */
    public Optional<Task> getTask(String workspaceId, String taskId) {
        return app(workspaceId).getTask(workspaceId, taskId);
    }

    // ==================== 审计日志 ====================

    /**
     * 获取 Workspace 审计日志（Action Log）。
     */
    public List<ObserverActionLog> getAuditLogs(String workspaceId, String targetType,
                                                ActionType actionType) {
        if (targetType != null && !targetType.isBlank()) {
            return app(workspaceId).getWorkspaceActionLogService()
                    .getActionLogs(targetType.toUpperCase(), workspaceId);
        } else if (actionType != null) {
            return app(workspaceId).getWorkspaceActionLogService().getActionLogsByType(actionType);
        } else {
            return app(workspaceId).getActionLogs(workspaceId);
        }
    }

    // ==================== 素材管理 ====================

    /**
     * 获取素材列表。
     */
    public List<Material> listMaterials(String workspaceId) {
        return app(workspaceId).listMaterials(workspaceId);
    }

    /**
     * 上传素材。
     */
    public Material uploadMaterial(String workspaceId, MultipartFile file, String uploader) throws IOException {
        return app(workspaceId).uploadMaterial(
                workspaceId,
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType(),
                uploader);
    }

    /**
     * 获取素材元数据。
     */
    public Optional<Material> getMaterial(String workspaceId, String materialId) {
        return app(workspaceId).getMaterial(workspaceId, materialId);
    }

    /**
     * 下载素材。
     */
    public InputStream downloadMaterial(String workspaceId, String materialId) {
        return app(workspaceId).downloadMaterial(workspaceId, materialId);
    }

    /**
     * 删除素材。
     */
    public void deleteMaterial(String workspaceId, String materialId) {
        app(workspaceId).deleteMaterial(workspaceId, materialId);
    }

    // ==================== 权限管理（占位） ====================

    /**
     * 获取权限成员列表（占位，需要与 User 系统集成）。
     */
    public List<Map<String, Object>> listPermissions(String workspaceId) {
        return List.of();
    }

    /**
     * 添加成员权限（占位）。
     */
    public void addPermission(String workspaceId, String userId, String role) {
        log.info("[WorkspaceApiApplication] Add permission userId={} role={} workspace={}", userId, role, workspaceId);
    }

    /**
     * 更新成员权限（占位）。
     */
    public void updatePermission(String workspaceId, String userId, String role) {
        log.info("[WorkspaceApiApplication] Update permission userId={} role={} workspace={}", userId, role, workspaceId);
    }

    /**
     * 移除成员权限（占位）。
     */
    public void removePermission(String workspaceId, String userId) {
        log.info("[WorkspaceApiApplication] Remove permission userId={} workspace={}", userId, workspaceId);
    }
}
