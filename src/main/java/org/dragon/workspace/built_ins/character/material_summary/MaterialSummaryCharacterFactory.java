package org.dragon.workspace.built_ins.character.material_summary;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.CharacterRuntimeBinder;
import org.dragon.config.store.ConfigKey;
import org.dragon.config.store.ConfigStore;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.built_ins.character.AbstractWorkspaceCharacterFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MaterialSummaryCharacter 工厂
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class MaterialSummaryCharacterFactory extends AbstractWorkspaceCharacterFactory {

    private static final String MATERIAL_SUMMARY_CHARACTER_PREFIX = "material_summary_";
    private static final String CHARACTER_TYPE = "material_summary";
    private static final String CHARACTER_NAME = "Material Summary";
    private static final String CHARACTER_DESCRIPTION = "负责为物料生成摘要";
    private static final String CONFIG_KEY_SUMMARY_PROMPT = "summaryPrompt";

    private final MaterialSummaryCharacterTools materialSummaryCharacterTools;
    private final ConfigStore configStore;

    public MaterialSummaryCharacterFactory(CharacterRegistry characterRegistry,
                                          WorkspaceRegistry workspaceRegistry,
                                          CharacterRuntimeBinder characterRuntimeBinder,
                                          MaterialSummaryCharacterTools materialSummaryCharacterTools,
                                          ConfigStore configStore) {
        super(characterRegistry, workspaceRegistry, characterRuntimeBinder);
        this.materialSummaryCharacterTools = materialSummaryCharacterTools;
        this.configStore = configStore;
    }

    @Override
    protected String getCharacterIdPrefix() {
        return MATERIAL_SUMMARY_CHARACTER_PREFIX;
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
        return materialSummaryCharacterTools.getAvailableTools();
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
        buildCharacterExtensions(character, workspaceId);
        characterRuntimeBinder.bind(character, workspaceId);
        characterRegistry.register(character);

        log.info("[MaterialSummaryCharacterFactory] Created MaterialSummary character {} for workspace {}",
                characterId, workspaceId);
        return character;
    }

    public Character getOrCreateMaterialSummaryCharacter(String workspaceId) {
        return getOrCreateCharacter(workspaceId);
    }

    public boolean hasMaterialSummaryCharacter(String workspaceId) {
        return hasCharacter(workspaceId);
    }

    public String getSummaryPrompt(String workspaceId) {
        ConfigKey configKey = ConfigKey.character(workspaceId, getCharacterId(workspaceId), CONFIG_KEY_SUMMARY_PROMPT);
        return configStore.get(configKey, "请为以下物料生成简洁摘要：\n{{materialContent}}");
    }

    public void setSummaryPrompt(String workspaceId, String prompt) {
        ConfigKey configKey = ConfigKey.character(workspaceId, getCharacterId(workspaceId), CONFIG_KEY_SUMMARY_PROMPT);
        configStore.set(configKey, prompt);
    }
}
