package org.dragon.workspace.built_ins.character.prompt_writer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.CharacterRuntimeBinder;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.built_ins.character.AbstractWorkspaceCharacterFactory;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * PromptWriter Character 工厂
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class PromptWriterCharacterFactory extends AbstractWorkspaceCharacterFactory {

    private static final String PROMPT_WRITER_CHARACTER_PREFIX = "prompt_writer_";
    private static final String CHARACTER_TYPE = "prompt_writer";
    private static final String CHARACTER_NAME = "Prompt Writer";
    private static final String CHARACTER_DESCRIPTION = "负责将 prompt 模板与动态数据拼接成完整的 prompt";

    private final PromptWriterCharacterTools promptWriterCharacterTools;

    public PromptWriterCharacterFactory(CharacterRegistry characterRegistry,
                                      WorkspaceRegistry workspaceRegistry,
                                      CharacterRuntimeBinder characterRuntimeBinder,
                                      PromptWriterCharacterTools promptWriterCharacterTools) {
        super(characterRegistry, workspaceRegistry, characterRuntimeBinder);
        this.promptWriterCharacterTools = promptWriterCharacterTools;
    }

    @Override
    protected String getCharacterIdPrefix() {
        return PROMPT_WRITER_CHARACTER_PREFIX;
    }

    @Override
    public String getCharacterType() {
        return CHARACTER_TYPE;
    }

    @Override
    protected String getCharacterName() {
        return CHARACTER_NAME;
    }

    @Override
    protected String getCharacterDescription() {
        return CHARACTER_DESCRIPTION;
    }

    @Override
    public List<ToolConnector> getAvailableTools() {
        return promptWriterCharacterTools.getAvailableTools();
    }

    @Override
    protected void buildCharacterExtensions(Character character, String workspaceId) {
        List<ToolConnector> availableTools = getAvailableTools();
        Set<String> allowedToolNames = new HashSet<>();
        for (ToolConnector tool : availableTools) {
            if (tool != null && tool.getName() != null) {
                allowedToolNames.add(tool.getName());
            }
        }
        character.setAllowedTools(allowedToolNames);
    }

    @Override
    public Character createCharacter(String workspaceId) {
        String characterId = getCharacterId(workspaceId);
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        Character character = createCharacterInstance(characterId, workspaceId);
        characterRuntimeBinder.bind(character, workspaceId);
        characterRegistry.register(character);

        log.info("[PromptWriterCharacterFactory] Created PromptWriter character {} for workspace {} with tools: {}",
                characterId, workspaceId, character.getAllowedTools());
        return character;
    }

    public Character getOrCreatePromptWriterCharacter(String workspaceId) {
        return getOrCreateCharacter(workspaceId);
    }

    public boolean hasPromptWriterCharacter(String workspaceId) {
        return hasCharacter(workspaceId);
    }
}
