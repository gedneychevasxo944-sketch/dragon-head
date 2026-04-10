package org.dragon.workspace.plugin.builtin;

import java.util.Map;

import org.dragon.character.Character;
import org.dragon.character.builtin.BuiltInCharacterFactory;
import org.dragon.workspace.plugin.PluginResult;
import org.dragon.workspace.plugin.WorkspaceBuiltinPlugin;
import org.dragon.agent.llm.util.CharacterCaller;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ProjectManager Built-in Plugin
 * 负责将复杂任务拆解为可执行的子任务
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectManagerBuiltinPlugin implements WorkspaceBuiltinPlugin {

    private final BuiltInCharacterFactory builtInCharacterFactory;
    private final CharacterCaller characterCaller;

    @Override
    public String getType() {
        return "project_manager";
    }

    @Override
    public Character getOrCreateCharacter(String workspaceId, String characterId) {
        return builtInCharacterFactory.getOrCreateProjectManagerCharacter(workspaceId);
    }

    @Override
    public PluginResult execute(String workspaceId, Map<String, Object> input) {
        try {
            String charId = (String) input.get("characterId");
            Character projectManager = getOrCreateCharacter(workspaceId, charId);
            String prompt = (String) input.get("prompt");

            if (prompt == null || prompt.isBlank()) {
                return PluginResult.failure("prompt is required");
            }

            String result = characterCaller.call(projectManager, prompt);
            return PluginResult.success(result);
        } catch (Exception e) {
            log.error("[ProjectManagerBuiltinPlugin] Failed to execute: {}", e.getMessage());
            return PluginResult.failure(e.getMessage());
        }
    }
}