package org.dragon.agent.model.store;

import org.dragon.agent.model.ModelInstance;
import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * ModelStore 模型实例存储接口
 */
public interface ModelStore extends Store {

    /**
     * 保存模型实例
     */
    void save(ModelInstance modelInstance);

    /**
     * 更新模型实例
     */
    void update(ModelInstance modelInstance);

    /**
     * 删除模型实例
     */
    void delete(String id);

    /**
     * 根据ID获取模型实例
     */
    Optional<ModelInstance> findById(String id);

    /**
     * 获取所有模型实例
     */
    List<ModelInstance> findAll();

    /**
     * 根据提供商获取模型实例列表
     */
    List<ModelInstance> findByProvider(ModelInstance.ModelProvider provider);

    /**
     * 获取所有启用的模型实例
     */
    List<ModelInstance> findEnabled();

    /**
     * 检查模型实例是否存在
     */
    boolean exists(String id);

    /**
     * 获取模型实例数量
     */
    int count();
}