package org.dragon.memory.store;

import io.ebean.Database;
import org.dragon.datasource.entity.SourceDocumentEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 数据源 MySQL 存储实现
 *
 * @author binarytom
 * @version 1.0
 */
@Slf4j
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlSourceDocumentStore implements SourceDocumentStore {

    private final Database mysqlDb;

    public MySqlSourceDocumentStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(SourceDocumentEntity entity) {
        log.info("[MySqlSourceDocumentStore] Saving source document: {}", entity.getId());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        mysqlDb.save(entity);
    }

    @Override
    public Optional<SourceDocumentEntity> findById(String id) {
        log.debug("[MySqlSourceDocumentStore] Finding source document by id: {}", id);
        return Optional.ofNullable(mysqlDb.find(SourceDocumentEntity.class, id));
    }

    @Override
    public List<SourceDocumentEntity> findAll() {
        log.debug("[MySqlSourceDocumentStore] Finding all source documents");
        return mysqlDb.find(SourceDocumentEntity.class).findList();
    }

    @Override
    public List<SourceDocumentEntity> findByCondition(String search, String status, String sourceType) {
        log.debug("[MySqlSourceDocumentStore] Finding source documents by condition: search={}, status={}, sourceType={}",
                search, status, sourceType);

        var query = mysqlDb.find(SourceDocumentEntity.class);

        if (search != null && !search.isEmpty()) {
            query.where()
                    .or()
                    .ilike("title", "%" + search + "%")
                    .ilike("sourcePath", "%" + search + "%")
                    .ilike("provider", "%" + search + "%")
                    .endOr();
        }

        if (status != null && !status.isEmpty()) {
            query.where().eq("status", status);
        }

        if (sourceType != null && !sourceType.isEmpty()) {
            query.where().eq("sourceType", sourceType);
        }

        return query.findList();
    }

    @Override
    public boolean deleteById(String id) {
        log.info("[MySqlSourceDocumentStore] Deleting source document by id: {}", id);
        int deletedCount = mysqlDb.delete(SourceDocumentEntity.class, id);
        return deletedCount > 0;
    }

    @Override
    public long countByCondition(String search, String status, String sourceType) {
        log.debug("[MySqlSourceDocumentStore] Counting source documents by condition: search={}, status={}, sourceType={}",
                search, status, sourceType);

        var query = mysqlDb.find(SourceDocumentEntity.class);

        if (search != null && !search.isEmpty()) {
            query.where()
                    .or()
                    .ilike("title", "%" + search + "%")
                    .ilike("sourcePath", "%" + search + "%")
                    .ilike("provider", "%" + search + "%")
                    .endOr();
        }

        if (status != null && !status.isEmpty()) {
            query.where().eq("status", status);
        }

        if (sourceType != null && !sourceType.isEmpty()) {
            query.where().eq("sourceType", sourceType);
        }

        return query.findCount();
    }
}
