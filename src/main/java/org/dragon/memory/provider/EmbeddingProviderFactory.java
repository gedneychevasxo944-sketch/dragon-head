package org.dragon.memory.provider;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * EmbeddingProvider 工厂类
 * 用于创建和缓存不同类型的 EmbeddingProvider 实例
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class EmbeddingProviderFactory {

    private final Map<String, EmbeddingProvider> providerCache = new HashMap<>();
    private final KimiEmbeddingProvider kimiEmbeddingProvider;

    public EmbeddingProviderFactory(KimiEmbeddingProvider kimiEmbeddingProvider) {
        this.kimiEmbeddingProvider = kimiEmbeddingProvider;
    }

    public static EmbeddingProvider create(String providerName, String model) {
        // 根据 providerName 创建对应的 EmbeddingProvider 实例
        if ("kimi".equalsIgnoreCase(providerName)) {
            return new KimiEmbeddingProvider();
        } else if ("dummy".equalsIgnoreCase(providerName)) {
            return new DummyEmbeddingProvider();
        } else {
            // 默认返回 dummy 提供者
            return new DummyEmbeddingProvider();
        }
    }

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
        // 根据 providerName 创建对应的 EmbeddingProvider 实例
        if ("kimi".equalsIgnoreCase(providerName)) {
            return kimiEmbeddingProvider;
        } else if ("dummy".equalsIgnoreCase(providerName)) {
            return new DummyEmbeddingProvider();
        } else {
            // 默认返回 dummy 提供者
            return new DummyEmbeddingProvider();
        }
    }
}
