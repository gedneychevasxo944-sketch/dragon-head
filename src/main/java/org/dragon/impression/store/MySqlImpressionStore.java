package org.dragon.impression.store;

import io.ebean.Database;
import org.dragon.impression.entity.ImpressionEntity;
import org.dragon.impression.enums.ImpressionType;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * MySqlImpressionStore 印象 MySQL 存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlImpressionStore implements ImpressionStore {

    private final Database mysqlDb;

    public MySqlImpressionStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(ImpressionEntity impression) {
        if (impression.getId() == null || impression.getId().isEmpty()) {
            impression.setId(java.util.UUID.randomUUID().toString());
        }
        mysqlDb.save(impression);
    }

    @Override
    public void update(ImpressionEntity impression) {
        mysqlDb.update(impression);
    }

    @Override
    public void delete(String id) {
        mysqlDb.delete(ImpressionEntity.class, id);
    }

    @Override
    public Optional<ImpressionEntity> findById(String id) {
        ImpressionEntity entity = mysqlDb.find(ImpressionEntity.class, id);
        return Optional.ofNullable(entity);
    }

    @Override
    public List<ImpressionEntity> findBySource(ImpressionType sourceType, String sourceId) {
        return mysqlDb.find(ImpressionEntity.class)
                .where()
                .eq("sourceType", sourceType.name())
                .eq("sourceId", sourceId)
                .orderBy("createdAt desc")
                .findList();
    }

    @Override
    public List<ImpressionEntity> findByTarget(ImpressionType targetType, String targetId) {
        return mysqlDb.find(ImpressionEntity.class)
                .where()
                .eq("targetType", targetType.name())
                .eq("targetId", targetId)
                .orderBy("createdAt desc")
                .findList();
    }

    @Override
    public Optional<ImpressionEntity> findBySourceAndTarget(ImpressionType sourceType, String sourceId,
                                                              ImpressionType targetType, String targetId) {
        ImpressionEntity entity = mysqlDb.find(ImpressionEntity.class)
                .where()
                .eq("sourceType", sourceType.name())
                .eq("sourceId", sourceId)
                .eq("targetType", targetType.name())
                .eq("targetId", targetId)
                .findOne();
        return Optional.ofNullable(entity);
    }

    @Override
    public void deleteBySourceAndTarget(ImpressionType sourceType, String sourceId,
                                         ImpressionType targetType, String targetId) {
        mysqlDb.find(ImpressionEntity.class)
                .where()
                .eq("sourceType", sourceType.name())
                .eq("sourceId", sourceId)
                .eq("targetType", targetType.name())
                .eq("targetId", targetId)
                .delete();
    }
}
