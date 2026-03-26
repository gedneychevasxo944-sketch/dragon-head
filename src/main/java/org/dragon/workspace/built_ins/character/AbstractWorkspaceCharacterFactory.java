package org.dragon.workspace.built_ins.character;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.character.Character;
import org.dragon.character.CharacterFactory;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.CharacterRuntimeBinder;
import org.dragon.character.profile.CharacterProfile;
import org.dragon.workspace.WorkspaceRegistry;

import lombok.extern.slf4j.Slf4j;

/**
 * Workspace Character 工厂抽象基类
 * 收口 workspace 校验、实例创建通用流程、runtime bind、registry register、getOrCreate/has 等逻辑
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
public abstract class AbstractWorkspaceCharacterFactory implements CharacterFactory<Character> {

    protected final CharacterRegistry characterRegistry;
    protected final WorkspaceRegistry workspaceRegistry;
    protected final CharacterRuntimeBinder characterRuntimeBinder;

    protected AbstractWorkspaceCharacterFactory(CharacterRegistry characterRegistry,
                                               WorkspaceRegistry workspaceRegistry,
                                               CharacterRuntimeBinder characterRuntimeBinder) {
        this.characterRegistry = characterRegistry;
        this.workspaceRegistry = workspaceRegistry;
        this.characterRuntimeBinder = characterRuntimeBinder;
    }

    /**
     * 返回 Character ID 前缀
     */
    protected abstract String getCharacterIdPrefix();

    /**
     * 返回 Character 类型
     */
    @Override
    public abstract String getCharacterType();

    /**
     * 返回 Character 名称
     */
    protected abstract String getCharacterName();

    /**
     * 返回 Character 描述
     */
    protected abstract String getCharacterDescription();

    /**
     * 返回可用工具列表（子类可选覆盖）
     */
    @Override
    public List<ToolConnector> getAvailableTools() {
        return List.of();
    }

    /**
     * 创建 Character 实例（子类可覆盖）
     */
    protected Character createCharacterInstance(String characterId, String workspaceId) {
        Character character = new Character();
        character.getProfile().setId(characterId);
        character.getProfile().setName(getCharacterName());
        character.getProfile().setDescription(getCharacterDescription());
        character.getProfile().setStatus(CharacterProfile.Status.RUNNING);
        character.getProfile().setWorkspaceIds(List.of(workspaceId));
        character.getProfile().setVersion(1);
        character.getProfile().setExtensions(new ConcurrentHashMap<>());
        character.getProfile().setAllowedTools(new HashSet<>());
        character.getProfile().setCreatedAt(LocalDateTime.now());
        character.getProfile().setUpdatedAt(LocalDateTime.now());
        buildCharacterExtensions(character, workspaceId);
        return character;
    }

    /**
     * 构建 Character 的额外字段（子类可选覆盖）
     * 如 allowedTools 等扩展字段
     */
    protected void buildCharacterExtensions(Character character, String workspaceId) {
        // 默认空实现
    }

    @Override
    public Character createCharacter(String workspaceId) {
        String characterId = getCharacterId(workspaceId);

        // 验证 Workspace 存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        // 创建 Character 实例
        Character character = createCharacterInstance(characterId, workspaceId);

        // 绑定运行时依赖
        characterRuntimeBinder.bind(character, workspaceId);

        // 注册到 CharacterRegistry
        characterRegistry.register(character);

        log.info("[{}] Created {} character {} for workspace {}",
                getClass().getSimpleName(), getCharacterType(), characterId, workspaceId);

        return character;
    }

    @Override
    public final Character getOrCreateCharacter(String workspaceId) {
        String characterId = getCharacterId(workspaceId);
        return characterRegistry.get(characterId)
                .orElseGet(() -> createCharacter(workspaceId));
    }

    @Override
    public final boolean hasCharacter(String workspaceId) {
        return characterRegistry.exists(getCharacterId(workspaceId));
    }

    /**
     * 获取 Character ID
     */
    public String getCharacterId(String workspaceId) {
        return getCharacterIdPrefix() + workspaceId;
    }
}
