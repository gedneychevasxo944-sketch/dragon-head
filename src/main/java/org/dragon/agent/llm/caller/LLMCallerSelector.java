package org.dragon.agent.llm.caller;

import org.dragon.agent.model.ModelInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * LLM Caller 路由器
 * 根据 ModelInstance.provider 路由到对应的 LLMCaller 实现
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Service
public class LLMCallerSelector {

    private final LLMCaller defaultCaller;
    private final KimiLLMCaller moonshotCaller;
    private final DeepseekLLMCaller deepseekCaller;
    private final MinimaxLLMCaller minimaxCaller;

    @Autowired
    public LLMCallerSelector(
            @Qualifier("kimiLLMCaller") KimiLLMCaller moonshotCaller,
            @Qualifier("deepseekLLMCaller") DeepseekLLMCaller deepseekCaller,
            @Qualifier("minimaxLLMCaller") MinimaxLLMCaller minimaxCaller,
            LLMCaller defaultCaller) {
        this.moonshotCaller = moonshotCaller;
        this.deepseekCaller = deepseekCaller;
        this.minimaxCaller = minimaxCaller;
        this.defaultCaller = defaultCaller;
    }

    /**
     * 根据模型实例选择对应的 LLMCaller
     *
     * @param model 模型实例
     * @return 对应的 LLMCaller
     */
    public LLMCaller select(ModelInstance model) {
        if (model == null || model.getProvider() == null) {
            log.warn("[LLMCallerSelector] Model or provider is null, using default caller");
            return defaultCaller;
        }

        return selectByProvider(model.getProvider());
    }

    /**
     * 根据 provider 选择对应的 LLMCaller
     *
     * @param provider 模型提供商
     * @return 对应的 LLMCaller
     */
    public LLMCaller selectByProvider(ModelInstance.ModelProvider provider) {
        if (provider == null) {
            return defaultCaller;
        }

        switch (provider) {
            case MOONSHOT:
                return moonshotCaller;
            case DEEPSEEK:
                return deepseekCaller;
            case MINIMAX:
                return minimaxCaller;
            default:
                log.warn("[LLMCallerSelector] Unknown provider {}: {}, using default caller",
                        provider, provider.name());
                return defaultCaller;
        }
    }

    /**
     * 获取默认 Caller
     *
     * @return 默认 LLMCaller
     */
    public LLMCaller getDefault() {
        return defaultCaller;
    }
}
