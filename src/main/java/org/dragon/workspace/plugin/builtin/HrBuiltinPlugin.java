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
 * Hr Built-in Plugin
 * 负责 Workspace 的人力资源管理，包括招聘、解雇、职责分配等
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrBuiltinPlugin implements WorkspaceBuiltinPlugin {

    private final BuiltInCharacterFactory builtInCharacterFactory;
    private final CharacterCaller characterCaller;

    @Override
    public String getType() {
        return "hr";
    }

    @Override
    public Character getOrCreateCharacter(String workspaceId, String characterId) {
        return builtInCharacterFactory.getOrCreateHrCharacter(workspaceId);
    }

    @Override
    public PluginResult execute(String workspaceId, Map<String, Object> input) {
        try {
            String charId = (String) input.get("characterId");
            Character hrCharacter = getOrCreateCharacter(workspaceId, charId);
            String prompt = (String) input.get("prompt");

            if (prompt == null || prompt.isBlank()) {
                return PluginResult.failure("prompt is required");
            }

            String result = characterCaller.call(hrCharacter, prompt);
            return PluginResult.success(result);
        } catch (Exception e) {
            log.error("[HrBuiltinPlugin] Failed to execute: {}", e.getMessage(), e);
            return PluginResult.failure(e.getMessage());
        }
    }
}