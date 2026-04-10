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
 * PromptWriter Built-in Plugin
 * 负责将 prompt 模板与动态数据拼接成完整的 prompt
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptWriterBuiltinPlugin implements WorkspaceBuiltinPlugin {

    private final BuiltInCharacterFactory builtInCharacterFactory;
    private final CharacterCaller characterCaller;

    @Override
    public String getType() {
        return "prompt_writer";
    }

    @Override
    public Character getOrCreateCharacter(String workspaceId, String characterId) {
        return builtInCharacterFactory.getOrCreatePromptWriterCharacter(workspaceId);
    }

    @Override
    public PluginResult execute(String workspaceId, Map<String, Object> input) {
        try {
            String charId = (String) input.get("characterId");
            Character promptWriter = getOrCreateCharacter(workspaceId, charId);
            String prompt = (String) input.get("prompt");

            if (prompt == null || prompt.isBlank()) {
                return PluginResult.failure("prompt is required");
            }

            String result = characterCaller.call(promptWriter, prompt);
            return PluginResult.success(result);
        } catch (Exception e) {
            log.error("[PromptWriterBuiltinPlugin] Failed to execute: {}", e.getMessage());
            return PluginResult.failure(e.getMessage());
        }
    }
}