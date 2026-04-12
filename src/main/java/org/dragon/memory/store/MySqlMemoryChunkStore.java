package org.dragon.memory.store;

import io.ebean.Database;
import lombok.extern.slf4j.Slf4j;
import org.dragon.datasource.entity.MemoryChunkEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 记忆片段 MySQL 存储实现
 *
 * @author binarytom
 * @version 1.0
 */
@Slf4j
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlMemoryChunkStore implements MemoryChunkStore {

    private final Database mysqlDb;

    public MySqlMemoryChunkStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(MemoryChunkEntity entity) {
        log.info("[MySqlMemoryChunkStore] Saving chunk: {}", entity.getId());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        mysqlDb.save(entity);
    }

    @Override
    public Optional<MemoryChunkEntity> findById(String id) {
        return Optional.ofNullable(mysqlDb.find(MemoryChunkEntity.class, id));
    }

    @Override
    public List<MemoryChunkEntity> findBySourceId(String sourceId) {
        return mysqlDb.find(MemoryChunkEntity.class)
                .where()
                .eq("sourceId", sourceId)
                .findList();
    }

    @Override
    public List<MemoryChunkEntity> findByCondition(String sourceId, String syncStatus, String indexedStatus, String tags, String search) {
        var query = mysqlDb.find(MemoryChunkEntity.class).where();
        if (sourceId != null && !sourceId.isBlank()) {
            query = query.eq("sourceId", sourceId);
        }
        if (syncStatus != null && !syncStatus.isBlank()) {
            query = query.eq("syncStatus", syncStatus);
        }
        if (indexedStatus != null && !indexedStatus.isBlank()) {
            query = query.eq("indexedStatus", indexedStatus);
        }
        if (tags != null && !tags.isBlank()) {
            query = query.contains("tags", tags);
        }
        if (search != null && !search.isBlank()) {
            query = query.or()
                    .contains("title", search)
                    .contains("content", search)
                    .endOr();
        }
        return query.findList();
    }

    @Override
    public boolean deleteById(String id) {
        log.info("[MySqlMemoryChunkStore] Deleting chunk: {}", id);
        return mysqlDb.delete(MemoryChunkEntity.class, id) > 0;
    }

    @Override
    public boolean deleteBatch(List<String> ids) {
        log.info("[MySqlMemoryChunkStore] Batch deleting {} chunks", ids.size());
        int deleted = mysqlDb.find(MemoryChunkEntity.class)
                .where()
                .in("id", ids)
                .delete();
        return deleted > 0;
    }

    @Override
    public long countByCondition(String sourceId, String syncStatus, String indexedStatus, String tags, String search) {
        var query = mysqlDb.find(MemoryChunkEntity.class).where();
        if (sourceId != null && !sourceId.isBlank()) {
            query = query.eq("sourceId", sourceId);
        }
        if (syncStatus != null && !syncStatus.isBlank()) {
            query = query.eq("syncStatus", syncStatus);
        }
        if (indexedStatus != null && !indexedStatus.isBlank()) {
            query = query.eq("indexedStatus", indexedStatus);
        }
        if (tags != null && !tags.isBlank()) {
            query = query.contains("tags", tags);
        }
        if (search != null && !search.isBlank()) {
            query = query.or()
                    .contains("title", search)
                    .contains("content", search)
                    .endOr();
        }
        return query.findCount();
    }

    @Override
    public void updateIndexStatus(String id, String status) {
        log.info("[MySqlMemoryChunkStore] Updating index status: id={}, status={}", id, status);
        mysqlDb.find(MemoryChunkEntity.class)
                .where()
                .eq("id", id)
                .asUpdate()
                .set("indexedStatus", status)
                .set("updatedAt", Instant.now())
                .update();
    }

    @Override
    public void updateIndexStatusBatch(List<String> ids, String status) {
        log.info("[MySqlMemoryChunkStore] Batch updating index status for {} chunks to {}", ids.size(), status);
        mysqlDb.find(MemoryChunkEntity.class)
                .where()
                .in("id", ids)
                .asUpdate()
                .set("indexedStatus", status)
                .set("updatedAt", Instant.now())
                .update();
    }

    @Override
    public void updateSyncStatus(String id, String syncStatus) {
        log.info("[MySqlMemoryChunkStore] Updating sync status: id={}, status={}", id, syncStatus);
        mysqlDb.find(MemoryChunkEntity.class)
                .where()
                .eq("id", id)
                .asUpdate()
                .set("syncStatus", syncStatus)
                .set("updatedAt", Instant.now())
                .update();
    }
}