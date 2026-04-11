package org.dragon.workspace;

import java.util.List;
import java.util.Optional;

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
import org.dragon.workspace.DeploymentService;
import org.dragon.workspace.member.TeamPosition;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.member.WorkspaceMemberService;
import org.dragon.workspace.member.TeamPositionService;
import org.dragon.workspace.plugin.WorkspacePluginService;
import org.dragon.workspace.task.TaskArrangementService;
import org.dragon.workspace.task.TaskArrangementService.TaskExecutionMode;
import org.dragon.workspace.task.dto.TaskCreationCommand;
import org.dragon.workspace.task.TaskExecutionService;
import org.dragon.workspace.task.TaskResumeService;
import org.dragon.workspace.task.WorkspaceTaskService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Workspace 门面服务
 *
 * <p>封装 Workspace 生命周期、成员、岗位、任务等核心业务逻辑。
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceFacadeService {

    private final WorkspaceLifecycleService workspaceLifecycleService;
    private final WorkspaceMemberService memberService;
    private final TeamPositionService teamPositionService;
    private final PermissionService permissionService;
    private final DeploymentService deploymentService;
    private final WorkspacePluginService workspacePluginService;
    private final WorkspaceTaskService workspaceTaskService;
    private final TaskArrangementService taskArrangementService;
    private final TaskResumeService taskResumeService;
    private final TaskExecutionService taskExecutionService;
    private final TaskStore taskStore;

    // ==================== Workspace CRUD ====================

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

    public Workspace createWorkspace(Workspace workspace) {
        return workspaceLifecycleService.createWorkspace(workspace);
    }

    public Optional<Workspace> getWorkspace(String workspaceId) {
        return workspaceLifecycleService.getWorkspace(workspaceId);
    }

    public Workspace updateWorkspace(String workspaceId, Workspace workspace) {
        workspace.setId(workspaceId);
        return workspaceLifecycleService.updateWorkspace(workspace);
    }

    public void deleteWorkspace(String workspaceId) {
        workspaceLifecycleService.deleteWorkspace(workspaceId);
    }

    public void activateWorkspace(String workspaceId) {
        workspaceLifecycleService.activateWorkspace(workspaceId);
    }

    public void deactivateWorkspace(String workspaceId) {
        workspaceLifecycleService.deactivateWorkspace(workspaceId);
    }

    public void archiveWorkspace(String workspaceId) {
        workspaceLifecycleService.archiveWorkspace(workspaceId);
    }

    // ==================== 成员管理 ====================

    public List<WorkspaceMember> listMembers(String workspaceId) {
        return memberService.listMembers(workspaceId);
    }

    public Optional<WorkspaceMember> getMember(String workspaceId, String characterId) {
        return memberService.getMember(workspaceId, characterId);
    }

    public WorkspaceMember addMember(String workspaceId, String characterId, String role,
                                     String position, Integer level, String sourceType) {
        return memberService.addMember(workspaceId, characterId, role, WorkspaceMember.Layer.NORMAL);
    }

    public void removeMember(String workspaceId, String memberId) {
        memberService.removeMember(workspaceId, memberId);
    }

    // ==================== 岗位管理 ====================

    public List<TeamPositionResponse> listTeamPositions(String workspaceId) {
        List<TeamPosition> positions = teamPositionService.listPositions(workspaceId);
        return teamPositionService.toResponseList(positions);
    }

    public TeamPositionResponse addTeamPosition(String workspaceId, String roleName,
            String rolePackage, String purpose, String scope) {
        TeamPosition position = teamPositionService.addPosition(
                workspaceId, roleName, rolePackage, purpose, scope);
        return teamPositionService.toResponse(position);
    }

    public TeamPositionResponse updateTeamPosition(String workspaceId, String positionId,
            String assignedCharacterId, String assignedBuiltinType, Boolean enabled) {
        TeamPosition position = teamPositionService.updatePosition(
                workspaceId, positionId, assignedCharacterId, assignedBuiltinType, enabled);

        if (assignedBuiltinType != null && !assignedBuiltinType.isBlank()) {
            try {
                workspacePluginService.initializeWorkspacePlugins(workspaceId);
                log.info("[WorkspaceFacadeService] Initialized built-in plugins for workspace {} via team position {}",
                        workspaceId, positionId);
            } catch (Exception e) {
                log.warn("[WorkspaceFacadeService] Failed to initialize built-in plugins for workspace {}: {}",
                        workspaceId, e.getMessage());
            }
        } else if (assignedCharacterId != null && !assignedCharacterId.isBlank()) {
            try {
                TeamPosition updated = teamPositionService.getPosition(workspaceId, positionId).orElse(null);
                if (updated != null) {
                    deploymentService.deployCharacter(
                            assignedCharacterId,
                            workspaceId,
                            updated.getRoleName(),
                            updated.getRoleName(),
                            3);
                    log.info("[WorkspaceFacadeService] Auto deployed character {} to workspace {} via team position {}",
                            assignedCharacterId, workspaceId, positionId);
                }
            } catch (Exception e) {
                log.warn("[WorkspaceFacadeService] Failed to auto deploy character {} to workspace {}: {}",
                        assignedCharacterId, workspaceId, e.getMessage());
            }
        }

        return teamPositionService.toResponse(position);
    }

    public void deleteTeamPosition(String workspaceId, String positionId) {
        teamPositionService.deletePosition(workspaceId, positionId);
    }

    // ==================== 任务管理 ====================

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

    public Optional<Task> getTask(String workspaceId, String taskId) {
        return workspaceTaskService.getTask(workspaceId, taskId);
    }

    public Task executeTask(String workspaceId, NormalizedMessage message, String creatorId) {
        TaskResumeService.ContinuationResult result =
                taskResumeService.resolve(workspaceId, message);

        if (result.getDecision() == TaskResumeService.ContinuationDecision.CONTINUE_EXISTING_TASK) {
            String taskId = result.getTaskId();
            log.info("[WorkspaceFacadeService] Continuing existing task {} for message", taskId);

            Task matchedTask = workspaceTaskService.getTask(workspaceId, taskId)
                    .orElseThrow(() -> new IllegalStateException("Task not found: " + taskId));

            Task executableTask = taskResumeService.resolveExecutableTask(matchedTask);

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

    public Task executeTask(String workspaceId, TaskCreationCommand command) {
        return taskArrangementService.submitTask(
                workspaceId, command, TaskExecutionMode.AUTO, null);
    }
}
