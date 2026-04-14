package org.dragon.asset.tag.store;

import io.ebean.Database;
import org.dragon.datasource.entity.AssetTagEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MySqlAssetTagStore 资产标签 MySQL 存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlAssetTagStore implements AssetTagStore {

    private final Database db;

    public MySqlAssetTagStore(@Qualifier("mysqlEbeanDatabase") Database db) {
        this.db = db;
    }

    @Override
    public void save(AssetTagEntity tag) {
        db.save(tag);
    }

    @Override
    public void delete(String resourceType, String resourceId, String name) {
        db.find(AssetTagEntity.class)
                .where()
                .eq("resourceType", resourceType)
                .eq("resourceId", resourceId)
                .eq("name", name)
                .delete();
    }

    @Override
    public List<AssetTagEntity> findByResource(String resourceType, String resourceId) {
        return db.find(AssetTagEntity.class)
                .where()
                .eq("resourceType", resourceType)
                .eq("resourceId", resourceId)
                .findList();
    }

    @Override
    public List<AssetTagEntity> findByResources(String resourceType, List<String> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return List.of();
        }
        return db.find(AssetTagEntity.class)
                .where()
                .eq("resourceType", resourceType)
                .in("resourceId", resourceIds)
                .findList();
    }

    @Override
    public Set<String> findTagNamesByResourceType(String resourceType) {
        return db.find(AssetTagEntity.class)
                .where()
                .eq("resourceType", resourceType)
                .findList()
                .stream()
                .map(AssetTagEntity::getName)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean exists(String resourceType, String resourceId, String name) {
        return db.find(AssetTagEntity.class)
                .where()
                .eq("resourceType", resourceType)
                .eq("resourceId", resourceId)
                .eq("name", name)
                .exists();
    }

    @Override
    public List<AssetTagEntity> findByTagNameAndResourceType(String tagName, String resourceType) {
        return db.find(AssetTagEntity.class)
                .where()
                .eq("name", tagName)
                .eq("resourceType", resourceType)
                .findList();
    }
}
