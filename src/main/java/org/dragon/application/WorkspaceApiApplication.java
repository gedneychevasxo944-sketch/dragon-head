package org.dragon.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.dragon.api.controller.dto.PageResponse;
import org.dragon.api.controller.dto.TeamPositionResponse;
import org.dragon.workspace.skill.WorkspaceSkill;
import org.dragon.api.controller.WorkspaceController.AddPositionRequest;
import org.dragon.api.controller.WorkspaceController.UpdatePositionRequest;
import org.dragon.task.Task;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.WorkspaceFacadeService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * WorkspaceApiApplication Workspace 模块 API 应用服务
 *
 * <p>对应前端 /workspaces 页面，作为 facade 委托 WorkspaceService 处理业务逻辑。
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceApiApplication {

    private final WorkspaceFacadeService workspaceService;

    // ==================== Workspace CRUD ====================

    /**
     * 分页获取 Workspace 列表。
     */
    public PageResponse<Workspace> listWorkspaces(int page, int pageSize, String search,
                                                  String status, String teamStatus, Boolean hasObserver) {
        return workspaceService.listWorkspaces(page, pageSize, search, status, teamStatus, hasObserver);
    }

    /**
     * 创建 Workspace。
     */
    public Workspace createWorkspace(Workspace workspace) {
        return workspaceService.createWorkspace(workspace);
    }

    /**
     * 获取 Workspace 详情。
     */
    public Optional<Workspace> getWorkspace(String workspaceId) {
        return workspaceService.getWorkspace(workspaceId);
    }

    /**
     * 更新 Workspace 设置。
     */
    public Workspace updateWorkspace(String workspaceId, Workspace workspace) {
        return workspaceService.updateWorkspace(workspaceId, workspace);
    }

    /**
     * 删除 Workspace。
     */
    public void deleteWorkspace(String workspaceId) {
        workspaceService.deleteWorkspace(workspaceId);
    }

    /**
     * 激活 Workspace。
     */
    public void activateWorkspace(String workspaceId) {
        workspaceService.activateWorkspace(workspaceId);
    }

    /**
     * 停用 Workspace。
     */
    public void deactivateWorkspace(String workspaceId) {
        workspaceService.deactivateWorkspace(workspaceId);
    }

    /**
     * 归档 Workspace。
     */
    public void archiveWorkspace(String workspaceId) {
        workspaceService.archiveWorkspace(workspaceId);
    }

    // ==================== 成员管理 ====================

    /**
     * 获取成员列表。
     */
    public List<WorkspaceMember> listMembers(String workspaceId) {
        return workspaceService.listMembers(workspaceId);
    }

    /**
     * 获取指定成员。
     */
    public Optional<WorkspaceMember> getMember(String workspaceId, String characterId) {
        return workspaceService.getMember(workspaceId, characterId);
    }

    /**
     * 添加成员。
     */
    public WorkspaceMember addMember(String workspaceId, String characterId, String role,
                                     String position, Integer level, String sourceType) {
        return workspaceService.addMember(workspaceId, characterId, role, position, level, sourceType);
    }

    /**
     * 移除成员。
     */
    public void removeMember(String workspaceId, String memberId) {
        workspaceService.removeMember(workspaceId, memberId);
    }

    // ==================== 技能管理 ====================

    /**
     * 获取 workspace 已圈选的 Skill 列表（含元信息及启用状态）。
     */
    public List<WorkspaceSkill> listWorkspaceSkillDetails(String workspaceId) {
        return workspaceService.listWorkspaceSkillDetails(workspaceId);
    }

    // ==================== 岗位管理 ====================

    /**
     * 获取岗位列表。
     */
    public List<TeamPositionResponse> listTeamPositions(String workspaceId) {
        return workspaceService.listTeamPositions(workspaceId);
    }

    /**
     * 添加岗位。
     */
    public TeamPositionResponse addTeamPosition(String workspaceId, AddPositionRequest request) {
        return workspaceService.addTeamPosition(workspaceId,
                request.getRoleName(), request.getRolePackage(), request.getPurpose(), request.getScope());
    }

    /**
     * 更新岗位。
     */
    public TeamPositionResponse updateTeamPosition(String workspaceId, String positionId,
            UpdatePositionRequest request) {
        return workspaceService.updateTeamPosition(workspaceId, positionId,
                request.getAssignedCharacterId(), request.getAssignedBuiltinType(), request.getEnabled());
    }

    /**
     * 删除岗位。
     */
    public void deleteTeamPosition(String workspaceId, String positionId) {
        workspaceService.deleteTeamPosition(workspaceId, positionId);
    }

    // ==================== 任务管理 ====================

    /**
     * 获取任务列表。
     */
    public PageResponse<Task> listTasks(String workspaceId, String status, int page, int pageSize) {
        return workspaceService.listTasks(workspaceId, status, page, pageSize);
    }

    /**
     * 获取任务详情。
     */
    public Optional<Task> getTask(String workspaceId, String taskId) {
        return workspaceService.getTask(workspaceId, taskId);
    }
}
