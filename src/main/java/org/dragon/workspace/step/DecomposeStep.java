package org.dragon.workspace.step;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dragon.agent.llm.util.CharacterCaller;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.config.PromptKeys;
import org.dragon.config.service.ConfigApplication;
import org.dragon.store.StoreFactory;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.context.StepResult;
import org.dragon.workspace.context.TaskContext;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.member.WorkspaceMemberService;
import org.dragon.workspace.task.dto.ChildTaskPlan;
import org.dragon.workspace.task.dto.TaskDecompositionResult;
import org.dragon.workspace.task.util.MemberUtils;

import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * DecomposeStep - 任务拆解
 *
 * <p>从 TaskArrangementService.decompose 迁移而来。
 * 通过 prompt_writer + project_manager 两个 Character 进行任务拆解。
 *
 * @author yijunw
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DecomposeStep implements Step {

    private final WorkspaceRegistry workspaceRegistry;
    private final WorkspaceMemberService memberService;
    private final CharacterRegistry characterRegistry;
    private final CharacterCaller characterCaller;
    private final ConfigApplication configApplication;
    private final StoreFactory storeFactory;
    private final Gson gson = new Gson();

    @Override
    public String getName() {
        return "decompose";
    }

    @Override
    public StepResult execute(TaskContext ctx) {
        long startTime = System.currentTimeMillis();
        Task task = ctx.getTask();
        String workspaceId = ctx.getWorkspaceId();

        if (task == null || workspaceId == null) {
            return StepResult.builder()
                    .stepName(getName())
                    .success(false)
                    .errorMessage("No task or workspace in context")
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        try {
            Workspace workspace = workspaceRegistry.get(workspaceId)
                    .orElseThrow(() -> new IllegalStateException("Workspace not found: " + workspaceId));
            List<WorkspaceMember> members = memberService.listMembers(workspaceId);

            // 任务分解
            TaskDecompositionResult result = decompose(task, workspace, members);

            if (result == null || result.getChildTasks() == null || result.getChildTasks().isEmpty()) {
                return StepResult.builder()
                        .stepName(getName())
                        .success(false)
                        .errorMessage("Task decomposition returned no child tasks")
                        .durationMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            // 创建子任务
            List<Task> childTasks = createChildTasks(result, task);

            // 保存子任务
            TaskStore taskStore = storeFactory.get(TaskStore.class);
            for (Task childTask : childTasks) {
                taskStore.save(childTask);
            }

            // 更新父任务
            task.setChildTaskIds(childTasks.stream().map(Task::getId).toList());
            task.setStatus(TaskStatus.RUNNING);
            taskStore.update(task);

            // 设置到上下文
            ctx.setConfigValue("childTasks", childTasks);

            return StepResult.builder()
                    .stepName(getName())
                    .input(task.getId())
                    .output(Map.of("childTaskCount", childTasks.size(), "childTasks", childTasks))
                    .success(true)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("[DecomposeStep] Task decomposition failed: {}", e.getMessage(), e);
            return StepResult.builder()
                    .stepName(getName())
                    .input(task.getId())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private TaskDecompositionResult decompose(Task parentTask, Workspace workspace, List<WorkspaceMember> members) {
        try {
            Character promptWriter = characterRegistry.get("prompt_writer").orElse(null);
            String promptTemplate = configApplication.getGlobalPrompt(PromptKeys.PROJECT_MANAGER_DECOMPOSE, "请将以下任务拆解为可执行的子任务。");
            String promptWriterInput = buildDecomposePromptWriterInput(parentTask, members, promptTemplate);
            String fullPrompt = characterCaller.call(promptWriter, promptWriterInput);

            Character projectManager = characterRegistry.get("project_manager").orElse(null);
            String result = characterCaller.call(projectManager, fullPrompt);

            return parseDecompositionResult(result);
        } catch (Exception e) {
            log.error("[DecomposeStep] Task decomposition failed: {}", e.getMessage(), e);
            return null;
        }
    }

    private String buildDecomposePromptWriterInput(Task task, List<WorkspaceMember> members, String promptTemplate) {
        List<Map<String, String>> memberInfos = members.stream()
                .map(m -> Map.of(
                        "characterId", m.getCharacterId(),
                        "role", m.getRole() != null ? m.getRole() : "",
                        "description", MemberUtils.buildMemberDescription(m)
                ))
                .toList();

        Map<String, Object> contextHints = new HashMap<>();
        contextHints.put("timestamp", LocalDateTime.now().toString());
        contextHints.put("collaborationMode", "AUTO");
        contextHints.put("allowFollowUp", true);
        contextHints.put("maxChildTasks", 10);

        Map<String, Object> input = Map.of(
                "workspaceId", task.getWorkspaceId(),
                "promptType", "task_decompose",
                "promptTemplate", promptTemplate,
                "task", Map.of(
                        "id", task.getId(),
                        "name", task.getName(),
                        "description", task.getDescription(),
                        "input", task.getInput() != null ? task.getInput().toString() : ""
                ),
                "members", memberInfos,
                "contextHints", contextHints
        );

        return gson.toJson(input);
    }

    private TaskDecompositionResult parseDecompositionResult(String result) {
        if (result == null || result.isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(result, TaskDecompositionResult.class);
        } catch (Exception e) {
            log.warn("[DecomposeStep] Failed to parse decomposition result: {}", e.getMessage());
            return null;
        }
    }

    private List<Task> createChildTasks(TaskDecompositionResult result, Task parentTask) {
        if (result == null || result.getChildTasks() == null || result.getChildTasks().isEmpty()) {
            return List.of();
        }

        List<ChildTaskPlan> plans = result.getChildTasks();
        List<Task> tasks = new java.util.ArrayList<>();

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
                    .build();
            tasks.add(task);
        }

        return tasks;
    }
}
