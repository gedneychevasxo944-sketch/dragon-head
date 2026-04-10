package org.dragon.workspace.plugin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.dragon.character.Character;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Built-in Plugin 注册表
 * 统一管理所有 Workspace 内置插件
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceBuiltinPluginRegistry {

    private final List<WorkspaceBuiltinPlugin> plugins;
    private final Map<String, WorkspaceBuiltinPlugin> pluginMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        for (WorkspaceBuiltinPlugin plugin : plugins) {
            register(plugin);
        }
        log.info("[WorkspaceBuiltinPluginRegistry] Registered {} built-in plugins: {}",
                pluginMap.size(), pluginMap.keySet());
    }

    /**
     * 注册插件
     */
    public void register(WorkspaceBuiltinPlugin plugin) {
        pluginMap.put(plugin.getType(), plugin);
        log.info("[WorkspaceBuiltinPluginRegistry] Registered plugin: {}", plugin.getType());
    }

    /**
     * 获取插件
     */
    public WorkspaceBuiltinPlugin get(String type) {
        return pluginMap.get(type);
    }

    /**
     * 检查插件是否存在
     */
    public boolean has(String type) {
        return pluginMap.containsKey(type);
    }

    /**
     * 获取 Workspace 的所有插件 Character
     *
     * @param workspaceId 工作空间 ID
     * @return 类型到 Character 的映射
     */
    public Map<String, Character> getAllPluginCharacters(String workspaceId) {
        return pluginMap.values().stream()
                .filter(WorkspaceBuiltinPlugin::isEnabled)
                .collect(Collectors.toMap(
                        WorkspaceBuiltinPlugin::getType,
                        p -> p.getOrCreateCharacter(workspaceId, null)
                ));
    }

    /**
     * 获取所有插件类型
     */
    public List<String> getPluginTypes() {
        return List.copyOf(pluginMap.keySet());
    }
}