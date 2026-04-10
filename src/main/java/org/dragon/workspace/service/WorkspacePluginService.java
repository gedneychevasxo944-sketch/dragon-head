package org.dragon.workspace.service;

import java.util.Map;

import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.workspace.member.TeamPosition;
import org.dragon.workspace.plugin.PluginResult;
import org.dragon.workspace.plugin.WorkspaceBuiltinPlugin;
import org.dragon.workspace.plugin.WorkspaceBuiltinPluginRegistry;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Workspace Plugin 服务
 * 统一管理 Built-in Character Plugin 的执行
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspacePluginService {

    private final WorkspaceBuiltinPluginRegistry pluginRegistry;
    private final CharacterRegistry characterRegistry;

    /**
     * 通过 TeamPosition 执行 built-in 角色
     *
     * @param workspaceId 工作空间 ID
     * @param position TeamPosition
     * @param input 任务输入
     * @return 执行结果
     */
    public PluginResult executeFromTeamPosition(
            String workspaceId,
            TeamPosition position,
            Map<String, Object> input) {

        if (!position.isBuiltin()) {
            // 非 built-in，视为普通 Character
            Character character = characterRegistry.get(position.getAssignedCharacterId())
                    .orElseThrow(() -> new IllegalArgumentException("Character not found: " + position.getAssignedCharacterId()));
            // 对于普通 Character，直接返回 success（调用方需要自行调用）
            return PluginResult.success("Character found: " + character.getId());
        }

        // Built-in 类型
        WorkspaceBuiltinPlugin plugin = pluginRegistry.get(position.getBuiltinType());
        if (plugin == null) {
            throw new IllegalArgumentException("Unknown builtin type: " + position.getBuiltinType());
        }

        return plugin.execute(workspaceId, input);
    }

    /**
     * 执行指定类型的 Built-in Plugin
     *
     * @param workspaceId 工作空间 ID
     * @param builtinType Built-in 类型
     * @param input 任务输入
     * @return 执行结果
     */
    public PluginResult executeBuiltin(
            String workspaceId,
            String builtinType,
            Map<String, Object> input) {

        WorkspaceBuiltinPlugin plugin = pluginRegistry.get(builtinType);
        if (plugin == null) {
            return PluginResult.failure("Unknown builtin type: " + builtinType);
        }

        return plugin.execute(workspaceId, input);
    }

    /**
     * 获取 Built-in Character
     *
     * @param workspaceId 工作空间 ID
     * @param builtinType Built-in 类型
     * @return Character
     */
    public Character getBuiltinCharacter(String workspaceId, String builtinType) {
        WorkspaceBuiltinPlugin plugin = pluginRegistry.get(builtinType);
        if (plugin == null) {
            throw new IllegalArgumentException("Unknown builtin type: " + builtinType);
        }
        return plugin.getOrCreateCharacter(workspaceId, null);
    }

    /**
     * 为 Workspace 初始化所有 built-in 插件
     *
     * @param workspaceId 工作空间 ID
     */
    public void initializeWorkspacePlugins(String workspaceId) {
        pluginRegistry.getAllPluginCharacters(workspaceId);
        log.info("[WorkspacePluginService] Initialized plugins for workspace {}", workspaceId);
    }

    /**
     * 检查是否为有效的 Built-in 类型
     */
    public boolean isValidBuiltinType(String builtinType) {
        return pluginRegistry.has(builtinType);
    }
}