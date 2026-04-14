package org.dragon.tool.runtime.adapter;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LLM 适配器注册表。
 *
 * <p>统一管理所有 {@link LlmToolAdapter} 实现，按厂商标识（provider）注册和查找。
 * 应用启动时将 {@link AnthropicToolAdapter} 和 {@link OpenAiToolAdapter} 预注册，
 * 后续可通过 {@link #register(LlmToolAdapter)} 动态添加新的厂商适配器。
 *
 * <p><b>使用方式</b>：
 * <pre>
 * LlmAdapterRegistry registry = LlmAdapterRegistry.createDefault();
 * LlmToolAdapter adapter = registry.getAdapter("anthropic");
 * JsonNode llmFormat = adapter.toProviderFormat(declaration);
 * </pre>
 */
@Component
public class LlmAdapterRegistry {

    /** 厂商标识 → 适配器实例，线程安全 */
    private final Map<String, LlmToolAdapter> adapters = new ConcurrentHashMap<>();

    /** 私有构造，使用工厂方法创建 */
    private LlmAdapterRegistry() {
    }

    /**
     * 创建并初始化默认注册表，预注册 Anthropic 和 OpenAI 适配器。
     *
     * @return 已初始化的注册表实例
     */
    public static LlmAdapterRegistry createDefault() {
        LlmAdapterRegistry registry = new LlmAdapterRegistry();
        registry.register(new AnthropicToolAdapter());
        registry.register(new OpenAiToolAdapter());
        return registry;
    }

    /**
     * 注册一个 LLM 适配器。
     * 若已存在相同 provider 的适配器，则覆盖。
     *
     * @param adapter 适配器实现
     */
    public void register(LlmToolAdapter adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("adapter must not be null");
        }
        adapters.put(adapter.supportedProvider(), adapter);
    }

    /**
     * 根据厂商标识获取适配器。
     *
     * @param provider LLM 厂商标识，如 {@code "anthropic"}、{@code "openai"}
     * @return 对应的适配器实例
     * @throws IllegalArgumentException 如果未注册该厂商的适配器
     */
    public LlmToolAdapter getAdapter(String provider) {
        LlmToolAdapter adapter = adapters.get(provider);
        if (adapter == null) {
            throw new IllegalArgumentException(
                    "No LlmToolAdapter registered for provider: '" + provider
                            + "'. Registered providers: " + adapters.keySet());
        }
        return adapter;
    }

    /**
     * 检查是否已注册指定厂商的适配器。
     *
     * @param provider LLM 厂商标识
     * @return true 表示已注册
     */
    public boolean hasAdapter(String provider) {
        return adapters.containsKey(provider);
    }

    /**
     * 获取所有已注册的适配器（只读视图）。
     *
     * @return 适配器集合
     */
    public Collection<LlmToolAdapter> getAllAdapters() {
        return Collections.unmodifiableCollection(adapters.values());
    }

    /**
     * 获取所有已注册的厂商标识。
     *
     * @return 厂商标识集合
     */
    public Collection<String> getSupportedProviders() {
        return Collections.unmodifiableSet(adapters.keySet());
    }
}
