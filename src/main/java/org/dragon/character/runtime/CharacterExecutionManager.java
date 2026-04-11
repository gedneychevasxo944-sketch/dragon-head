package org.dragon.character.runtime;

import org.dragon.agent.model.ModelRegistry;
import org.dragon.agent.orchestration.OrchestrationService;
import org.dragon.agent.react.ReActExecutor;
import org.dragon.agent.workflow.WorkflowExecutor;
import org.dragon.agent.workflow.WorkflowStore;
import org.dragon.character.Character;
import org.dragon.character.mind.Mind;
import org.dragon.character.mind.TraitResolutionService;
import org.dragon.config.service.ConfigApplication;
import org.dragon.skill.runtime.SkillRegistry;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Character 执行管理器
 * 负责为 Character 注入运行时依赖并执行
 *
 * <p>当 Character 从数据库加载后，其 runtime 字段为 null。
 * 在调用 character.run() 前，必须通过此类注入运行时依赖。
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterExecutionManager {

    private final ReActExecutor reActExecutor;
    private final ConfigApplication configApplication;
    private final WorkflowExecutor workflowExecutor;
    private final WorkflowStore workflowStore;
    private final ModelRegistry modelRegistry;
    private final OrchestrationService orchestrationService;
    private final SkillRegistry skillRegistry;
    private final TraitResolutionService traitResolutionService;

    /**
     * 执行 Character 对话
     *
     * @param character Character 实例
     * @param userInput 用户输入
     * @return AI 回复内容
     */
    public String execute(Character character, String userInput) {
        validateCharacter(character);

        // 构建运行时依赖
        CharacterRuntime runtime = buildRuntime(character);

        // 注入运行时
        character.setRuntime(runtime);

        try {
            return character.run(userInput);
        } catch (IllegalStateException e) {
            String message = parseRuntimeError(e, character.getId());
            log.error("[CharacterExecutionManager] Execution failed for character {}: {}",
                    character.getId(), message);
            throw new IllegalStateException(message);
        }
    }

    /**
     * 执行 Character 对话（带 Task 上下文）
     *
     * @param character Character 实例
     * @param userInput 用户输入
     * @param task Task 上下文
     * @return AI 回复内容
     */
    public String execute(Character character, String userInput, org.dragon.task.Task task) {
        validateCharacter(character);

        CharacterRuntime runtime = buildRuntime(character);
        character.setRuntime(runtime);

        try {
            return character.run(userInput, task);
        } catch (IllegalStateException e) {
            String message = parseRuntimeError(e, character.getId());
            log.error("[CharacterExecutionManager] Execution failed for character {}: {}",
                    character.getId(), message);
            throw new IllegalStateException(message);
        }
    }

    /**
     * 验证 Character 实例
     */
    private void validateCharacter(Character character) {
        if (character == null) {
            throw new IllegalArgumentException("Character cannot be null");
        }
        if (character.getId() == null) {
            throw new IllegalArgumentException("Character id cannot be null");
        }
    }

    /**
     * 构建运行时依赖
     */
    private CharacterRuntime buildRuntime(Character character) {
        String workspaceId = null;
        if (character.getWorkspaceIds() != null && !character.getWorkspaceIds().isEmpty()) {
            workspaceId = character.getWorkspaceIds().get(0);
        }

        return CharacterRuntime.builder()
                .reActExecutor(reActExecutor)
                .configApplication(configApplication)
                .workflowExecutor(workflowExecutor)
                .workflowStore(workflowStore)
                .modelRegistry(modelRegistry)
                .orchestrationService(orchestrationService)
                .skillRegistry(skillRegistry)
                .traitResolutionService(traitResolutionService)
                .workspaceId(workspaceId != null ? parseWorkspaceId(workspaceId) : null)
                .build();
    }

    /**
     * 解析工作空间 ID
     */
    private Long parseWorkspaceId(String workspaceId) {
        try {
            return Long.parseLong(workspaceId);
        } catch (NumberFormatException e) {
            log.warn("[CharacterExecutionManager] Invalid workspace ID format: {}", workspaceId);
            return null;
        }
    }

    /**
     * 解析运行时错误信息
     */
    private String parseRuntimeError(IllegalStateException e, String characterId) {
        String message = e.getMessage();
        if (message == null) {
            return "Character " + characterId + " execution failed: unknown error";
        }

        if (message.contains("OrchestrationService not initialized")) {
            return "Character " + characterId + " is not properly initialized: OrchestrationService is missing";
        }
        if (message.contains("ReActExecutor not initialized")) {
            return "Character " + characterId + " is not properly initialized: ReActExecutor is missing";
        }
        if (message.contains("WorkflowExecutor not initialized")) {
            return "Character " + characterId + " is not properly initialized: WorkflowExecutor is missing";
        }
        if (message.contains("WorkflowStore not initialized")) {
            return "Character " + characterId + " is not properly initialized: WorkflowStore is missing";
        }
        if (message.contains("runtime is null")) {
            return "Character " + characterId + " is not properly initialized: runtime dependencies are missing. Please redeploy the character.";
        }

        return "Character " + characterId + " execution failed: " + message;
    }
}
