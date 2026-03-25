package org.dragon.memory.provider;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class EmbeddingProviderFactory {

    private final Map<String, EmbeddingProvider> providerCache = new HashMap<>();

    public EmbeddingProvider getProvider(String providerName, String model) {
        String cacheKey = providerName + ":" + model;
        if (providerCache.containsKey(cacheKey)) {
            return providerCache.get(cacheKey);
        }

        // 根据 providerName 和 model 创建对应的 EmbeddingProvider 实例
        EmbeddingProvider provider = createProvider(providerName, model);
        providerCache.put(cacheKey, provider);
        return provider;
    }

    private EmbeddingProvider createProvider(String providerName, String model) {
        // 简单实现：目前只支持 dummy 提供者
        // 实际应用中需要根据 providerName 和 model 创建不同的提供者
        return new DummyEmbeddingProvider();
    }
}
