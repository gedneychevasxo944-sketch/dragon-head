package org.dragon.step.workspace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dragon.agent.llm.util.CharacterCaller;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.config.PromptKeys;
import org.dragon.config.service.ConfigApplication;
import org.dragon.store.StoreFactory;
import org.dragon.task.Task;
import org.dragon.task.TaskStore;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.WorkspaceFacadeService;
import org.dragon.step.StepResult;
import org.dragon.step.Step;
import org.dragon.step.ExecutionContext;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.member.WorkspaceMemberService;
import org.dragon.workspace.task.util.MemberUtils;

import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AssignStep - 成员分配
 *
 * <p>从 TaskArrangementService.resolveAssignments 迁移而来。
 * 通过 prompt_writer + member_selector 进行成员分配。
 *
 * @author yijunw
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssignStep implements Step {

    private final WorkspaceFacadeService workspaceFacadeService;
    private final WorkspaceMemberService memberService;
    private final CharacterRegistry characterRegistry;
    private final CharacterCaller characterCaller;
    private final ConfigApplication configApplication;
    private final StoreFactory storeFactory;
    private final Gson gson = new Gson();

    @Override
    public String getName() {
        return "assign";
    }

    @SuppressWarnings("unchecked")
    @Override
    public StepResult execute(ExecutionContext ctx) {
        long startTime = System.currentTimeMillis();
        String workspaceId = ctx.getWorkspaceId();

        if (workspaceId == null) {
            return StepResult.builder()
                    .stepName(getName())
                    .success(false)
                    .errorMessage("No workspace in context")
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        try {
            // 获取上下文中的子任务（从 DecomposeStep 产出）
            List<Task> childTasks = (List<Task>) ctx.getConfig().get("childTasks");
            if (childTasks == null || childTasks.isEmpty()) {
                log.info("[AssignStep] No child tasks to assign");
                return StepResult.builder()
                        .stepName(getName())
                        .output("no child tasks")
                        .success(true)
                        .durationMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            Workspace workspace = workspaceFacadeService.getWorkspace(workspaceId)
                    .orElseThrow(() -> new IllegalStateException("Workspace not found: " + workspaceId));
            List<WorkspaceMember> members = memberService.listMembers(workspaceId);

            // 成员分配
            List<Task> assignedTasks = resolveAssignments(childTasks, workspace, members);

            // 保存更新后的任务
            TaskStore taskStore = storeFactory.get(TaskStore.class);
            for (Task task : assignedTasks) {
                taskStore.update(task);
            }

            return StepResult.builder()
                    .stepName(getName())
                    .input(childTasks.size() + " tasks")
                    .output(Map.of("assignedCount", assignedTasks.size()))
                    .success(true)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("[AssignStep] Assignment failed: {}", e.getMessage(), e);
            return StepResult.builder()
                    .stepName(getName())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private List<Task> resolveAssignments(List<Task> tasks, Workspace workspace, List<WorkspaceMember> members) {
        List<String> excludedCharacterIds = new ArrayList<>();

        for (Task task : tasks) {
            if (task.getCharacterId() != null && !task.getCharacterId().isEmpty()) {
                excludedCharacterIds.add(task.getCharacterId());
            }
        }

        for (Task task : tasks) {
            if (task.getCharacterId() != null && !task.getCharacterId().isEmpty()) {
                continue;
            }

            try {
                String selectedId = selectMemberForTask(task, workspace, members, excludedCharacterIds);
                if (selectedId != null) {
                    task.setCharacterId(selectedId);
                    excludedCharacterIds.add(selectedId);
                    log.info("[AssignStep] Assigned task {} to character {}", task.getId(), selectedId);
                }
            } catch (Exception e) {
                log.error("[AssignStep] Failed to assign task {}: {}", task.getId(), e.getMessage());
            }
        }

        return tasks;
    }

    private String selectMemberForTask(Task task, Workspace workspace, List<WorkspaceMember> members, List<String> excludedCharacterIds) {
        Character promptWriter = characterRegistry.get("prompt_writer").orElse(null);
        Character memberSelector = characterRegistry.get("member_selector").orElse(null);
        String promptTemplate = configApplication.getGlobalPrompt(PromptKeys.MEMBER_SELECTOR_SELECT, "请从可用成员中选择最合适的执行者。");

        String promptWriterInputJson = buildMemberSelectionPromptWriterInput(task, workspace, members, promptTemplate, excludedCharacterIds);
        String selectionPrompt = characterCaller.call(promptWriter, promptWriterInputJson);
        String rawResult = characterCaller.call(memberSelector, selectionPrompt);

        if (rawResult == null || rawResult.isEmpty() || "NONE".equalsIgnoreCase(rawResult.trim())) {
            return null;
        }

        String selectedId = rawResult.trim();
        boolean found = members.stream().anyMatch(m -> m.getCharacterId().equals(selectedId));
        return found ? selectedId : null;
    }

    private String buildMemberSelectionPromptWriterInput(Task task, Workspace workspace,
            List<WorkspaceMember> members, String promptTemplate, List<String> excludedCharacterIds) {

        List<Map<String, String>> memberInfos = members.stream()
                .filter(m -> !excludedCharacterIds.contains(m.getCharacterId()))
                .map(m -> Map.of(
                        "characterId", m.getCharacterId(),
                        "role", m.getRole() != null ? m.getRole() : "",
                        "description", MemberUtils.buildMemberDescription(m)
                ))
                .toList();

        Map<String, Object> input = Map.of(
                "workspaceId", workspace.getId(),
                "promptType", "member_selection",
                "promptTemplate", promptTemplate,
                "task", Map.of(
                        "id", task.getId(),
                        "name", task.getName(),
                        "description", task.getDescription()
                ),
                "members", memberInfos
        );

        return gson.toJson(input);
    }
}
