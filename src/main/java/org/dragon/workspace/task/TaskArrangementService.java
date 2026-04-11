package org.dragon.workspace.task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dragon.character.Character;
import org.dragon.config.PromptKeys;
import org.dragon.config.service.ConfigApplication;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.cooperation.chat.ChatRoom;
import org.dragon.workspace.cooperation.task.CollaborationSessionCoordinator;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.member.WorkspaceMemberService;
import org.dragon.workspace.plugin.WorkspacePluginService;
import org.dragon.workspace.task.dto.AssignmentDecision;
import org.dragon.workspace.task.dto.ChildTaskPlan;
import org.dragon.workspace.task.dto.PromptWriterInput;
import org.dragon.workspace.task.dto.TaskCreationCommand;
import org.dragon.workspace.task.dto.TaskDecompositionResult;
import org.dragon.workspace.task.util.MemberUtils;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.agent.llm.util.CharacterCaller;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务编排服务
 * <p>
 * 职责：
 * - 任务分解（通过 ProjectManager Character）
 * - 成员选择（通过 MemberSelector Character）
 * - 子任务创建与依赖映射
 * - 协作会话绑定
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskArrangementService {

    private final WorkspaceRegistry workspaceRegistry;
    private final WorkspaceMemberService memberService;
    private final ChatRoom chatRoom;
    private final TaskExecutionService taskExecutionService;
    private final CollaborationSessionCoordinator sessionCoordinator;
    private final WorkspacePluginService workspacePluginService;
    private final CharacterCaller characterCaller;
    private final ConfigApplication configApplication;
    private final StoreFactory storeFactory;
    private final Gson gson = new Gson();

    private TaskStore getTaskStore() {
        return storeFactory.get(TaskStore.class);
    }

    /**
     * 任务执行模式枚举
     */
    @Getter
    public enum TaskExecutionMode {
        AUTO,
        SPECIFIED,
        DEFAULT
    }

    /**
     * 提交任务到工作空间
     *
     * @param workspaceId 工作空间 ID
     * @param command 任务创建命令对象
     * @param executionMode 执行模式
     * @param specifiedCharacterIds 指定的 Character ID 列表
     * @return 工作空间任务
     */
    public Task submitTask(String workspaceId, TaskCreationCommand command,
            TaskExecutionMode executionMode, List<String> specifiedCharacterIds) {
        Workspace workspace = workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

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

        getTaskStore().save(task);
        log.info("[TaskArrangementService] Submitted task {} to workspace {}", task.getId(), workspaceId);

        processTask(task, workspace, executionMode, specifiedCharacterIds);

        return task;
    }

    /**
     * 处理任务
     */
    private void processTask(Task task, Workspace workspace,
            TaskExecutionMode executionMode, List<String> specifiedCharacterIds) {
        List<WorkspaceMember> members = memberService.listMembers(task.getWorkspaceId());
        if (members.isEmpty()) {
            log.warn("[TaskArrangementService] No members available in workspace {}", task.getWorkspaceId());
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("No members available");
            getTaskStore().update(task);
            return;
        }

        if (executionMode == TaskExecutionMode.SPECIFIED) {
            handleSpecifiedMode(task, members, specifiedCharacterIds);
        } else if (executionMode == TaskExecutionMode.DEFAULT) {
            handleDefaultMode(task, members);
        } else {
            handleAutoMode(task, workspace, members);
        }
    }

    /**
     * AUTO 模式：通过 ProjectManager 分解任务
     */
    private void handleAutoMode(Task task, Workspace workspace, List<WorkspaceMember> members) {
        // 1. 任务分解
        TaskDecompositionResult decompositionResult = decompose(task, workspace, members);

        if (decompositionResult == null || decompositionResult.getChildTasks() == null || decompositionResult.getChildTasks().isEmpty()) {
            log.warn("[TaskArrangementService] Task decomposition failed for task {}", task.getId());
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("Task decomposition returned no child tasks");
            getTaskStore().update(task);
            return;
        }

        // 2. 成员分配
        TaskDecompositionResult assignedResult = resolveAssignments(decompositionResult, workspace, members);

        // 3. 创建子任务
        List<Task> childTasks = createChildTasks(assignedResult, task);

        if (childTasks.isEmpty()) {
            log.warn("[TaskArrangementService] Failed to parse child tasks for task {}", task.getId());
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("Failed to parse child tasks");
            getTaskStore().update(task);
            return;
        }

        // 保存子任务
        for (Task childTask : childTasks) {
            getTaskStore().save(childTask);
        }
        task.setChildTaskIds(childTasks.stream().map(Task::getId).toList());
        task.setStatus(TaskStatus.RUNNING);
        getTaskStore().update(task);

        // 4. 创建协作会话并绑定
        sessionCoordinator.createAndBindSession(task, childTasks);

        // 5. 执行子任务
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
     * 分解任务
     */
    private TaskDecompositionResult decompose(Task parentTask, Workspace workspace, List<WorkspaceMember> members) {
        try {
            Character promptWriter = workspacePluginService.getBuiltinCharacter(workspace.getId(), "prompt_writer");
            String promptTemplate = configApplication.getGlobalPrompt(PromptKeys.PROJECT_MANAGER_DECOMPOSE, "请将以下任务拆解为可执行的子任务。");
            String promptWriterInput = buildDecomposePromptWriterInput(parentTask, members, promptTemplate);
            String fullPrompt = characterCaller.call(promptWriter, promptWriterInput);

            Character projectManager = workspacePluginService.getBuiltinCharacter(workspace.getId(), "project_manager");
            String result = characterCaller.call(projectManager, fullPrompt);

            return parseDecompositionResult(result);
        } catch (Exception e) {
            log.error("[TaskArrangementService] Task decomposition failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析任务分配
     */
    private TaskDecompositionResult resolveAssignments(TaskDecompositionResult result, Workspace workspace, List<WorkspaceMember> members) {
        if (result == null || result.getChildTasks() == null || result.getChildTasks().isEmpty()) {
            return result;
        }

        Character promptWriter = workspacePluginService.getBuiltinCharacter(workspace.getId(), "prompt_writer");
        Character memberSelector = workspacePluginService.getBuiltinCharacter(workspace.getId(), "member_selector");
        String promptTemplate = configApplication.getGlobalPrompt(PromptKeys.MEMBER_SELECTOR_SELECT, "请从可用成员中选择最合适的执行者来完成指定任务。");

        List<String> excludedCharacterIds = new ArrayList<>();

        for (ChildTaskPlan plan : result.getChildTasks()) {
            if (plan.getCharacterId() != null && !plan.getCharacterId().isEmpty()) {
                excludedCharacterIds.add(plan.getCharacterId());
                continue;
            }

            try {
                AssignmentDecision decision = selectMemberForTask(plan, workspace, members, promptWriter, memberSelector, promptTemplate, excludedCharacterIds);
                if (decision != null && decision.getSelectedCharacterId() != null) {
                    plan.setCharacterId(decision.getSelectedCharacterId());
                    plan.setCharacterRole(decision.getSelectedMemberRole());
                    excludedCharacterIds.add(decision.getSelectedCharacterId());
                }
            } catch (Exception e) {
                log.error("[TaskArrangementService] Failed to assign plan {}: {}", plan.getPlanTaskId(), e.getMessage());
            }
        }

        return result;
    }

    /**
     * 为单个任务选择成员
     */
    private AssignmentDecision selectMemberForTask(ChildTaskPlan plan, Workspace workspace,
            List<WorkspaceMember> members, Character promptWriter, Character memberSelector,
            String promptTemplate, List<String> excludedCharacterIds) {

        String promptWriterInputJson = buildMemberSelectionPromptWriterInput(plan, workspace, members, promptTemplate, excludedCharacterIds);
        String selectionPrompt = characterCaller.call(promptWriter, promptWriterInputJson);
        String rawResult = characterCaller.call(memberSelector, selectionPrompt);

        if (rawResult == null || rawResult.isEmpty() || "NONE".equalsIgnoreCase(rawResult.trim())) {
            return null;
        }

        String selectedId = rawResult.trim();
        WorkspaceMember selectedMember = members.stream()
                .filter(m -> m.getCharacterId().equals(selectedId))
                .findFirst()
                .orElse(null);

        if (selectedMember == null) {
            return null;
        }

        return AssignmentDecision.builder()
                .planTaskId(plan.getPlanTaskId())
                .selectedCharacterId(selectedId)
                .selectedMemberRole(selectedMember.getRole())
                .selectionReason("Selected based on capability matching")
                .confidence(0.8)
                .decisionAt(LocalDateTime.now())
                .build();
    }

    /**
     * 从分解结果创建子任务列表
     */
    private List<Task> createChildTasks(TaskDecompositionResult result, Task parentTask) {
        if (result == null || result.getChildTasks() == null || result.getChildTasks().isEmpty()) {
            return List.of();
        }

        List<ChildTaskPlan> plans = result.getChildTasks();
        List<Task> tasks = new ArrayList<>();

        // 阶段1：创建所有 Task
        for (ChildTaskPlan plan : plans) {
            Task task = Task.builder()
                    .id(UUID.randomUUID().toString())
                    .parentTaskId(parentTask.getId())
                    .workspaceId(parentTask.getWorkspaceId())
                    .name(plan.getName())
                    .description(plan.getDescription())
                    .characterId(plan.getCharacterId())
                    .input(parentTask.getInput())
                    .status(TaskStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .metadata(parentTask.getMetadata())
                    .sourceChatId(parentTask.getSourceChatId())
                    .sourceMessageId(parentTask.getSourceMessageId())
                    .sourceChannel(parentTask.getSourceChannel())
                    .build();
            tasks.add(task);
        }

        // 阶段2：建立 planTaskId -> taskId 映射
        Map<String, String> planIdMapping = buildPlanIdMapping(plans, tasks);

        // 阶段3：解析依赖 planTaskIds 为真实 taskIds
        resolveDependencyPlanTaskIds(plans, tasks, planIdMapping);

        return tasks;
    }

    private Map<String, String> buildPlanIdMapping(List<ChildTaskPlan> plans, List<Task> tasks) {
        Map<String, String> mapping = new HashMap<>();
        for (int i = 0; i < plans.size(); i++) {
            String planId = plans.get(i).getPlanTaskId();
            if (planId != null && !planId.isEmpty()) {
                mapping.put(planId, tasks.get(i).getId());
            }
        }
        return mapping;
    }

    private void resolveDependencyPlanTaskIds(List<ChildTaskPlan> plans, List<Task> tasks, Map<String, String> planIdMapping) {
        for (int i = 0; i < plans.size(); i++) {
            List<String> planDeps = plans.get(i).getDependencyPlanTaskIds();
            if (planDeps == null || planDeps.isEmpty()) {
                continue;
            }
            List<String> resolvedDeps = new ArrayList<>();
            for (String planDepId : planDeps) {
                String resolvedId = planIdMapping.get(planDepId);
                if (resolvedId != null) {
                    resolvedDeps.add(resolvedId);
                }
            }
            tasks.get(i).setDependencyTaskIds(resolvedDeps);
        }
    }

    private List<String> getSpecifiedCharacterIds(List<WorkspaceMember> members, List<String> specifiedCharacterIds) {
        if (specifiedCharacterIds == null || specifiedCharacterIds.isEmpty()) {
            return Collections.emptyList();
        }
        return members.stream()
                .filter(m -> specifiedCharacterIds.contains(m.getCharacterId()))
                .map(WorkspaceMember::getCharacterId)
                .collect(Collectors.toList());
    }

    private String buildDecomposePromptWriterInput(Task task, List<WorkspaceMember> members, String promptTemplate) {
        List<PromptWriterInput.MemberInfo> memberInfos = members.stream()
                .map(m -> PromptWriterInput.MemberInfo.builder()
                        .characterId(m.getCharacterId())
                        .role(m.getRole())
                        .layer(m.getLayer() != null ? m.getLayer().toString() : null)
                        .capability(m.getTags() != null ? String.join(", ", m.getTags()) : null)
                        .description(MemberUtils.buildMemberDescription(m))
                        .build())
                .collect(Collectors.toList());

        Map<String, Object> memberCapabilities = members.stream()
                .collect(Collectors.toMap(WorkspaceMember::getCharacterId, m -> m.getTags() != null ? m.getTags() : List.of()));
        Map<String, String> memberDescriptions = members.stream()
                .collect(Collectors.toMap(WorkspaceMember::getCharacterId, MemberUtils::buildMemberDescription));

        Map<String, Object> contextHints = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "collaborationMode", "AUTO",
                "allowFollowUp", true,
                "maxChildTasks", 10,
                "memberCapabilities", memberCapabilities,
                "memberDescriptions", memberDescriptions,
                "collaborationConstraint", "子任务间应保持独立性，按依赖关系顺序执行"
        );

        var input = PromptWriterInput.builder()
                .workspaceId(task.getWorkspaceId())
                .promptType("task_decompose")
                .promptTemplate(promptTemplate)
                .task(PromptWriterInput.TaskInfo.builder()
                        .id(task.getId())
                        .name(task.getName())
                        .description(task.getDescription())
                        .input(task.getInput())
                        .parentTaskId(task.getParentTaskId())
                        .build())
                .members(memberInfos)
                .contextHints(contextHints)
                .build();

        return gson.toJson(input);
    }

    private String buildMemberSelectionPromptWriterInput(ChildTaskPlan plan, Workspace workspace,
            List<WorkspaceMember> members, String promptTemplate, List<String> excludedCharacterIds) {

        List<PromptWriterInput.MemberInfo> memberInfos = members.stream()
                .filter(m -> !excludedCharacterIds.contains(m.getCharacterId()))
                .map(m -> PromptWriterInput.MemberInfo.builder()
                        .characterId(m.getCharacterId())
                        .role(m.getRole())
                        .layer(m.getLayer() != null ? m.getLayer().toString() : null)
                        .capability(m.getTags() != null ? String.join(", ", m.getTags()) : null)
                        .description(MemberUtils.buildMemberDescription(m))
                        .build())
                .collect(Collectors.toList());

        PromptWriterInput.TaskInfo taskInfo = PromptWriterInput.TaskInfo.builder()
                .id(plan.getPlanTaskId())
                .name(plan.getName())
                .description(plan.getDescription())
                .build();

        Map<String, Object> contextHints = new HashMap<>();
        contextHints.put("expectedOutput", plan.getExpectedOutput());
        contextHints.put("excludeCharacterIds", excludedCharacterIds);
        contextHints.put("dependencyPlanTaskIds", plan.getDependencyPlanTaskIds());

        return gson.toJson(PromptWriterInput.builder()
                .workspaceId(workspace.getId())
                .promptType("member_selection")
                .promptTemplate(promptTemplate)
                .task(taskInfo)
                .members(memberInfos)
                .contextHints(contextHints)
                .allowFollowUp(false)
                .build());
    }

    private TaskDecompositionResult parseDecompositionResult(String result) {
        if (result == null || result.isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(result, TaskDecompositionResult.class);
        } catch (Exception e) {
            log.warn("[TaskArrangementService] Failed to parse decomposition result: {}", e.getMessage());
            return tryExtractFromAlternativeFormat(result);
        }
    }

    private TaskDecompositionResult tryExtractFromAlternativeFormat(String result) {
        try {
            com.google.gson.JsonObject json = gson.fromJson(result, com.google.gson.JsonObject.class);
            var builder = TaskDecompositionResult.builder();
            if (json.has("summary")) builder.summary(json.get("summary").getAsString());
            if (json.has("collaborationMode")) builder.collaborationMode(json.get("collaborationMode").getAsString());
            return builder.build();
        } catch (Exception e) {
            log.error("[TaskArrangementService] Failed to extract decomposition result: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 重新平衡任务
     */
    public void rebalance(String taskId, String feedback) {
        log.info("[TaskArrangementService] Rebalancing task {} with feedback: {}", taskId, feedback);
    }
}
