package org.dragon.memory.store;

import io.ebean.Database;
import lombok.extern.slf4j.Slf4j;
import org.dragon.datasource.entity.BindingEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 绑定关系 MySQL 存储实现
 *
 * @author binarytom
 * @version 1.0
 */
@Slf4j
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlBindingStore implements BindingStore {

    private final Database mysqlDb;

    public MySqlBindingStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(BindingEntity entity) {
        log.info("[MySqlBindingStore] Saving binding: {}", entity.getId());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        mysqlDb.save(entity);
    }

    @Override
    public Optional<BindingEntity> findById(String id) {
        log.debug("[MySqlBindingStore] Finding binding by id: {}", id);
        return Optional.ofNullable(mysqlDb.find(BindingEntity.class, id));
    }

    @Override
    public List<BindingEntity> findByTarget(String targetType, String targetId) {
        log.debug("[MySqlBindingStore] Finding bindings by target: type={}, id={}", targetType, targetId);
        return mysqlDb.find(BindingEntity.class)
                .where()
                .eq("targetType", targetType)
                .eq("targetId", targetId)
                .findList();
    }

    @Override
    public List<BindingEntity> findByFileId(String fileId) {
        log.debug("[MySqlBindingStore] Finding bindings by fileId: {}", fileId);
        return mysqlDb.find(BindingEntity.class)
                .where()
                .eq("fileId", fileId)
                .findList();
    }

    @Override
    public boolean deleteById(String id) {
        log.info("[MySqlBindingStore] Deleting binding by id: {}", id);
        return mysqlDb.delete(BindingEntity.class, id) > 0;
    }

    @Override
    public long countByTarget(String targetType, String targetId) {
        return mysqlDb.find(BindingEntity.class)
                .where()
                .eq("targetType", targetType)
                .eq("targetId", targetId)
                .findCount();
    }
}