package org.dragon.impression.store;

import org.dragon.impression.entity.ImpressionEntity;
import org.dragon.impression.enums.ImpressionType;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MemoryImpressionStore 印象内存存储实现
 */
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryImpressionStore implements ImpressionStore {

    private final Map<String, ImpressionEntity> store = new ConcurrentHashMap<>();

    @Override
    public void save(ImpressionEntity impression) {
        if (impression.getId() == null) {
            impression.setId(java.util.UUID.randomUUID().toString());
        }
        store.put(impression.getId(), impression);
    }

    @Override
    public void update(ImpressionEntity impression) {
        if (impression.getId() == null || !store.containsKey(impression.getId())) {
            throw new IllegalArgumentException("Impression not found: " + impression.getId());
        }
        store.put(impression.getId(), impression);
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }

    @Override
    public Optional<ImpressionEntity> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ImpressionEntity> findBySource(ImpressionType sourceType, String sourceId) {
        return store.values().stream()
                .filter(e -> sourceType == e.getSourceType() && sourceId.equals(e.getSourceId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ImpressionEntity> findByTarget(ImpressionType targetType, String targetId) {
        return store.values().stream()
                .filter(e -> targetType == e.getTargetType() && targetId.equals(e.getTargetId()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ImpressionEntity> findBySourceAndTarget(ImpressionType sourceType, String sourceId,
                                                              ImpressionType targetType, String targetId) {
        return store.values().stream()
                .filter(e -> sourceType == e.getSourceType()
                        && sourceId.equals(e.getSourceId())
                        && targetType == e.getTargetType()
                        && targetId.equals(e.getTargetId()))
                .findFirst();
    }

    @Override
    public void deleteBySourceAndTarget(ImpressionType sourceType, String sourceId,
                                         ImpressionType targetType, String targetId) {
        store.entrySet().removeIf(e -> {
            ImpressionEntity entity = e.getValue();
            return sourceType == entity.getSourceType()
                    && sourceId.equals(entity.getSourceId())
                    && targetType == entity.getTargetType()
                    && targetId.equals(entity.getTargetId());
        });
    }
}
