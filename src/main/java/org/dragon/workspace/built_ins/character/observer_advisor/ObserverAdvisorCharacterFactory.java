package org.dragon.workspace.built_ins.character.observer_advisor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.character.Character;
import org.dragon.character.CharacterFactory;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.CharacterRuntimeBinder;
import org.dragon.character.profile.CharacterProfile;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.built_ins.character.observer_advisor.tool.ObserverAdvisorCharacterTools;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * ObserverAdvisor Character 工厂
 * 支持三种粒度的 ObserverAdvisor Character:
 * - global: observer_advisor (全局优化建议)
 * - workspace-scoped: observer_advisor:workspace:{workspaceId}
 * - character-scoped: observer_advisor:character:{characterId}
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class ObserverAdvisorCharacterFactory implements CharacterFactory<Character> {

    public static final String OBSERVER_ADVISOR_TYPE = "observer_advisor";
    public static final String GLOBAL_KEY = "observer_advisor";
    public static final String WORKSPACE_KEY_PREFIX = "observer_advisor:workspace:";
    public static final String CHARACTER_KEY_PREFIX = "observer_advisor:character:";

    private final CharacterRegistry characterRegistry;
    private final WorkspaceRegistry workspaceRegistry;
    private final CharacterRuntimeBinder characterRuntimeBinder;
    private final ObserverAdvisorCharacterTools tools;

    public ObserverAdvisorCharacterFactory(
            CharacterRegistry characterRegistry,
            WorkspaceRegistry workspaceRegistry,
            CharacterRuntimeBinder characterRuntimeBinder,
            ObserverAdvisorCharacterTools tools) {
        this.characterRegistry = characterRegistry;
        this.workspaceRegistry = workspaceRegistry;
        this.characterRuntimeBinder = characterRuntimeBinder;
        this.tools = tools;
    }

    @Override
    public String getCharacterType() {
        return OBSERVER_ADVISOR_TYPE;
    }

    /**
     * 获取 Global ObserverAdvisor Character
     */
    public Character getOrCreateGlobal() {
        return characterRegistry.get(GLOBAL_KEY)
                .orElseGet(() -> createGlobalCharacter());
    }

    /**
     * 获取 Workspace-scoped ObserverAdvisor Character
     */
    public Character getOrCreateForWorkspace(String workspaceId) {
        String characterId = WORKSPACE_KEY_PREFIX + workspaceId;
        return characterRegistry.get(characterId)
                .orElseGet(() -> createWorkspaceCharacter(workspaceId));
    }

    /**
     * 获取 Character-scoped ObserverAdvisor Character
     */
    public Character getOrCreateForCharacter(String characterId) {
        String id = CHARACTER_KEY_PREFIX + characterId;
        return characterRegistry.get(id)
                .orElseGet(() -> createCharacterScopedCharacter(characterId));
    }

    /**
     * 检查 Global Character 是否存在
     */
    public boolean hasGlobal() {
        return characterRegistry.exists(GLOBAL_KEY);
    }

    /**
     * 检查 Workspace-scoped Character 是否存在
     */
    public boolean hasForWorkspace(String workspaceId) {
        return characterRegistry.exists(WORKSPACE_KEY_PREFIX + workspaceId);
    }

    /**
     * 检查 Character-scoped Character 是否存在
     */
    public boolean hasForCharacter(String characterId) {
        return characterRegistry.exists(CHARACTER_KEY_PREFIX + characterId);
    }

    @Override
    public Character createCharacter(String workspaceId) {
        return getOrCreateForWorkspace(workspaceId);
    }

    @Override
    public Character getOrCreateCharacter(String workspaceId) {
        return getOrCreateForWorkspace(workspaceId);
    }

    @Override
    public boolean hasCharacter(String workspaceId) {
        return hasForWorkspace(workspaceId);
    }

    /**
     * 获取可用工具列表
     */
    @Override
    public List<ToolConnector> getAvailableTools() {
        return tools.getAvailableTools();
    }

    // ========== 私有方法 ==========

    private Character createGlobalCharacter() {
        Character character = new Character();
        character.getProfile().setId(GLOBAL_KEY);
        character.getProfile().setName("Observer Advisor (Global)");
        character.getProfile().setDescription("全局优化顾问，提供系统级的优化建议");
        character.getProfile().setStatus(CharacterProfile.Status.RUNNING);
        character.getProfile().setWorkspaceIds(List.of());
        character.getProfile().setVersion(1);
        character.getProfile().setExtensions(new ConcurrentHashMap<>());
        character.getProfile().setAllowedTools(collectToolNames());
        character.getProfile().setCreatedAt(LocalDateTime.now());
        character.getProfile().setUpdatedAt(LocalDateTime.now());

        characterRuntimeBinder.bind(character, null);
        characterRegistry.register(character);

        log.info("[ObserverAdvisorCharacterFactory] Created global observer_advisor character");
        return character;
    }

    private Character createWorkspaceCharacter(String workspaceId) {
        String characterId = WORKSPACE_KEY_PREFIX + workspaceId;

        // 验证 Workspace 存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        Character character = new Character();
        character.getProfile().setId(characterId);
        character.getProfile().setName("Observer Advisor");
        character.getProfile().setDescription("Workspace 优化顾问，为指定 Workspace 提供优化建议");
        character.getProfile().setStatus(CharacterProfile.Status.RUNNING);
        character.getProfile().setWorkspaceIds(List.of(workspaceId));
        character.getProfile().setVersion(1);
        character.getProfile().setExtensions(new ConcurrentHashMap<>());
        character.getProfile().setAllowedTools(collectToolNames());
        character.getProfile().setCreatedAt(LocalDateTime.now());
        character.getProfile().setUpdatedAt(LocalDateTime.now());

        characterRuntimeBinder.bind(character, workspaceId);
        characterRegistry.register(character);

        log.info("[ObserverAdvisorCharacterFactory] Created workspace-scoped observer_advisor character {} for workspace {}",
                characterId, workspaceId);
        return character;
    }

    private Character createCharacterScopedCharacter(String characterId) {
        String id = CHARACTER_KEY_PREFIX + characterId;

        Character character = new Character();
        character.getProfile().setId(id);
        character.getProfile().setName("Observer Advisor");
        character.getProfile().setDescription("针对特定 Character 的优化顾问");
        character.getProfile().setStatus(CharacterProfile.Status.RUNNING);
        character.getProfile().setWorkspaceIds(List.of());
        character.getProfile().setVersion(1);
        character.getProfile().setExtensions(new ConcurrentHashMap<>());
        character.getProfile().setAllowedTools(collectToolNames());
        character.getProfile().setCreatedAt(LocalDateTime.now());
        character.getProfile().setUpdatedAt(LocalDateTime.now());

        characterRuntimeBinder.bind(character, null);
        characterRegistry.register(character);

        log.info("[ObserverAdvisorCharacterFactory] Created character-scoped observer_advisor character {} for character {}",
                id, characterId);
        return character;
    }

    private Set<String> collectToolNames() {
        Set<String> names = new HashSet<>();
        for (ToolConnector tool : tools.getAvailableTools()) {
            if (tool != null && tool.getName() != null) {
                names.add(tool.getName());
            }
        }
        return names;
    }
}