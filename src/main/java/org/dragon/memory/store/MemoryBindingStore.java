package org.dragon.memory.store;

import lombok.extern.slf4j.Slf4j;
import org.dragon.datasource.entity.BindingEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 绑定关系内存存储实现
 *
 * @author binarytom
 * @version 1.0
 */
@Slf4j
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryBindingStore implements BindingStore {

    private final Map<String, BindingEntity> storage = new ConcurrentHashMap<>();

    @Override
    public void save(BindingEntity entity) {
        log.info("[MemoryBindingStore] Saving binding: {}", entity.getId());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        storage.put(entity.getId(), entity);
    }

    @Override
    public Optional<BindingEntity> findById(String id) {
        log.debug("[MemoryBindingStore] Finding binding by id: {}", id);
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<BindingEntity> findByTarget(String targetType, String targetId) {
        log.debug("[MemoryBindingStore] Finding bindings by target: type={}, id={}", targetType, targetId);
        return storage.values().stream()
                .filter(e -> targetType.equals(e.getTargetType()) && targetId.equals(e.getTargetId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<BindingEntity> findByFileId(String fileId) {
        log.debug("[MemoryBindingStore] Finding bindings by fileId: {}", fileId);
        return storage.values().stream()
                .filter(e -> fileId.equals(e.getFileId()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteById(String id) {
        log.info("[MemoryBindingStore] Deleting binding by id: {}", id);
        return storage.remove(id) != null;
    }

    @Override
    public long countByTarget(String targetType, String targetId) {
        return storage.values().stream()
                .filter(e -> targetType.equals(e.getTargetType()) && targetId.equals(e.getTargetId()))
                .count();
    }
}