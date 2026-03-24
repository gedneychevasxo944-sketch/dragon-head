package org.dragon.workspace.service;

import org.dragon.task.Task;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.service.dto.TaskDecompositionResult;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class TaskDecomposer {

    private final org.dragon.workspace.built_ins.character.project_manager.ProjectManagerCharacterFactory projectManagerCharacterFactory;
    private final org.dragon.workspace.built_ins.character.prompt_writer.PromptWriterCharacterFactory promptWriterCharacterFactory;
    private final org.dragon.agent.llm.util.CharacterCaller characterCaller;
    private final org.dragon.config.PromptManager promptManager;
    private final org.dragon.config.PromptKeys PromptKeys;
    private final PromptWriterInputAssembler promptWriterInputAssembler;

    /**
     * 分解任务
     *
     * @param parentTask 父任务
     * @param workspace Workspace
     * @param members 成员列表
     * @return 分解结果
     */
    public TaskDecompositionResult decompose(Task parentTask, Workspace workspace, java.util.List<WorkspaceMember> members) {
        try {
            // 获取 PromptWriter Character
            var promptWriterCharacter = promptWriterCharacterFactory
                    .getOrCreatePromptWriterCharacter(workspace.getId());

            // 获取 prompt 模板
            String promptTemplate = promptManager.getGlobalPrompt(
                    org.dragon.config.PromptKeys.PROJECT_MANAGER_DECOMPOSE,
                    "请将以下任务拆解为可执行的子任务。");

            // 构建 PromptWriter 输入
            String promptWriterInput = promptWriterInputAssembler.buildTaskDecomposeInput(
                    "task_decompose", promptTemplate, parentTask, members);

            // 调用 PromptWriter 获取完整 prompt
            String fullPrompt = characterCaller.call(promptWriterCharacter, promptWriterInput);

            // 获取 ProjectManager Character
            var projectManagerCharacter = projectManagerCharacterFactory
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
     * 解析分解结果
     */
    private TaskDecompositionResult parseDecompositionResult(String result) {
        if (result == null || result.isEmpty()) {
            log.warn("[TaskDecomposer] Empty decomposition result");
            return null;
        }
        try {
            return new com.google.gson.Gson().fromJson(result, TaskDecompositionResult.class);
        } catch (Exception e) {
            log.warn("[TaskDecomposer] Failed to parse decomposition result: {}", e.getMessage());
            return tryExtractFromAlternativeFormat(result);
        }
    }

    private TaskDecompositionResult tryExtractFromAlternativeFormat(String result) {
        try {
            com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(result, com.google.gson.JsonObject.class);
            var builder = TaskDecompositionResult.builder();
            if (json.has("summary")) builder.summary(json.get("summary").getAsString());
            if (json.has("collaborationMode")) builder.collaborationMode(json.get("collaborationMode").getAsString());
            // 进一步解析 childTasks...
            return builder.build();
        } catch (Exception e) {
            log.error("[TaskDecomposer] Failed to extract decomposition result: {}", e.getMessage());
            return null;
        }
    }
}
