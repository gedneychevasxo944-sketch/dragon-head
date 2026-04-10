package org.dragon.workspace.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.api.controller.dto.TeamPositionResponse;
import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.permission.enums.ResourceType;
import org.dragon.permission.service.PermissionService;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.util.UserUtils;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.member.TeamPosition;
import org.dragon.workspace.service.lifecycle.WorkspaceLifecycleService;
import org.dragon.workspace.service.member.WorkspaceMemberManagementService;
import org.dragon.workspace.service.teampositions.WorkspaceTeamPositionsManagementService;
import org.dragon.workspace.service.task.arrangement.WorkspaceTaskArrangementService;
import org.dragon.workspace.service.task.arrangement.WorkspaceTaskArrangementService.TaskExecutionMode;
import org.dragon.workspace.service.task.arrangement.dto.TaskCreationCommand;
import org.dragon.workspace.service.task.execution.WorkspaceTaskExecutionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * WorkspaceService Workspace 领域服务
 *
 * <p>封装 Workspace 生命周期、成员、岗位、任务等核心业务逻辑。
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceLifecycleService workspaceLifecycleService;
    private final WorkspaceMemberManagementService memberManagementService;
    private final WorkspaceTeamPositionsManagementService teamPositionsManagementService;
    private final PermissionService permissionService;
    private final DeploymentService deploymentService;
    private final WorkspacePluginService workspacePluginService;
    private final WorkspaceTaskService workspaceTaskService;
    private final WorkspaceTaskArrangementService workspaceTaskArrangementService;
    private final TaskStore taskStore;
    private final TaskContinuationResolver taskContinuationResolver;
    private final TaskResumeTargetResolver taskResumeTargetResolver;
    private final WorkspaceTaskExecutionService taskExecutionService;

    // ==================== Workspace CRUD ====================

    /**
     * 分页获取 Workspace 列表。
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
                    if (search != null && !search.isBlank()) {
                        String s = search.toLowerCase();
                        boolean nameMatch = w.getName() != null && w.getName().toLowerCase().contains(s);
                        if (!nameMatch) return false;
                    }
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
     */
    public Workspace createWorkspace(Workspace workspace) {
        return workspaceLifecycleService.createWorkspace(workspace);
    }

    /**
     * 获取 Workspace 详情。
     */
    public Optional<Workspace> getWorkspace(String workspaceId) {
        return workspaceLifecycleService.getWorkspace(workspaceId);
    }

    /**
     * 更新 Workspace 设置。
     */
    public Workspace updateWorkspace(String workspaceId, Workspace workspace) {
        workspace.setId(workspaceId);
        return workspaceLifecycleService.updateWorkspace(workspace);
    }

    /**
     * 删除 Workspace。
     */
    public void deleteWorkspace(String workspaceId) {
        workspaceLifecycleService.deleteWorkspace(workspaceId);
    }

    /**
     * 激活 Workspace。
     */
    public void activateWorkspace(String workspaceId) {
        workspaceLifecycleService.activateWorkspace(workspaceId);
    }

    /**
     * 停用 Workspace。
     */
    public void deactivateWorkspace(String workspaceId) {
        workspaceLifecycleService.deactivateWorkspace(workspaceId);
    }

    /**
     * 归档 Workspace。
     */
    public void archiveWorkspace(String workspaceId) {
        workspaceLifecycleService.archiveWorkspace(workspaceId);
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
        return memberManagementService.addMember(workspaceId, characterId, role, WorkspaceMember.Layer.NORMAL);
    }

    /**
     * 移除成员。
     */
    public void removeMember(String workspaceId, String memberId) {
        memberManagementService.removeMember(workspaceId, memberId);
    }

    // ==================== 岗位管理 ====================

    /**
     * 获取岗位列表。
     */
    public List<TeamPositionResponse> listTeamPositions(String workspaceId) {
        List<TeamPosition> positions = teamPositionsManagementService.listPositions(workspaceId);
        return teamPositionsManagementService.toResponseList(positions);
    }

    /**
     * 添加岗位。
     */
    public TeamPositionResponse addTeamPosition(String workspaceId, String roleName,
            String rolePackage, String purpose, String scope) {
        TeamPosition position = teamPositionsManagementService.addPosition(
                workspaceId, roleName, rolePackage, purpose, scope);
        return teamPositionsManagementService.toResponse(position);
    }

    /**
     * 更新岗位。
     * 当分配 Character 时，自动创建派驻关系。
     * 当分配 Built-in 类型时，初始化插件 Character。
     */
    public TeamPositionResponse updateTeamPosition(String workspaceId, String positionId,
            String assignedCharacterId, String assignedBuiltinType, Boolean enabled) {
        TeamPosition position = teamPositionsManagementService.updatePosition(
                workspaceId, positionId, assignedCharacterId, assignedBuiltinType, enabled);

        // 联动：根据类型处理
        if (assignedBuiltinType != null && !assignedBuiltinType.isBlank()) {
            // Built-in 类型：初始化插件
            try {
                workspacePluginService.initializeWorkspacePlugins(workspaceId);
                log.info("[WorkspaceService] Initialized built-in plugins for workspace {} via team position {}",
                        workspaceId, positionId);
            } catch (Exception e) {
                log.warn("[WorkspaceService] Failed to initialize built-in plugins for workspace {}: {}",
                        workspaceId, e.getMessage());
            }
        } else if (assignedCharacterId != null && !assignedCharacterId.isBlank()) {
            // 普通 Character：自动创建派驻
            try {
                TeamPosition updated = teamPositionsManagementService.getPosition(workspaceId, positionId).orElse(null);
                if (updated != null) {
                    deploymentService.deployCharacter(
                            assignedCharacterId,
                            workspaceId,
                            updated.getRoleName(),
                            updated.getRoleName(),
                            3);
                    log.info("[WorkspaceService] Auto deployed character {} to workspace {} via team position {}",
                            assignedCharacterId, workspaceId, positionId);
                }
            } catch (Exception e) {
                log.warn("[WorkspaceService] Failed to auto deploy character {} to workspace {}: {}",
                        assignedCharacterId, workspaceId, e.getMessage());
            }
        }

        return teamPositionsManagementService.toResponse(position);
    }

    /**
     * 删除岗位。
     */
    public void deleteTeamPosition(String workspaceId, String positionId) {
        teamPositionsManagementService.deletePosition(workspaceId, positionId);
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
                all = workspaceTaskService.listTasksByStatus(workspaceId, ts);
            } catch (Exception e) {
                all = workspaceTaskService.listTasks(workspaceId);
            }
        } else {
            all = workspaceTaskService.listTasks(workspaceId);
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
        return workspaceTaskService.getTask(workspaceId, taskId);
    }

    /**
     * 执行任务（处理 NormalizedMessage，支持任务续跑）。
     */
    public Task executeTask(String workspaceId, NormalizedMessage message, String creatorId) {
        TaskContinuationResolver.ContinuationResult result =
                taskContinuationResolver.resolve(workspaceId, message);

        if (result.getDecision() == TaskContinuationResolver.ContinuationDecision.CONTINUE_EXISTING_TASK) {
            String taskId = result.getTaskId();
            log.info("[WorkspaceService] Continuing existing task {} for message", taskId);

            Task matchedTask = workspaceTaskService.getTask(workspaceId, taskId)
                    .orElseThrow(() -> new IllegalStateException("Task not found: " + taskId));

            Task executableTask = taskResumeTargetResolver.resolveExecutableTask(matchedTask);

            Task parentTask = matchedTask.getParentTaskId() != null
                    ? workspaceTaskService.getTask(workspaceId, matchedTask.getParentTaskId())
                            .orElse(matchedTask)
                    : matchedTask;

            executableTask = workspaceTaskService.resumeTask(workspaceId, executableTask.getId(), message.getTextContent());
            taskExecutionService.executeChildTask(executableTask, parentTask);

            return matchedTask;
        } else {
            TaskCreationCommand command = TaskCreationCommand.builder()
                    .taskName("用户请求")
                    .taskDescription(message.getTextContent())
                    .input(message)
                    .creatorId(creatorId)
                    .metadata(message.getMetadata())
                    .sourceChannel(message.getChannel())
                    .sourceMessageId(message.getMessageId())
                    .sourceChatId(message.getChatId())
                    .build();
            return executeTask(workspaceId, command);
        }
    }

    /**
     * 执行任务（通过 TaskCreationCommand 提交新任务）。
     */
    public Task executeTask(String workspaceId, TaskCreationCommand command) {
        return workspaceTaskArrangementService.submitTask(
                workspaceId, command, TaskExecutionMode.AUTO, null);
    }
}