package org.dragon.memory.store;

import lombok.extern.slf4j.Slf4j;
import org.dragon.datasource.entity.MemoryChunkEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 记忆片段内存存储实现
 *
 * @author binarytom
 * @version 1.0
 */
@Slf4j
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryMemoryChunkStore implements MemoryChunkStore {

    private final Map<String, MemoryChunkEntity> storage = new ConcurrentHashMap<>();

    @Override
    public void save(MemoryChunkEntity entity) {
        log.info("[MemoryMemoryChunkStore] Saving chunk: {}", entity.getId());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        storage.put(entity.getId(), entity);
    }

    @Override
    public Optional<MemoryChunkEntity> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<MemoryChunkEntity> findBySourceId(String sourceId) {
        return storage.values().stream()
                .filter(e -> sourceId.equals(e.getSourceId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MemoryChunkEntity> findByCondition(String sourceId, String syncStatus, String indexedStatus, String search) {
        return storage.values().stream()
                .filter(e -> sourceId == null || sourceId.equals(e.getSourceId()))
                .filter(e -> syncStatus == null || syncStatus.equals(e.getSyncStatus()))
                .filter(e -> indexedStatus == null || indexedStatus.equals(e.getIndexedStatus()))
                .filter(e -> search == null
                        || (e.getContent() != null && e.getContent().contains(search))
                        || (e.getTitle() != null && e.getTitle().contains(search)))
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteById(String id) {
        log.info("[MemoryMemoryChunkStore] Deleting chunk: {}", id);
        return storage.remove(id) != null;
    }

    @Override
    public boolean deleteBatch(List<String> ids) {
        log.info("[MemoryMemoryChunkStore] Batch deleting {} chunks", ids.size());
        ids.forEach(storage::remove);
        return true;
    }

    @Override
    public long countByCondition(String sourceId, String syncStatus, String indexedStatus, String search) {
        return findByCondition(sourceId, syncStatus, indexedStatus, search).size();
    }

    @Override
    public void updateIndexStatus(String id, String status) {
        MemoryChunkEntity entity = storage.get(id);
        if (entity != null) {
            entity.setIndexedStatus(status);
            entity.setUpdatedAt(Instant.now());
        }
    }

    @Override
    public void updateIndexStatusBatch(List<String> ids, String status) {
        ids.forEach(id -> updateIndexStatus(id, status));
    }

    @Override
    public void updateSyncStatus(String id, String syncStatus) {
        MemoryChunkEntity entity = storage.get(id);
        if (entity != null) {
            entity.setSyncStatus(syncStatus);
            entity.setUpdatedAt(Instant.now());
        }
    }
}