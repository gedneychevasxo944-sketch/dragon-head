package org.dragon.workspace.service.task.arrangement;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.chat.ChatRoom;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.service.task.arrangement.dto.TaskCreationCommand;
import org.dragon.workspace.service.task.arrangement.dto.TaskDecompositionResult;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TaskArrangementService 工作空间任务编排服务
 * 核心编排逻辑：任务分解、成员选择、任务分配
 * 通过 ProjectManager Character 实现智能编排
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceTaskArrangementService {

    private final WorkspaceRegistry workspaceRegistry;
    private final org.dragon.workspace.service.member.WorkspaceMemberManagementService memberService;
    private final ChatRoom chatRoom;
    private final org.dragon.workspace.service.task.execution.WorkspaceTaskExecutionService taskExecutionService;
    private final TaskDecomposer taskDecomposer;
    private final TaskAssignmentResolver taskAssignmentResolver;
    private final ChildTaskFactory childTaskFactory;
    private final org.dragon.workspace.service.task.execution.CollaborationSessionCoordinator sessionCoordinator;
    private final StoreFactory storeFactory;

    private TaskStore getTaskStore() {
        return storeFactory.get(TaskStore.class);
    }

    /**
     * 任务执行模式枚举
     */
    @Getter
    public enum TaskExecutionMode {
        /**
         * 自动选择 Character 执行
         */
        AUTO,
        /**
         * 使用指定的 Character 执行
         */
        SPECIFIED,
        /**
         * 使用默认 Character 执行
         */
        DEFAULT
    }

    /**
     * 提交任务到工作空间
     *
     * @param workspaceId 工作空间 ID
     * @param command 任务创建命令对象
     * @param executionMode 执行模式
     * @param specifiedCharacterIds 指定的 Character ID 列表（当 executionMode 为 SPECIFIED 时使用）
     * @return 工作空间任务
     */
    public Task submitTask(String workspaceId, TaskCreationCommand command,
            TaskExecutionMode executionMode, List<String> specifiedCharacterIds) {
        // 验证工作空间存在
        Workspace workspace = workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        // 创建任务
        Task task = Task.builder()
                .id(UUID.randomUUID().toString())
                .workspaceId(workspaceId)
                .name(command.getTaskName())
                .description(command.getTaskDescription())
                .input(command.getInput())
                .creatorId(command.getCreatorId())
                .status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .metadata(command.getMetadata())
                .sourceChatId(command.getSourceChatId())
                .sourceMessageId(command.getSourceMessageId())
                .sourceChannel(command.getSourceChannel())
                .build();

        // 持久化任务
        getTaskStore().save(task);
        log.info("[TaskArrangementService] Submitted task {} to workspace {}", task.getId(), workspaceId);

        // 自动开始处理任务
        processTask(task, workspace, executionMode, specifiedCharacterIds);

        return task;
    }

    /**
     * 处理任务
     * 主流程：获取成员 -> 任务分解 -> 创建协作会话 -> 保存子任务 -> 执行
     */
    private void processTask(Task task, Workspace workspace,
            TaskExecutionMode executionMode, List<String> specifiedCharacterIds) {
        // 获取工作空间成员
        List<WorkspaceMember> members = memberService.listMembers(task.getWorkspaceId());
        if (members.isEmpty()) {
            log.warn("[TaskArrangementService] No members available in workspace {}", task.getWorkspaceId());
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("No members available");
            getTaskStore().update(task);
            return;
        }

        // 处理指定模式（AUTO 模式下由 ProjectManager 决定角色）
        if (executionMode == TaskExecutionMode.SPECIFIED) {
            // 使用指定的 Character
            handleSpecifiedMode(task, members, specifiedCharacterIds);
        } else if (executionMode == TaskExecutionMode.DEFAULT) {
            // 使用默认 Character
            handleDefaultMode(task, members);
        } else {
            // AUTO 模式：通过 ProjectManager 分解任务，characterId 直接从分解结果获取
            handleAutoMode(task, workspace, members);
        }
    }

    /**
     * AUTO 模式：任务分解后直接获取 characterId
     */
    private void handleAutoMode(Task task, Workspace workspace, List<WorkspaceMember> members) {
        // 1. 任务分解 - 委托给 TaskDecomposer（只分解结构，不分配角色）
        TaskDecompositionResult decompositionResult = taskDecomposer.decompose(task, workspace, members);

        if (decompositionResult == null || decompositionResult.getChildTasks() == null || decompositionResult.getChildTasks().isEmpty()) {
            log.warn("[TaskArrangementService] Task decomposition failed for task {}", task.getId());
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("Task decomposition returned no child tasks");
            getTaskStore().update(task);
            return;
        }

        // 2. 成员分配 - 委托给 TaskAssignmentResolver（通过 member_selector 选择执行者）
        TaskDecompositionResult assignedResult = taskAssignmentResolver.resolveAssignments(decompositionResult, workspace, members);

        // 3. 创建子任务 - 委托给 ChildTaskFactory（使用已分配的角色创建任务）
        List<Task> childTasks = childTaskFactory.createChildTasks(assignedResult, task);

        if (childTasks.isEmpty()) {
            log.warn("[TaskArrangementService] Failed to parse child tasks for task {}", task.getId());
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("Failed to parse child tasks");
            getTaskStore().update(task);
            return;
        }

        // 保存子任务到 Store
        for (Task childTask : childTasks) {
            getTaskStore().save(childTask);
        }
        task.setChildTaskIds(childTasks.stream().map(Task::getId).toList());
        task.setStatus(TaskStatus.RUNNING);
        getTaskStore().update(task);

        // 3. 创建协作会话并绑定 - 委托给 CollaborationSessionCoordinator
        sessionCoordinator.createAndBindSession(task, childTasks);

        // 4. 执行子任务（委托给 TaskExecutionService）
        taskExecutionService.executeChildTasks(childTasks, task);
    }

    /**
     * SPECIFIED 模式：使用指定的 Character
     */
    private void handleSpecifiedMode(Task task, List<WorkspaceMember> members, List<String> specifiedCharacterIds) {
        List<String> characterIds = getSpecifiedCharacterIds(members, specifiedCharacterIds);
        if (characterIds.isEmpty()) {
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("No valid specified characters");
            getTaskStore().update(task);
            return;
        }

        createAndExecuteSingleChildTask(task, characterIds.get(0), characterIds);
    }

    /**
     * DEFAULT 模式：使用默认 Character
     */
    private void handleDefaultMode(Task task, List<WorkspaceMember> members) {
        if (members.isEmpty()) {
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("No members available");
            getTaskStore().update(task);
            return;
        }

        String characterId = members.get(0).getCharacterId();
        createAndExecuteSingleChildTask(task, characterId, List.of(characterId));
    }

    /**
     * 创建并执行单一子任务
     */
    private void createAndExecuteSingleChildTask(Task parentTask, String characterId, List<String> assignedMemberIds) {
        Task childTask = Task.builder()
                .id(UUID.randomUUID().toString())
                .parentTaskId(parentTask.getId())
                .workspaceId(parentTask.getWorkspaceId())
                .characterId(characterId)
                .name(parentTask.getName())
                .description(parentTask.getDescription())
                .input(parentTask.getInput())
                .status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        getTaskStore().save(childTask);
        parentTask.setChildTaskIds(List.of(childTask.getId()));
        parentTask.setAssignedMemberIds(assignedMemberIds);
        parentTask.setStatus(TaskStatus.RUNNING);
        getTaskStore().update(parentTask);

        taskExecutionService.executeChildTask(childTask, parentTask);
    }

    /**
     * 获取指定的 Character ID 列表
     */
    private List<String> getSpecifiedCharacterIds(List<WorkspaceMember> members,
            List<String> specifiedCharacterIds) {
        if (specifiedCharacterIds == null || specifiedCharacterIds.isEmpty()) {
            return Collections.emptyList();
        }

        return members.stream()
                .filter(m -> specifiedCharacterIds.contains(m.getCharacterId()))
                .map(WorkspaceMember::getCharacterId)
                .collect(Collectors.toList());
    }

    /**
     * 重新平衡任务
     *
     * @param taskId 任务 ID
     * @param feedback 执行反馈
     */
    public void rebalance(String taskId, ExecutionFeedback feedback) {
        // TODO: 实现动态调整逻辑
        log.info("[TaskArrangementService] Rebalancing task {} with feedback: {}", taskId, feedback);
    }

    /**
     * ExecutionFeedback 执行反馈
     */
    public static class ExecutionFeedback {
        private String childTaskId;
        private boolean success;
        private String errorMessage;
        private long durationMs;

        public ExecutionFeedback(String childTaskId, boolean success, String errorMessage, long durationMs) {
            this.childTaskId = childTaskId;
            this.success = success;
            this.errorMessage = errorMessage;
            this.durationMs = durationMs;
        }

        public String getChildTaskId() { return childTaskId; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public long getDurationMs() { return durationMs; }
    }
}
