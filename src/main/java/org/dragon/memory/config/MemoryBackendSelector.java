package org.dragon.memory.config;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MemoryBackendSelector {

    public ResolvedMemoryBackendConfig selectBackend(Map<String, Object> rawConfig) {
        MemoryRuntimeConfigResolver resolver = new MemoryRuntimeConfigResolver();
        ResolvedMemorySearchConfig searchConfig = resolver.resolve(rawConfig);

        String backend = (String) rawConfig.getOrDefault("backend", "builtin");
        boolean fallbackEnabled = true;

        return new ResolvedMemoryBackendConfig(backend, searchConfig);
    }
}