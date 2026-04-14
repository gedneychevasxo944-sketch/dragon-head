package org.dragon.template.store;

import org.dragon.permission.enums.ResourceType;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.dragon.datasource.entity.TemplateMarkEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MemoryTemplateMarkStore 模板标记内存存储实现
 *
 * @author yijunw
 */
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryTemplateMarkStore implements TemplateMarkStore {

    private final Map<String, TemplateMarkEntity> store = new ConcurrentHashMap<>();

    private String key(ResourceType resourceType, String resourceId) {
        return resourceType.name() + ":" + resourceId;
    }

    private String keyById(String id) {
        return id;
    }

    @Override
    public void save(TemplateMarkEntity templateMark) {
        if (templateMark.getId() == null) {
            templateMark.setId(java.util.UUID.randomUUID().toString());
        }
        if (templateMark.getCreatedAt() == null) {
            templateMark.setCreatedAt(LocalDateTime.now());
        }
        templateMark.setUpdatedAt(LocalDateTime.now());
        store.put(keyById(templateMark.getId()), templateMark);
    }

    @Override
    public void update(TemplateMarkEntity templateMark) {
        templateMark.setUpdatedAt(LocalDateTime.now());
        store.put(keyById(templateMark.getId()), templateMark);
    }

    @Override
    public Optional<TemplateMarkEntity> findById(String id) {
        return Optional.ofNullable(store.get(keyById(id)));
    }

    @Override
    public Optional<TemplateMarkEntity> findByResource(ResourceType resourceType, String resourceId) {
        return store.values().stream()
                .filter(e -> resourceType.equals(e.getResourceType())
                        && resourceId.equals(e.getResourceId()))
                .findFirst();
    }

    @Override
    public List<TemplateMarkEntity> findAll() {
        return store.values().stream().collect(Collectors.toList());
    }

    @Override
    public List<TemplateMarkEntity> findByResourceType(ResourceType resourceType) {
        return store.values().stream()
                .filter(e -> resourceType.equals(e.getResourceType()))
                .collect(Collectors.toList());
    }

    @Override
    public List<TemplateMarkEntity> findByCategory(String category) {
        return store.values().stream()
                .filter(e -> category.equals(e.getCategory()))
                .collect(Collectors.toList());
    }

    @Override
    public void incrementUsageCount(String id) {
        TemplateMarkEntity mark = store.get(keyById(id));
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
