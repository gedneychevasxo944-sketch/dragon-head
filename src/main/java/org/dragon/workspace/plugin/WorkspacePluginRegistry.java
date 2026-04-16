package org.dragon.workspace.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * WorkspacePlugin 注册表
 *
 * <p>管理所有 WorkspacePlugin，按 Step 名称索引。
 *
 * @author yijunw
 */
@Component
public class WorkspacePluginRegistry {

    /**
     * stepName -> plugins
     */
    private final Map<String, List<WorkspacePlugin>> stepPlugins = new ConcurrentHashMap<>();

    /**
     * 注册插件
     */
    public void register(WorkspacePlugin plugin) {
        if (plugin == null || !plugin.isEnabled()) {
            return;
        }
        for (String stepName : plugin.getTargetSteps()) {
            stepPlugins.computeIfAbsent(stepName, k -> new ArrayList<>()).add(plugin);
        }
    }

    /**
     * 获取某个 Step 关联的所有插件
     */
    public List<WorkspacePlugin> getPlugins(String stepName) {
        return stepPlugins.getOrDefault(stepName, List.of());
    }

    /**
     * 获取某个 Step 关联的插件，按名称过滤
     */
    public List<WorkspacePlugin> getPlugins(String stepName, String pluginName) {
        return stepPlugins.getOrDefault(stepName, List.of()).stream()
                .filter(p -> p.getName().equals(pluginName))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有已注册的插件
     */
    public List<WorkspacePlugin> getAllPlugins() {
        return stepPlugins.values().stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());
    }
}
