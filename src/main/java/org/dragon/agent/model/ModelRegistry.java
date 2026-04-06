package org.dragon.agent.model;

import org.dragon.agent.model.store.ModelStore;
import org.dragon.config.context.InheritanceContext;
import org.dragon.config.service.ConfigApplication;
import org.dragon.store.StoreFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 模型注册中心
 * 负责管理所有可用模型的注册、获取和健康检查
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class ModelRegistry {

    private final ModelStore modelStore;

    /**
     * 默认模型 ID
     */
    private volatile String defaultModelId;

    @Autowired
    public ModelRegistry(StoreFactory storeFactory, ConfigApplication configApplication) {
        this.modelStore = storeFactory.get(ModelStore.class);
        this.defaultModelId = configApplication.getStringValue(
                "model.default-id",
                InheritanceContext.forGlobal(),
                null
        );
    }

    /**
     * 注册模型
     *
     * @param model 模型实例
     */
    public void register(ModelInstance model) {
        if (model == null || model.getId() == null) {
            throw new IllegalArgumentException("Model or Model id cannot be null");
        }

        modelStore.save(model);
        log.info("[ModelRegistry] Registered model: {} (provider: {}, model: {})",
                model.getId(), model.getProvider(), model.getModelName());

        // 如果没有默认模型，设为第一个
        if (defaultModelId == null) {
            defaultModelId = model.getId();
        }
    }

    /**
     * 注销模型
     *
     * @param modelId 模型 ID
     */
    public void unregister(String modelId) {
        if (!modelStore.exists(modelId)) {
            return;
        }

        modelStore.delete(modelId);
        log.info("[ModelRegistry] Unregistered model: {}", modelId);

        // 如果删除的是默认模型，选择下一个
        if (defaultModelId != null && defaultModelId.equals(modelId)) {
            List<ModelInstance> all = modelStore.findAll();
            defaultModelId = all.isEmpty() ? null : all.get(0).getId();
        }
    }

    /**
     * 获取模型
     *
     * @param modelId 模型 ID
     * @return Optional 模型
     */
    public Optional<ModelInstance> get(String modelId) {
        return modelStore.findById(modelId);
    }

    /**
     * 获取默认模型
     *
     * @return Optional 模型
     */
    public Optional<ModelInstance> getDefault() {
        if (defaultModelId == null) {
            return Optional.empty();
        }
        return get(defaultModelId);
    }

    /**
     * 获取所有模型
     *
     * @return 模型列表
     */
    public List<ModelInstance> listModels() {
        return modelStore.findAll();
    }

    /**
     * 获取所有启用的模型
     *
     * @return 启用的模型列表
     */
    public List<ModelInstance> listEnabledModels() {
        return modelStore.findEnabled();
    }

    /**
     * 根据提供商获取模型
     *
     * @param provider 提供商
     * @return 模型列表
     */
    public List<ModelInstance> listByProvider(ModelInstance.ModelProvider provider) {
        return modelStore.findByProvider(provider);
    }

    /**
     * 设置默认模型
     *
     * @param modelId 模型 ID
     */
    public void setDefaultModel(String modelId) {
        if (!modelStore.exists(modelId)) {
            throw new IllegalArgumentException("Model not found: " + modelId);
        }
        defaultModelId = modelId;
        log.info("[ModelRegistry] Set default model: {}", modelId);
    }

    /**
     * 启用模型
     *
     * @param modelId 模型 ID
     */
    public void enable(String modelId) {
        get(modelId).ifPresent(model -> {
            model.setEnabled(true);
            modelStore.update(model);
            log.info("[ModelRegistry] Enabled model: {}", modelId);
        });
    }

    /**
     * 禁用模型
     *
     * @param modelId 模型 ID
     */
    public void disable(String modelId) {
        get(modelId).ifPresent(model -> {
            model.setEnabled(false);
            modelStore.update(model);
            log.info("[ModelRegistry] Disabled model: {}", modelId);
        });
    }

    /**
     * 健康检查
     *
     * @param modelId 模型 ID
     * @return 是否健康
     */
    public boolean healthCheck(String modelId) {
        ModelInstance model = modelStore.findById(modelId).orElse(null);
        if (model == null || !model.isEnabled()) {
            return false;
        }
        // 暂时返回 true
        return true;
    }

    /**
     * 检查模型是否存在
     *
     * @param modelId 模型 ID
     * @return 是否存在
     */
    public boolean exists(String modelId) {
        return modelStore.exists(modelId);
    }

    /**
     * 获取注册表大小
     *
     * @return 模型数量
     */
    public int size() {
        return modelStore.count();
    }
}