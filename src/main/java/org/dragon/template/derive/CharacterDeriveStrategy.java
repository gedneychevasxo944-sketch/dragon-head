package org.dragon.template.derive;

import org.dragon.character.Character;
import org.dragon.character.profile.CharacterProfile;
import org.dragon.character.service.CharacterService;
import org.dragon.permission.enums.ResourceType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CharacterDeriveStrategy Character 派生策略
 *
 * @author yijunw
 */
@Component
public class CharacterDeriveStrategy implements DeriveStrategy {

    private final CharacterService characterService;

    public CharacterDeriveStrategy(CharacterService characterService) {
        this.characterService = characterService;
    }

    @Override
    public ResourceType getSupportedType() {
        return ResourceType.CHARACTER;
    }

    @Override
    public Object derive(Object templateAsset, DeriveContext context) {
        Character template = (Character) templateAsset;
        DeriveTemplateRequest request = context.getRequest();

        String newName = request.getName() != null ? request.getName() : template.getName() + "_copy";
        String newDescription = request.getDescription() != null ? request.getDescription() : template.getDescription();

        Character character = new Character();
        character.setName(newName);
        character.setDescription(newDescription);
        character.setPromptTemplate(template.getPromptTemplate());
        character.setMbti(template.getMbti());
        character.setMindConfig(template.getMindConfig());

        if (template.getAllowedTools() != null) {
            character.setAllowedTools(new HashSet<>(template.getAllowedTools()));
        }
        if (template.getDefaultTools() != null) {
            character.setDefaultTools(List.copyOf(template.getDefaultTools()));
        }

        Map<String, Object> extensions = new HashMap<>();
        extensions.put("source", "template_derived");
        extensions.put("templateId", template.getId());
        character.setExtensions(extensions);

        return characterService.createCharacter(character);
    }

    @Override
    public Object createDraft(CreateContext request) {
        Character character = new Character();
        character.setName(request.getName());
        character.setDescription(request.getDescription());

        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) request.getConfig();
        if (config != null) {
            Object promptTemplate = config.get("promptTemplate");
            if (promptTemplate instanceof String) {
                character.setPromptTemplate((String) promptTemplate);
            }
            Object mbti = config.get("mbti");
            if (mbti instanceof String) {
                character.setMbti((String) mbti);
            }
            Object mindConfig = config.get("mindConfig");
            if (mindConfig instanceof CharacterProfile.MindConfig) {
                character.setMindConfig((CharacterProfile.MindConfig) mindConfig);
            }
            Object allowedTools = config.get("allowedTools");
            if (allowedTools instanceof List) {
                character.setAllowedTools(new HashSet<>((List<String>) allowedTools));
            }
            Object defaultTools = config.get("defaultTools");
            if (defaultTools instanceof List) {
                character.setDefaultTools(List.copyOf((List<String>) defaultTools));
            }
        }

        Map<String, Object> extensions = new HashMap<>();
        extensions.put("source", "template_draft");
        character.setExtensions(extensions);

        return characterService.createCharacter(character);
    }
}
