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
import org.dragon.util.UserUtils;
import org.dragon.workspace.member.HandlerType;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.member.WorkspaceMemberService;
import org.dragon.workspace.task.TaskArrangementService;
import org.dragon.workspace.task.TaskArrangementService.TaskExecutionMode;
import org.dragon.workspace.task.TaskExecutionService;
import org.dragon.workspace.task.TaskResumeService;
import org.dragon.workspace.task.WorkspaceTaskService;
import org.dragon.workspace.task.dto.TaskCreationCommand;
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
    private final PermissionService permissionService;
    private final DeploymentService deploymentService;
    private final WorkspaceTaskService workspaceTaskService;
    private final TaskArrangementService taskArrangementService;
    private final TaskResumeService taskResumeService;
    private final TaskExecutionService taskExecutionService;
    // private final TaskStore taskStore;

    // ==================== Workspace CRUD ====================

    public PageResponse<Workspace> listWorkspaces(int page, int pageSize, String search,
                                                  String status, String teamStatus, Boolean hasObserver) {
        Long userId = Long.valueOf(UserUtils.getUserId());
        List<String> visibleIds = permissionService.getVisibleAssets(ResourceType.WORKSPACE, userId);
        if (visibleIds == null || visibleIds.isEmpty()) {
            return PageResponse.of(List.of(), 0, page, pageSize);
        }

        List<Workspace> all;
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            try {
                Workspace.Status st = Workspace.Status.valueOf(status.toUpperCase());
                all = workspaceLifecycleService.listWorkspacesByIdsAndStatus(visibleIds, st);
            } catch (IllegalArgumentException e) {
                all = workspaceLifecycleService.listWorkspacesByIds(visibleIds);
            }
        } else {
            all = workspaceLifecycleService.listWorkspacesByIds(visibleIds);
        }

        List<Workspace> filtered = all.stream()
                .filter(w -> {
                    if (search != null && !search.isBlank()) {
                        String s = search.toLowerCase();
                        return w.getName() != null && w.getName().toLowerCase().contains(s);
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
        return memberService.listPositions(workspaceId).stream()
                .map(TeamPositionResponse::from)
                .toList();
    }

    public TeamPositionResponse addTeamPosition(String workspaceId, String roleName,
            String rolePackage, String purpose, String scope) {
        WorkspaceMember position = memberService.addPosition(
                workspaceId, roleName, rolePackage, purpose, scope);
        return TeamPositionResponse.from(position);
    }

    public TeamPositionResponse updateTeamPosition(String workspaceId, String positionId,
            String assignedCharacterId, String assignedBuiltinType, Boolean enabled) {
        // assignedBuiltinType 格式: "builtin:{type}"，如 "builtin:hr"
        HandlerType handlerType = HandlerType.BUILTIN_CHARACTER;
        String handlerId = null;

        if (assignedBuiltinType != null && !assignedBuiltinType.isBlank()) {
            // 从 builtin:{type} 格式中提取 builtin type，然后获取对应的 character id
            if (assignedBuiltinType.startsWith("builtin:")) {
                String builtinType = assignedBuiltinType.substring("builtin:".length());
                // builtin type 就是 character id，如 "hr", "member_selector" 等
                handlerId = builtinType;
            }
        } else if (assignedCharacterId != null && !assignedCharacterId.isBlank()) {
            handlerId = assignedCharacterId;
        }

        final String finalHandlerId = handlerId;
        memberService.updatePositionHandler(workspaceId, positionId, handlerType, handlerId);

        // 处理 enabled
        if (enabled != null) {
            memberService.getPosition(workspaceId, positionId).ifPresent(m -> {
                m.setEnabled(enabled);
                memberService.updateMemberRole(workspaceId, positionId, m.getRole());
            });
        }

        // 如果分配了 character，自动部署
        if (finalHandlerId != null) {
            try {
                memberService.getPosition(workspaceId, positionId).ifPresent(m -> {
                    deploymentService.deployCharacter(
                            finalHandlerId,
                            workspaceId,
                            m.getRoleName(),
                            m.getRoleName(),
                            3);
                    log.info("[WorkspaceFacadeService] Auto deployed character {} to workspace {} via position {}",
                            finalHandlerId, workspaceId, positionId);
                });
            } catch (Exception e) {
                log.warn("[WorkspaceFacadeService] Failed to auto deploy character {} to workspace {}: {}",
                        finalHandlerId, workspaceId, e.getMessage());
            }
        }

        return memberService.getPosition(workspaceId, positionId)
                .map(TeamPositionResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Position not found: " + positionId));
    }

    public void deleteTeamPosition(String workspaceId, String positionId) {
        memberService.removePosition(workspaceId, positionId);
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
                    .sourceChannel(message.getChannel().getCode())
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
