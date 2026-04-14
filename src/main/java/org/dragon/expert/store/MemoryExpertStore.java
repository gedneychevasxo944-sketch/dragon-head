package org.dragon.expert.store;

import org.dragon.permission.enums.ResourceType;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.dragon.datasource.entity.ExpertEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MemoryExpertStore Expert 标记内存存储实现
 *
 * @author yijunw
 */
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryExpertStore implements ExpertStore {

    private final Map<String, ExpertEntity> store = new ConcurrentHashMap<>();

    private String keyById(String id) {
        return id;
    }

    @Override
    public void save(ExpertEntity expertMark) {
        if (expertMark.getId() == null) {
            expertMark.setId(java.util.UUID.randomUUID().toString());
        }
        if (expertMark.getCreatedAt() == null) {
            expertMark.setCreatedAt(LocalDateTime.now());
        }
        expertMark.setUpdatedAt(LocalDateTime.now());
        store.put(keyById(expertMark.getId()), expertMark);
    }

    @Override
    public void update(ExpertEntity expertMark) {
        expertMark.setUpdatedAt(LocalDateTime.now());
        store.put(keyById(expertMark.getId()), expertMark);
    }

    @Override
    public Optional<ExpertEntity> findById(String id) {
        return Optional.ofNullable(store.get(keyById(id)));
    }

    @Override
    public Optional<ExpertEntity> findByResource(ResourceType resourceType, String resourceId) {
        return store.values().stream()
                .filter(e -> resourceType.equals(e.getResourceType())
                        && resourceId.equals(e.getResourceId()))
                .findFirst();
    }

    @Override
    public List<ExpertEntity> findAll() {
        return store.values().stream().collect(Collectors.toList());
    }

    @Override
    public List<ExpertEntity> findByResourceType(ResourceType resourceType) {
        return store.values().stream()
                .filter(e -> resourceType.equals(e.getResourceType()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ExpertEntity> findByCategory(String category) {
        return store.values().stream()
                .filter(e -> category.equals(e.getCategory()))
                .collect(Collectors.toList());
    }

    @Override
    public void incrementUsageCount(String id) {
        ExpertEntity mark = store.get(keyById(id));
        if (mark != null) {
            mark.setUsageCount(mark.getUsageCount() + 1);
            mark.setUpdatedAt(LocalDateTime.now());
        }
    }

    @Override
    public void delete(String id) {
        store.remove(keyById(id));
    }

    @Override
    public void deleteByResource(ResourceType resourceType, String resourceId) {
        store.values().removeIf(e -> resourceType.equals(e.getResourceType())
                && resourceId.equals(e.getResourceId()));
    }
}