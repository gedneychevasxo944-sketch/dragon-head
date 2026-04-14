package org.dragon.workspace.task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dragon.agent.llm.util.CharacterCaller;
import org.dragon.agent.react.context.PromptMaterialContext;
import org.dragon.agent.react.context.PromptMaterialConfig;
import org.dragon.agent.react.context.PromptMaterialContextBuilder;
import org.dragon.character.Character;
import org.dragon.config.PromptKeys;
import org.dragon.config.service.ConfigApplication;
import org.dragon.store.StoreFactory;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.cooperation.chat.ChatRoom;
import org.dragon.workspace.cooperation.task.CollaborationSessionCoordinator;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.member.WorkspaceMemberService;
import org.dragon.workspace.plugin.WorkspacePluginService;
import org.dragon.workspace.task.dto.AssignmentDecision;
import org.dragon.workspace.task.dto.BatchMemberSelectionInput;
import org.dragon.workspace.task.dto.ChildTaskPlan;
import org.dragon.workspace.task.dto.PromptWriterInput;
import org.dragon.workspace.task.dto.TaskCreationCommand;
import org.dragon.workspace.task.dto.TaskDecompositionResult;
import org.dragon.workspace.task.util.MemberUtils;
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
    private final PromptMaterialContextBuilder promptMaterialContextBuilder;
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

        if (null == executionMode) {
            handleAutoMode(task, workspace, members);
        } else switch (executionMode) {
            case SPECIFIED -> handleSpecifiedMode(task, members, specifiedCharacterIds);
            case DEFAULT -> handleDefaultMode(task, members);
            default -> handleAutoMode(task, workspace, members);
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
     * SPECIFIED 模式：使用指定的 Character，直接执行跳过子任务封装
     */
    private void handleSpecifiedMode(Task task, List<WorkspaceMember> members, List<String> specifiedCharacterIds) {
        List<String> characterIds = getSpecifiedCharacterIds(members, specifiedCharacterIds);
        if (characterIds.isEmpty()) {
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("No valid specified characters");
            getTaskStore().update(task);
            return;
        }

        task.setAssignedMemberIds(characterIds);
        task.setStatus(TaskStatus.RUNNING);
        getTaskStore().update(task);

        taskExecutionService.executeSpecifiedTask(task, characterIds.get(0));
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
        // TODO: 这里是从所有character里面取的第一个，后面应该需要根据配置来选
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

        // 分离已分配和待分配的 plan
        List<String> excludedCharacterIds = new ArrayList<>();
        List<ChildTaskPlan> needsAssignment = new ArrayList<>();

        for (ChildTaskPlan plan : result.getChildTasks()) {
            if (plan.getCharacterId() != null && !plan.getCharacterId().isEmpty()) {
                excludedCharacterIds.add(plan.getCharacterId());
            } else {
                needsAssignment.add(plan);
            }
        }

        // 3+ 个待分配任务时使用批量选择
        if (needsAssignment.size() >= 3) {
            try {
                Map<String, String> batchAssignments = selectMembersForTasksBatch(
                        needsAssignment, workspace, members, promptWriter, memberSelector, promptTemplate, excludedCharacterIds);

                for (ChildTaskPlan plan : needsAssignment) {
                    String assignedId = batchAssignments.get(plan.getPlanTaskId());
                    if (assignedId != null) {
                        plan.setCharacterId(assignedId);
                        WorkspaceMember member = members.stream()
                                .filter(m -> m.getCharacterId().equals(assignedId))
                                .findFirst().orElse(null);
                        if (member != null) {
                            plan.setCharacterRole(member.getRole());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("[TaskArrangementService] Batch assignment failed, falling back to serial: {}", e.getMessage(), e);
                assignSerially(needsAssignment, workspace, members, promptWriter, memberSelector, promptTemplate, excludedCharacterIds);
            }
        } else {
            // 1-2 个待分配任务，使用串行选择
            assignSerially(needsAssignment, workspace, members, promptWriter, memberSelector, promptTemplate, excludedCharacterIds);
        }

        return result;
    }

    /**
     * 批量选择成员（单次 LLM 调用）
     */
    private Map<String, String> selectMembersForTasksBatch(List<ChildTaskPlan> plans, Workspace workspace,
            List<WorkspaceMember> members, Character promptWriter, Character memberSelector,
            String promptTemplate, List<String> excludedCharacterIds) {

        List<BatchMemberSelectionInput.PlanSummary> planSummaries = plans.stream()
                .map(p -> BatchMemberSelectionInput.PlanSummary.builder()
                        .planTaskId(p.getPlanTaskId())
                        .taskName(p.getName())
                        .taskDescription(p.getDescription())
                        .expectedOutput(p.getExpectedOutput())
                        .dependencyPlanTaskIds(p.getDependencyPlanTaskIds())
                        .build())
                .collect(Collectors.toList());

        List<BatchMemberSelectionInput.MemberInfo> memberInfos = members.stream()
                .filter(m -> !excludedCharacterIds.contains(m.getCharacterId()))
                .map(m -> BatchMemberSelectionInput.MemberInfo.builder()
                        .characterId(m.getCharacterId())
                        .role(m.getRole())
                        .description(MemberUtils.buildMemberDescription(m))
                        .build())
                .collect(Collectors.toList());

        BatchMemberSelectionInput input = BatchMemberSelectionInput.builder()
                .workspaceId(workspace.getId())
                .plans(planSummaries)
                .availableMembers(memberInfos)
                .excludedCharacterIds(excludedCharacterIds)
                .build();

        String inputJson = gson.toJson(input);
        String batchPromptTemplate = configApplication.getGlobalPrompt(
                PromptKeys.MEMBER_SELECTOR_SELECT_BATCH,
                "请为以下每个任务选择最合适的执行者，返回 JSON 数组格式：[{planTaskId, characterId}]");
        String promptWriterInput = String.format(batchPromptTemplate, inputJson);
        String selectionPrompt = characterCaller.call(promptWriter, promptWriterInput);

        return parseBatchSelectionResult(selectionPrompt);
    }

    /**
     * 解析批量选择结果
     */
    private Map<String, String> parseBatchSelectionResult(String rawResult) {
        Map<String, String> assignments = new HashMap<>();
        if (rawResult == null || rawResult.isEmpty()) {
            return assignments;
        }
        try {
            com.google.gson.JsonArray jsonArray = gson.fromJson(rawResult, com.google.gson.JsonArray.class);
            for (int i = 0; i < jsonArray.size(); i++) {
                com.google.gson.JsonObject obj = jsonArray.get(i).getAsJsonObject();
                String planTaskId = obj.get("planTaskId").getAsString();
                String characterId = obj.get("characterId").getAsString();
                assignments.put(planTaskId, characterId);
            }
        } catch (Exception e) {
            log.warn("[TaskArrangementService] Failed to parse batch selection result: {}", e.getMessage());
        }
        return assignments;
    }

    /**
     * 串行选择成员（兼容备用）
     */
    private void assignSerially(List<ChildTaskPlan> plans, Workspace workspace,
            List<WorkspaceMember> members, Character promptWriter, Character memberSelector,
            String promptTemplate, List<String> excludedCharacterIds) {

        for (ChildTaskPlan plan : plans) {
            try {
                AssignmentDecision decision = selectMemberForTask(plan, workspace, members, promptWriter, memberSelector, promptTemplate, excludedCharacterIds);
                if (decision != null && decision.getSelectedCharacterId() != null) {
                    plan.setCharacterId(decision.getSelectedCharacterId());
                    plan.setCharacterRole(decision.getSelectedMemberRole());
                    excludedCharacterIds.add(decision.getSelectedCharacterId());
                }
            } catch (Exception e) {
                log.error("[TaskArrangementService] Failed to assign plan {}: {}", plan.getPlanTaskId(), e.getMessage(), e);
            }
        }
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
                        .capability(null)
                        .description(MemberUtils.buildMemberDescription(m))
                        .build())
                .collect(Collectors.toList());

        // 从已构建的 memberInfos 复用 description，避免重复调用 MemberUtils.buildMemberDescription()
        Map<String, String> memberDescriptions = memberInfos.stream()
                .collect(Collectors.toMap(PromptWriterInput.MemberInfo::getCharacterId, PromptWriterInput.MemberInfo::getDescription));

        // 构建 contextHints
        Map<String, Object> contextHints = new HashMap<>();
        contextHints.put("timestamp", LocalDateTime.now().toString());
        contextHints.put("collaborationMode", "AUTO");
        contextHints.put("allowFollowUp", true);
        contextHints.put("maxChildTasks", 10);
        contextHints.put("memberDescriptions", memberDescriptions);
        contextHints.put("collaborationConstraint", "子任务间应保持独立性，按依赖关系顺序执行");

        // 通过 PromptMaterialContextBuilder 获取 Workspace Personality 并加入 contextHints
        if (promptMaterialContextBuilder != null) {
            PromptMaterialContext pctx = promptMaterialContextBuilder.buildForTaskDecomposition(
                    task.getWorkspaceId(), task, members);
            if (pctx != null && pctx.getWorkspacePersonality() != null) {
                contextHints.put("workspacePersonality", buildWorkspacePersonalityContext(pctx.getWorkspacePersonality()));
            }
        }

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

    /**
     * 构建 Workspace Personality 上下文字符串
     */
    private String buildWorkspacePersonalityContext(org.dragon.workspace.WorkspacePersonality personality) {
        if (personality == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (personality.getWorkingStyle() != null) {
            sb.append("工作风格: ").append(personality.getWorkingStyle()).append("; ");
        }
        if (personality.getDecisionPattern() != null) {
            sb.append("决策模式: ").append(personality.getDecisionPattern()).append("; ");
        }
        if (personality.getRiskTolerance() != null) {
            sb.append("风险容忍度: ").append(personality.getRiskTolerance()).append("; ");
        }
        if (personality.getCoreValues() != null && !personality.getCoreValues().isBlank()) {
            sb.append("核心价值观: ").append(personality.getCoreValues()).append("; ");
        }
        if (personality.getCollaborationPreference() != null && !personality.getCollaborationPreference().isBlank()) {
            sb.append("协作偏好: ").append(personality.getCollaborationPreference());
        }
        return sb.toString();
    }

    private String buildMemberSelectionPromptWriterInput(ChildTaskPlan plan, Workspace workspace,
            List<WorkspaceMember> members, String promptTemplate, List<String> excludedCharacterIds) {

        List<PromptWriterInput.MemberInfo> memberInfos = members.stream()
                .filter(m -> !excludedCharacterIds.contains(m.getCharacterId()))
                .map(m -> PromptWriterInput.MemberInfo.builder()
                        .characterId(m.getCharacterId())
                        .role(m.getRole())
                        .layer(m.getLayer() != null ? m.getLayer().toString() : null)
                        .capability(null)
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
            log.error("[TaskArrangementService] Failed to extract decomposition result: {}", e.getMessage(), e);
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
