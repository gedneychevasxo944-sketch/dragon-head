package org.dragon.character.builtin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.character.Character;
import org.dragon.character.CharacterFactory;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.CharacterRuntimeBinder;
import org.dragon.character.profile.CharacterProfile;
import org.dragon.tools.AgentTool;
import org.dragon.tools.ToolRegistry;
import org.dragon.workspace.WorkspaceRegistry;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一的 Built-in Character 工厂
 * 基于 BuiltInCharacterRegistry 的配置动态创建 character
 * 支持 4 种 scope: global, workspace, character, workspace+character
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BuiltInCharacterFactory implements CharacterFactory<Character> {

    private final CharacterRegistry characterRegistry;
    private final WorkspaceRegistry workspaceRegistry;
    private final @Lazy CharacterRuntimeBinder characterRuntimeBinder;
    private final ToolRegistry toolRegistry;

    /** Observer Advisor 的特殊 scope 前缀 */
    public static final String GLOBAL_KEY = "observer_advisor";
    public static final String WORKSPACE_KEY_PREFIX = "observer_advisor:workspace:";
    public static final String CHARACTER_KEY_PREFIX = "observer_advisor:character:";

    @Override
    public String getCharacterType() {
        // 统一工厂不绑定单一类型，支持多种类型
        return "builtin";
    }

    @Override
    public Character createCharacter(String workspaceId) {
        throw new UnsupportedOperationException("Use scoped methods: getOrCreateGlobal(), getOrCreateForWorkspace(), etc.");
    }

    @Override
    public Character getOrCreateCharacter(String workspaceId) {
        throw new UnsupportedOperationException("Use scoped methods: getOrCreateGlobal(), getOrCreateForWorkspace(), etc.");
    }

    @Override
    public boolean hasCharacter(String workspaceId) {
        throw new UnsupportedOperationException("Use scoped methods: hasGlobal(), hasForWorkspace(), etc.");
    }

    @Override
    public List<AgentTool> getAvailableTools() {
        // 统一工厂返回空，具体工具由各 scoped 工厂提供
        return List.of();
    }

    // ==================== 全局 scope ====================

    /**
     * 获取或创建全局 Character（仅支持 observer_advisor）
     */
    public Character getOrCreateGlobal(String type) {
        if (!"observer_advisor".equals(type)) {
            throw new IllegalArgumentException("Global scope only supports observer_advisor, got: " + type);
        }
        String characterId = GLOBAL_KEY;
        return characterRegistry.get(characterId)
                .orElseGet(() -> createGlobalCharacter(type));
    }

    public boolean hasGlobal(String type) {
        if (!"observer_advisor".equals(type)) {
            return false;
        }
        return characterRegistry.exists(GLOBAL_KEY);
    }

    private Character createGlobalCharacter(String type) {
        BuiltInCharacterDefinition definition = BuiltInCharacterRegistry.getDefinition(type)
                .orElseThrow(() -> new IllegalArgumentException("Unknown character type: " + type));

        Character character = new Character();
        character.getProfile().setId(GLOBAL_KEY);
        character.getProfile().setName(definition.name());
        character.getProfile().setDescription(definition.description());
        character.getProfile().setStatus(CharacterProfile.Status.RUNNING);
        character.getProfile().setExtensions(new ConcurrentHashMap<>());
        character.getProfile().setAllowedTools(collectToolNames(definition.toolNames()));
        character.getProfile().setCreatedAt(LocalDateTime.now());
        character.getProfile().setUpdatedAt(LocalDateTime.now());

        characterRuntimeBinder.bind(character, null);
        characterRegistry.register(character);

        log.info("[BuiltInCharacterFactory] Created global {} character: {}", type, GLOBAL_KEY);
        return character;
    }

    // ==================== Workspace scope ====================

    /**
     * 获取或创建 Workspace-scoped Character
     */
    public Character getOrCreateForWorkspace(String type, String workspaceId) {
        // 验证 Workspace 存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        String characterId = buildWorkspaceCharacterId(type, workspaceId);
        return characterRegistry.get(characterId)
                .orElseGet(() -> createWorkspaceCharacter(type, workspaceId));
    }

    public boolean hasForWorkspace(String type, String workspaceId) {
        String characterId = buildWorkspaceCharacterId(type, workspaceId);
        return characterRegistry.exists(characterId);
    }

    private Character createWorkspaceCharacter(String type, String workspaceId) {
        BuiltInCharacterDefinition definition = BuiltInCharacterRegistry.getDefinition(type)
                .orElseThrow(() -> new IllegalArgumentException("Unknown character type: " + type));

        String characterId = buildWorkspaceCharacterId(type, workspaceId);

        Character character = new Character();
        character.getProfile().setId(characterId);
        character.getProfile().setName(definition.name());
        character.getProfile().setDescription(definition.description());
        character.getProfile().setStatus(CharacterProfile.Status.RUNNING);
        character.getProfile().setExtensions(new ConcurrentHashMap<>());
        character.getProfile().setAllowedTools(collectToolNames(definition.toolNames()));
        character.getProfile().setCreatedAt(LocalDateTime.now());
        character.getProfile().setUpdatedAt(LocalDateTime.now());

        characterRuntimeBinder.bind(character, workspaceId);
        characterRegistry.register(character);

        log.info("[BuiltInCharacterFactory] Created workspace-scoped {} character {} for workspace {}",
                type, characterId, workspaceId);
        return character;
    }

    // ==================== Character scope (independent of workspace) ====================

    /**
     * 获取或创建 Character-scoped Character（独立于 workspace）
     */
    public Character getOrCreateForCharacter(String type, String characterId) {
        String fullCharacterId = buildCharacterScopedId(type, characterId);
        return characterRegistry.get(fullCharacterId)
                .orElseGet(() -> createCharacterScopedCharacter(type, characterId));
    }

    public boolean hasForCharacter(String type, String characterId) {
        String fullCharacterId = buildCharacterScopedId(type, characterId);
        return characterRegistry.exists(fullCharacterId);
    }

    private Character createCharacterScopedCharacter(String type, String targetCharacterId) {
        BuiltInCharacterDefinition definition = BuiltInCharacterRegistry.getDefinition(type)
                .orElseThrow(() -> new IllegalArgumentException("Unknown character type: " + type));

        String fullCharacterId = buildCharacterScopedId(type, targetCharacterId);

        Character character = new Character();
        character.getProfile().setId(fullCharacterId);
        character.getProfile().setName(definition.name());
        character.getProfile().setDescription(definition.description());
        character.getProfile().setStatus(CharacterProfile.Status.RUNNING);
        character.getProfile().setExtensions(new ConcurrentHashMap<>());
        character.getProfile().setAllowedTools(collectToolNames(definition.toolNames()));
        character.getProfile().setCreatedAt(LocalDateTime.now());
        character.getProfile().setUpdatedAt(LocalDateTime.now());

        characterRuntimeBinder.bind(character, null);
        characterRegistry.register(character);

        log.info("[BuiltInCharacterFactory] Created character-scoped {} character {} for character {}",
                type, fullCharacterId, targetCharacterId);
        return character;
    }

    // ==================== Workspace + Character scope ====================

    /**
     * 获取或创建 Workspace+Character-scoped Character（最高优先级）
     */
    public Character getOrCreateForWorkspaceCharacter(String type, String workspaceId, String characterId) {
        // 验证 Workspace 存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        String fullCharacterId = buildWorkspaceCharacterScopedId(type, workspaceId, characterId);
        return characterRegistry.get(fullCharacterId)
                .orElseGet(() -> createWorkspaceCharacterScopedCharacter(type, workspaceId, characterId));
    }

    public boolean hasForWorkspaceCharacter(String type, String workspaceId, String characterId) {
        String fullCharacterId = buildWorkspaceCharacterScopedId(type, workspaceId, characterId);
        return characterRegistry.exists(fullCharacterId);
    }

    private Character createWorkspaceCharacterScopedCharacter(String type, String workspaceId, String characterId) {
        BuiltInCharacterDefinition definition = BuiltInCharacterRegistry.getDefinition(type)
                .orElseThrow(() -> new IllegalArgumentException("Unknown character type: " + type));

        String fullCharacterId = buildWorkspaceCharacterScopedId(type, workspaceId, characterId);

        Character character = new Character();
        character.getProfile().setId(fullCharacterId);
        character.getProfile().setName(definition.name());
        character.getProfile().setDescription(definition.description());
        character.getProfile().setStatus(CharacterProfile.Status.RUNNING);
        character.getProfile().setExtensions(new ConcurrentHashMap<>());
        character.getProfile().setAllowedTools(collectToolNames(definition.toolNames()));
        character.getProfile().setCreatedAt(LocalDateTime.now());
        character.getProfile().setUpdatedAt(LocalDateTime.now());

        characterRuntimeBinder.bind(character, workspaceId);
        characterRegistry.register(character);

        log.info("[BuiltInCharacterFactory] Created workspace+character-scoped {} character {} for workspace {} / character {}",
                type, fullCharacterId, workspaceId, characterId);
        return character;
    }

    // ==================== 通用方法 ====================

    /**
     * 获取指定类型的可用工具列表
     */
    public List<AgentTool> getAvailableToolsForType(String type) {
        BuiltInCharacterDefinition definition = BuiltInCharacterRegistry.getDefinition(type)
                .orElseThrow(() -> new IllegalArgumentException("Unknown character type: " + type));

        return definition.toolNames().stream()
                .map(toolRegistry::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * 根据工具名称集合收集可用的工具名
     */
    private Set<String> collectToolNames(List<String> toolNames) {
        Set<String> names = new HashSet<>();
        if (toolNames == null) {
            return names;
        }
        for (String toolName : toolNames) {
            if (toolRegistry.get(toolName).isPresent()) {
                names.add(toolName);
            } else {
                log.warn("[BuiltInCharacterFactory] Tool not found in registry: {}", toolName);
            }
        }
        return names;
    }

    // ==================== ID 构建方法 ====================

    private String buildWorkspaceCharacterId(String type, String workspaceId) {
        BuiltInCharacterDefinition definition = BuiltInCharacterRegistry.getDefinition(type)
                .orElseThrow(() -> new IllegalArgumentException("Unknown character type: " + type));
        return definition.idPrefix() + workspaceId;
    }

    private String buildCharacterScopedId(String type, String characterId) {
        return type + ":character:" + characterId;
    }

    private String buildWorkspaceCharacterScopedId(String type, String workspaceId, String characterId) {
        return type + ":workspace:" + workspaceId + ":character:" + characterId;
    }

    // ==================== 向后兼容方法 ====================

    /**
     * 获取 HR Character（向后兼容）
     */
    public Character getOrCreateHrCharacter(String workspaceId) {
        return getOrCreateForWorkspace("hr", workspaceId);
    }

    /**
     * 获取 MemberSelector Character（向后兼容）
     */
    public Character getOrCreateMemberSelectorCharacter(String workspaceId) {
        return getOrCreateForWorkspace("member_selector", workspaceId);
    }

    /**
     * 获取 ProjectManager Character（向后兼容）
     */
    public Character getOrCreateProjectManagerCharacter(String workspaceId) {
        return getOrCreateForWorkspace("project_manager", workspaceId);
    }

    /**
     * 获取 PromptWriter Character（向后兼容）
     */
    public Character getOrCreatePromptWriterCharacter(String workspaceId) {
        return getOrCreateForWorkspace("prompt_writer", workspaceId);
    }

    /**
     * 获取 CommonSenseWriter Character（向后兼容）
     */
    public Character getOrCreateCommonSenseWriterCharacter(String workspaceId) {
        return getOrCreateForWorkspace("commonsense_writer", workspaceId);
    }

    /**
     * 获取 MaterialSummary Character（向后兼容）
     */
    public Character getOrCreateMaterialSummaryCharacter(String workspaceId) {
        return getOrCreateForWorkspace("material_summary", workspaceId);
    }

    /**
     * 获取 ObserverAdvisor Character 的 3 种 scope（向后兼容）
     */
    public Character getOrCreateObserverAdvisorGlobal() {
        return getOrCreateGlobal("observer_advisor");
    }

    public Character getOrCreateObserverAdvisorForWorkspace(String workspaceId) {
        return getOrCreateForWorkspace("observer_advisor", workspaceId);
    }

    public Character getOrCreateObserverAdvisorForCharacter(String characterId) {
        return getOrCreateForCharacter("observer_advisor", characterId);
    }
}
