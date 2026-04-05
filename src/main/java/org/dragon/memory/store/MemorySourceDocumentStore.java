package org.dragon.memory.store;

import org.dragon.datasource.entity.SourceDocumentEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据源内存存储实现
 *
 * @author binarytom
 * @version 1.0
 */
@Slf4j
@StoreTypeAnn(StoreType.MEMORY)
public class MemorySourceDocumentStore implements SourceDocumentStore {

    private final Map<String, SourceDocumentEntity> storage = new ConcurrentHashMap<>();

    @Override
    public void save(SourceDocumentEntity entity) {
        log.info("[MemorySourceDocumentStore] Saving source document: {}", entity.getId());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        storage.put(entity.getId(), entity);
    }

    @Override
    public Optional<SourceDocumentEntity> findById(String id) {
        log.debug("[MemorySourceDocumentStore] Finding source document by id: {}", id);
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<SourceDocumentEntity> findAll() {
        log.debug("[MemorySourceDocumentStore] Finding all source documents");
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<SourceDocumentEntity> findByCondition(String search, String status, String sourceType) {
        log.debug("[MemorySourceDocumentStore] Finding source documents by condition: search={}, status={}, sourceType={}",
                search, status, sourceType);

        return storage.values().stream()
                .filter(entity -> matchesSearch(entity, search))
                .filter(entity -> matchesStatus(entity, status))
                .filter(entity -> matchesSourceType(entity, sourceType))
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteById(String id) {
        log.info("[MemorySourceDocumentStore] Deleting source document by id: {}", id);
        return storage.remove(id) != null;
    }

    @Override
    public long countByCondition(String search, String status, String sourceType) {
        log.debug("[MemorySourceDocumentStore] Counting source documents by condition: search={}, status={}, sourceType={}",
                search, status, sourceType);

        return storage.values().stream()
                .filter(entity -> matchesSearch(entity, search))
                .filter(entity -> matchesStatus(entity, status))
                .filter(entity -> matchesSourceType(entity, sourceType))
                .count();
    }

    private boolean matchesSearch(SourceDocumentEntity entity, String search) {
        if (search == null || search.isEmpty()) {
            return true;
        }
        String lowerCaseSearch = search.toLowerCase();
        return entity.getTitle().toLowerCase().contains(lowerCaseSearch) ||
                (entity.getSourcePath() != null && entity.getSourcePath().toLowerCase().contains(lowerCaseSearch)) ||
                (entity.getProvider() != null && entity.getProvider().toLowerCase().contains(lowerCaseSearch));
    }

    private boolean matchesStatus(SourceDocumentEntity entity, String status) {
        if (status == null || status.isEmpty()) {
            return true;
        }
        return status.equals(entity.getStatus());
    }

    private boolean matchesSourceType(SourceDocumentEntity entity, String sourceType) {
        if (sourceType == null || sourceType.isEmpty()) {
            return true;
        }
        return sourceType.equals(entity.getSourceType());
    }
}
