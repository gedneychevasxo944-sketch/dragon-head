package org.dragon.memory;

import org.dragon.memory.config.MemoryBackendSelector;
import org.dragon.memory.config.ResolvedMemoryBackendConfig;
import org.dragon.memory.config.ResolvedMemorySearchConfig;
import org.dragon.memory.impl.FallbackMemorySearchManager;
import org.dragon.memory.impl.MemoryIndexManager;
import org.dragon.memory.impl.QmdMemoryManager;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MemorySearchManagerFactory {

    private static final Map<String, MemorySearchManager> managerCache = new ConcurrentHashMap<>();
    private final MemoryBackendSelector backendSelector;

    public MemorySearchManagerFactory(MemoryBackendSelector backendSelector) {
        this.backendSelector = backendSelector;
    }

    public MemorySearchManager getMemorySearchManager(Map<String, Object> config) {
        String configKey = generateConfigKey(config);

        if (managerCache.containsKey(configKey)) {
            return managerCache.get(configKey);
        }

        ResolvedMemoryBackendConfig backendConfig = backendSelector.selectBackend(config);
        MemorySearchManager manager;

        if ("qmd".equals(backendConfig.getBackend())) {
            try {
                manager = new QmdMemoryManager(backendConfig.getSearchConfig());
            } catch (Exception e) {
                // Qmd 初始化失败，使用 builtin 作为 fallback
                manager = new FallbackMemorySearchManager(
                        new QmdMemoryManager(backendConfig.getSearchConfig()),
                        new MemoryIndexManager(backendConfig.getSearchConfig())
                );
            }
        } else {
            manager = new MemoryIndexManager(backendConfig.getSearchConfig());
        }

        managerCache.put(configKey, manager);
        return manager;
    }

    // 清理缓存的方法，用于配置变更时
    public void clearCache() {
        // 先关闭所有 manager 以释放资源
        for (MemorySearchManager manager : managerCache.values()) {
            try {
                manager.close();
            } catch (Exception e) {
                System.err.println("Error closing manager: " + e.getMessage());
            }
        }
        managerCache.clear();
    }

    private String generateConfigKey(Map<String, Object> config) {
        // 简单的配置哈希生成，实际项目中应使用更健壮的方法
        // 将配置转换为字符串并进行哈希计算
        StringBuilder keyBuilder = new StringBuilder();
        config.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> keyBuilder.append(entry.getKey()).append(":").append(entry.getValue()).append(";"));
        return Integer.toHexString(keyBuilder.toString().hashCode());
    }
}