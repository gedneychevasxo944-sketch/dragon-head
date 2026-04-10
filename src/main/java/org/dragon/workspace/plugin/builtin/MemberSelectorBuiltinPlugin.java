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
 * MemberSelector Built-in Plugin
 * 负责从 Workspace 中已雇佣的 Character 中选择最合适的执行者
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberSelectorBuiltinPlugin implements WorkspaceBuiltinPlugin {

    private final BuiltInCharacterFactory builtInCharacterFactory;
    private final CharacterCaller characterCaller;

    @Override
    public String getType() {
        return "member_selector";
    }

    @Override
    public Character getOrCreateCharacter(String workspaceId, String characterId) {
        return builtInCharacterFactory.getOrCreateMemberSelectorCharacter(workspaceId);
    }

    @Override
    public PluginResult execute(String workspaceId, Map<String, Object> input) {
        try {
            String characterId = (String) input.get("characterId");
            Character memberSelector = getOrCreateCharacter(workspaceId, characterId);
            String prompt = (String) input.get("prompt");

            if (prompt == null || prompt.isBlank()) {
                return PluginResult.failure("prompt is required");
            }

            String result = characterCaller.call(memberSelector, prompt);
            return PluginResult.success(result);
        } catch (Exception e) {
            log.error("[MemberSelectorBuiltinPlugin] Failed to execute: {}", e.getMessage());
            return PluginResult.failure(e.getMessage());
        }
    }
}