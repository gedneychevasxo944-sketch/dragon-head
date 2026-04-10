package org.dragon.workspace.plugin;

import org.dragon.character.Character;

import java.util.Map;

/**
 * Workspace 内置角色插件接口
 * 所有 Built-in Character 以插件形式注册到 Workspace
 *
 * @author wyj
 * @version 1.0
 */
public interface WorkspaceBuiltinPlugin {

    /**
     * 插件类型唯一标识
     * 对应 BuiltInCharacterDefinition.type
     */
    String getType();

    /**
     * 获取插件对应的 Character
     * 根据 workspaceId 和可选的 characterId 创建/获取 Character
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID（可选，用于 character-scoped）
     * @return Character 实例
     */
    Character getOrCreateCharacter(String workspaceId, String characterId);

    /**
     * 执行插件任务
     *
     * @param workspaceId 工作空间 ID
     * @param input 任务输入
     * @return 执行结果
     */
    PluginResult execute(String workspaceId, Map<String, Object> input);

    /**
     * 获取插件描述
     */
    default String getDescription() {
        return "Workspace built-in plugin: " + getType();
    }

    /**
     * 插件是否启用
     */
    default boolean isEnabled() {
        return true;
    }
}