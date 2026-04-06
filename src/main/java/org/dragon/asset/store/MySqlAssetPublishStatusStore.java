package org.dragon.asset.store;

import io.ebean.Database;
import org.dragon.datasource.entity.AssetPublishStatusEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * MySqlAssetPublishStatusStore 资产发布状态MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlAssetPublishStatusStore implements AssetPublishStatusStore {

    private final Database mysqlDb;

    public MySqlAssetPublishStatusStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(AssetPublishStatusEntity entity) {
        mysqlDb.save(entity);
    }

    @Override
    public void update(AssetPublishStatusEntity entity) {
        mysqlDb.update(entity);
    }

    @Override
    public Optional<AssetPublishStatusEntity> findById(String id) {
        AssetPublishStatusEntity entity = mysqlDb.find(AssetPublishStatusEntity.class, id);
        return Optional.ofNullable(entity);
    }

    @Override
    public Optional<AssetPublishStatusEntity> findByResource(String resourceType, String resourceId) {
        AssetPublishStatusEntity entity = mysqlDb.find(AssetPublishStatusEntity.class)
                .where()
                .eq("resourceType", resourceType)
                .eq("resourceId", resourceId)
                .findOne();
        return Optional.ofNullable(entity);
    }

    @Override
    public List<AssetPublishStatusEntity> findByResourceTypeAndStatus(String resourceType, String status) {
        return mysqlDb.find(AssetPublishStatusEntity.class)
                .where()
                .eq("resourceType", resourceType)
                .eq("status", status)
                .findList();
    }

    @Override
    public List<AssetPublishStatusEntity> findByResourceType(String resourceType) {
        return mysqlDb.find(AssetPublishStatusEntity.class)
                .where()
                .eq("resourceType", resourceType)
                .findList();
    }

    @Override
    public List<AssetPublishStatusEntity> findByStatus(String status) {
        return mysqlDb.find(AssetPublishStatusEntity.class)
                .where()
                .eq("status", status)
                .findList();
    }

    @Override
    public boolean exists(String resourceType, String resourceId) {
        return findByResource(resourceType, resourceId).isPresent();
    }

    @Override
    public void delete(String resourceType, String resourceId) {
        mysqlDb.find(AssetPublishStatusEntity.class)
                .where()
                .eq("resourceType", resourceType)
                .eq("resourceId", resourceId)
                .delete();
    }
}