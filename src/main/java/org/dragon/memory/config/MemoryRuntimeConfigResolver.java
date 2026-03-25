package org.dragon.memory.config;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MemoryRuntimeConfigResolver {

    public ResolvedMemorySearchConfig resolve(Map<String, Object> rawConfig) {
        ResolvedMemorySearchConfig config = new ResolvedMemorySearchConfig();
        // TODO: 实现配置解析逻辑
        return config;
    }
}