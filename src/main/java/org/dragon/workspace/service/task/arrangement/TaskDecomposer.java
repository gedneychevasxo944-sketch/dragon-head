package org.dragon.workspace.service.task.arrangement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dragon.task.Task;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.built_ins.BuiltInCharacterFactory;
import org.dragon.workspace.service.task.arrangement.dto.TaskDecompositionResult;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

/**
 * 任务分解器
 * 负责将父任务分解为子任务计划
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class TaskDecomposer {

    private final BuiltInCharacterFactory builtInCharacterFactory;
    private final org.dragon.agent.llm.util.CharacterCaller characterCaller;
    private final org.dragon.config.PromptManager promptManager;
    private final Gson gson = new Gson();

    public TaskDecomposer(
            BuiltInCharacterFactory builtInCharacterFactory,
            org.dragon.agent.llm.util.CharacterCaller characterCaller,
            org.dragon.config.PromptManager promptManager) {
        this.builtInCharacterFactory = builtInCharacterFactory;
        this.characterCaller = characterCaller;
        this.promptManager = promptManager;
    }

    /**
     * 分解任务
     */
    public TaskDecompositionResult decompose(Task parentTask, Workspace workspace, List<WorkspaceMember> members) {
        try {
            // 获取 PromptWriter Character
            var promptWriterCharacter = builtInCharacterFactory.getPromptWriterCharacterFactory()
                    .getOrCreatePromptWriterCharacter(workspace.getId());

            // 获取 prompt 模板
            String promptTemplate = promptManager.getGlobalPrompt(
                    org.dragon.config.PromptKeys.PROJECT_MANAGER_DECOMPOSE,
                    "请将以下任务拆解为可执行的子任务。");

            // 构建 PromptWriter 输入
            String promptWriterInput = buildPromptWriterInput("task_decompose", promptTemplate, parentTask, members);

            // 调用 PromptWriter 获取完整 prompt
            String fullPrompt = characterCaller.call(promptWriterCharacter, promptWriterInput);

            // 获取 ProjectManager Character
            var projectManagerCharacter = builtInCharacterFactory.getProjectManagerCharacterFactory()
                    .getOrCreateProjectManagerCharacter(workspace.getId());

            // 调用 ProjectManager 进行任务分解
            String result = characterCaller.call(projectManagerCharacter, fullPrompt);

            // 解析结果
            return parseDecompositionResult(result);

        } catch (Exception e) {
            log.error("[TaskDecomposer] Task decomposition failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 构建 PromptWriter 输入
     */
    private String buildPromptWriterInput(String promptType, String promptTemplate, Task task, List<WorkspaceMember> members) {
        var memberInfos = members.stream()
                .map(m -> {
                    String capability = m.getTags() != null && !m.getTags().isEmpty()
                            ? String.join(", ", m.getTags())
                            : null;
                    String description = buildMemberDescription(m);
                    return org.dragon.workspace.built_ins.character.prompt_writer.dto.PromptWriterInput.MemberInfo.builder()
                            .characterId(m.getCharacterId())
                            .role(m.getRole())
                            .layer(m.getLayer() != null ? m.getLayer().toString() : null)
                            .capability(capability)
                            .description(description)
                            .build();
                })
                .collect(Collectors.toList());

        Map<String, Object> memberCapabilities = members.stream()
                .collect(Collectors.toMap(
                        WorkspaceMember::getCharacterId,
                        m -> m.getTags() != null ? m.getTags() : List.of()
                ));
        Map<String, Object> memberDescriptions = members.stream()
                .collect(Collectors.toMap(
                        WorkspaceMember::getCharacterId,
                        this::buildMemberDescription
                ));

        Map<String, Object> contextHints = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "collaborationMode", "AUTO",
                "allowFollowUp", true,
                "maxChildTasks", 10,
                "memberCapabilities", memberCapabilities,
                "memberDescriptions", memberDescriptions,
                "collaborationConstraint", "子任务间应保持独立性，按依赖关系顺序执行"
        );

        var input = org.dragon.workspace.built_ins.character.prompt_writer.dto.PromptWriterInput.builder()
                .workspaceId(task.getWorkspaceId())
                .promptType(promptType)
                .promptTemplate(promptTemplate)
                .task(org.dragon.workspace.built_ins.character.prompt_writer.dto.PromptWriterInput.TaskInfo.builder()
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
     * 构建成员描述
     */
    private String buildMemberDescription(WorkspaceMember m) {
        StringBuilder sb = new StringBuilder();
        if (m.getRole() != null) {
            sb.append("角色: ").append(m.getRole());
        }
        if (m.getLayer() != null) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("层级: ").append(m.getLayer());
        }
        if (m.getTags() != null && !m.getTags().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("标签: ").append(String.join(", ", m.getTags()));
        }
        return sb.toString();
    }

    /**
     * 解析分解结果
     */
    private TaskDecompositionResult parseDecompositionResult(String result) {
        if (result == null || result.isEmpty()) {
            log.warn("[TaskDecomposer] Empty decomposition result");
            return null;
        }
        try {
            return gson.fromJson(result, TaskDecompositionResult.class);
        } catch (Exception e) {
            log.warn("[TaskDecomposer] Failed to parse decomposition result: {}", e.getMessage());
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
            log.error("[TaskDecomposer] Failed to extract decomposition result: {}", e.getMessage());
            return null;
        }
    }
}
