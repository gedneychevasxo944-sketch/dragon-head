package org.dragon.agent.model.store;

import org.dragon.agent.model.ModelInstance;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MemoryModelStore 模型实例内存存储实现
 */
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryModelStore implements ModelStore {

    private final Map<String, ModelInstance> store = new ConcurrentHashMap<>();

    @Override
    public void save(ModelInstance modelInstance) {
        if (modelInstance == null || modelInstance.getId() == null) {
            throw new IllegalArgumentException("ModelInstance or ModelInstance id cannot be null");
        }
        store.put(modelInstance.getId(), modelInstance);
    }

    @Override
    public void update(ModelInstance modelInstance) {
        if (modelInstance == null || modelInstance.getId() == null) {
            throw new IllegalArgumentException("ModelInstance or ModelInstance id cannot be null");
        }
        if (!store.containsKey(modelInstance.getId())) {
            throw new IllegalArgumentException("ModelInstance not found: " + modelInstance.getId());
        }
        store.put(modelInstance.getId(), modelInstance);
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }

    @Override
    public Optional<ModelInstance> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ModelInstance> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<ModelInstance> findByProvider(ModelInstance.ModelProvider provider) {
        return store.values().stream()
                .filter(m -> m.getProvider() == provider)
                .collect(Collectors.toList());
    }

    @Override
    public List<ModelInstance> findEnabled() {
        return store.values().stream()
                .filter(ModelInstance::isEnabled)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String id) {
        return store.containsKey(id);
    }

    @Override
    public int count() {
        return store.size();
    }
}