package org.dragon.workspace;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.observer.actionlog.ObserverActionLog;
import org.dragon.workspace.hiring.HireMode;
import org.dragon.workspace.material.Material;
import org.dragon.workspace.member.CharacterDuty;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.observer.actionlog.ObserverActionLogService;
import org.dragon.workspace.service.hiring.WorkspaceHiringService;
import org.dragon.workspace.service.lifecycle.WorkspaceLifecycleService;
import org.dragon.workspace.service.material.WorkspaceMaterialService;
import org.dragon.workspace.service.member.WorkspaceMemberManagementService;
import org.dragon.task.Task;
import org.dragon.task.TaskStore;
import org.dragon.workspace.service.task.arrangement.WorkspaceTaskArrangementService;
import org.dragon.workspace.service.task.arrangement.WorkspaceTaskArrangementService.TaskExecutionMode;
import org.dragon.workspace.service.task.arrangement.dto.TaskCreationCommand;
import org.dragon.workspace.service.task.execution.WorkspaceTaskExecutionService;
import org.dragon.workspace.service.WorkspaceTaskService;
import org.dragon.workspace.service.TaskContinuationResolver;
import org.dragon.workspace.service.TaskResumeTargetResolver;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceApplication Workspace 应用实例
 * 代表一个具体的 Workspace 实例，通过 Builder 构建
 * 提供对内所有 Workspace 服务的统一访问
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Getter
public class WorkspaceApplication {

    private final String workspaceId;
    private final WorkspaceLifecycleService workspaceLifecycleService;
    private final WorkspaceHiringService workspaceHiringService;
    private final ObserverActionLogService workspaceActionLogService;
    private final WorkspaceMemberManagementService workspaceMemberService;
    private final WorkspaceMaterialService materialService;
    private final WorkspaceTaskService workspaceTaskService;
    private final CharacterRegistry characterRegistry;
    private final WorkspaceTaskArrangementService workspaceTaskArrangementService;
    private final TaskStore taskStore;
    private final TaskContinuationResolver taskContinuationResolver;
    private final TaskResumeTargetResolver taskResumeTargetResolver;
    private final WorkspaceTaskExecutionService taskExecutionService;

    /**
     * 私有构造函数，通过 Builder 构建
     */
    WorkspaceApplication(WorkspaceApplicationBuilder builder) {
        this.workspaceId = builder.workspaceId;
        this.workspaceLifecycleService = builder.workspaceLifecycleService;
        this.workspaceHiringService = builder.workspaceHiringService;
        this.workspaceActionLogService = builder.workspaceActionLogService;
        this.workspaceMemberService = builder.workspaceMemberService;
        this.materialService = builder.materialService;
        this.workspaceTaskService = builder.workspaceTaskService;
        this.characterRegistry = builder.characterRegistry;
        this.workspaceTaskArrangementService = builder.workspaceTaskArrangementService;
        this.taskStore = builder.taskStore;
        this.taskContinuationResolver = builder.taskContinuationResolver;
        this.taskResumeTargetResolver = builder.taskResumeTargetResolver;
        this.taskExecutionService = builder.taskExecutionService;
    }

    // ==================== Workspace 生命周期管理委托 ====================

    public Workspace createWorkspace(Workspace workspace) {
        return workspaceLifecycleService.createWorkspace(workspace);
    }

    public Workspace updateWorkspace(Workspace workspace) {
        return workspaceLifecycleService.updateWorkspace(workspace);
    }

    public void deleteWorkspace(String workspaceId) {
        workspaceLifecycleService.deleteWorkspace(workspaceId);
    }

    public Optional<Workspace> getWorkspace(String workspaceId) {
        return workspaceLifecycleService.getWorkspace(workspaceId);
    }

    public List<Workspace> listWorkspaces() {
        return workspaceLifecycleService.listWorkspaces();
    }

    public List<Workspace> listWorkspacesByStatus(Workspace.Status status) {
        return workspaceLifecycleService.listWorkspacesByStatus(status);
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

    // ==================== 成员管理委托 ====================

    public WorkspaceMember addMember(String workspaceId, String characterId, String role) {
        return workspaceMemberService.addMember(workspaceId, characterId, role, WorkspaceMember.Layer.NORMAL);
    }

    public void removeMember(String workspaceId, String characterId) {
        workspaceMemberService.removeMember(workspaceId, characterId);
    }

    public List<WorkspaceMember> listMembers(String workspaceId) {
        return workspaceMemberService.listMembers(workspaceId);
    }

    // ==================== 雇佣管理委托 ====================

    public void hire(String workspaceId, String characterId, HireMode mode) {
        workspaceHiringService.hire(workspaceId, characterId, mode);
    }

    public void hire(String workspaceId, String characterId, HireMode mode, List<String> defaultCharacterIds) {
        workspaceHiringService.hire(workspaceId, characterId, mode, defaultCharacterIds);
    }

    public void fire(String workspaceId, String characterId, HireMode mode) {
        workspaceHiringService.fire(workspaceId, characterId, mode);
    }

    public Optional<Character> getHrCharacter(String workspaceId) {
        return workspaceHiringService.getHrCharacter(workspaceId);
    }

    public void setCharacterDuty(String workspaceId, String characterId, String dutyDescription) {
        workspaceHiringService.setCharacterDuty(workspaceId, characterId, dutyDescription);
    }

    public Optional<CharacterDuty> getCharacterDuty(String workspaceId, String characterId) {
        return workspaceHiringService.getCharacterDuty(workspaceId, characterId);
    }

    // ==================== 动作日志委托 ====================

    public List<ObserverActionLog> getActionLogs(String workspaceId) {
        return workspaceActionLogService.getActionLogs("WORKSPACE", workspaceId);
    }

    // ==================== 任务管理委托 ====================

    public Optional<Task> getTask(String workspaceId, String taskId) {
        return workspaceTaskService.getTask(workspaceId, taskId);
    }

    public String getTaskResult(String workspaceId, String taskId) {
        return workspaceTaskService.getTaskResult(workspaceId, taskId);
    }

    public Task cancelTask(String workspaceId, String taskId) {
        return workspaceTaskService.cancelTask(workspaceId, taskId);
    }

    public List<Task> listTasks(String workspaceId) {
        return workspaceTaskService.listTasks(workspaceId);
    }

    // ==================== 任务分发执行 ====================

    /**
     * 执行任务（处理 NormalizedMessage，支持任务续跑）
     * 当消息应该继续已有任务时，直接恢复该任务
     * 当消息应该开启新任务时，创建新任务
     *
     * @param message 归一化的用户消息
     * @param creatorId 创建者 ID
     * @return 工作空间任务
     */
    public Task executeTask(NormalizedMessage message, String creatorId) {
        // 调用续跑解析器判断
        TaskContinuationResolver.ContinuationResult result =
                taskContinuationResolver.resolve(workspaceId, message);

        if (result.getDecision() == TaskContinuationResolver.ContinuationDecision.CONTINUE_EXISTING_TASK) {
            // 继续已有任务
            String taskId = result.getTaskId();
            log.info("[WorkspaceApplication] Continuing existing task {} for message", taskId);

            Task matchedTask = workspaceTaskService.getTask(workspaceId, taskId)
                    .orElseThrow(() -> new IllegalStateException("Task not found: " + taskId));

            // 使用恢复目标解析器获取实际可执行的任务
            Task executableTask = taskResumeTargetResolver.resolveExecutableTask(matchedTask);

            // 获取父任务（用于 executeChildTask）
            Task parentTask = matchedTask.getParentTaskId() != null
                    ? workspaceTaskService.getTask(workspaceId, matchedTask.getParentTaskId())
                            .orElse(matchedTask)
                    : matchedTask;

            // 恢复任务（追加用户输入，不自动执行）
            executableTask = workspaceTaskService.resumeTask(workspaceId, executableTask.getId(), message.getTextContent());

            // 继续执行（传递正确的父子关系）
            taskExecutionService.executeChildTask(executableTask, parentTask);

            return matchedTask;
        } else {
            // 开启新任务
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
            return executeTask(command);
        }
    }

    /**
     * 执行任务（通过 TaskCreationCommand 提交新任务）
     *
     * @param command 任务创建命令
     * @return 工作空间任务
     */
    public Task executeTask(TaskCreationCommand command) {
        return workspaceTaskArrangementService.submitTask(
                workspaceId, command, TaskExecutionMode.AUTO, null);
    }

    // ==================== 物料管理委托 ====================

    public Material uploadMaterial(String workspaceId, InputStream inputStream,
            String filename, long size, String contentType, String uploader) {
        return materialService.upload(workspaceId, inputStream, filename, size, contentType, uploader);
    }

    public Optional<Material> getMaterial(String workspaceId, String materialId) {
        return materialService.get(materialId);
    }

    public InputStream downloadMaterial(String workspaceId, String materialId) {
        return materialService.download(materialId);
    }

    public void deleteMaterial(String workspaceId, String materialId) {
        materialService.delete(materialId);
    }

    public List<Material> listMaterials(String workspaceId) {
        return materialService.listByWorkspace(workspaceId);
    }
}
