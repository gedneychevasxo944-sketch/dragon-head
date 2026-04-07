package org.dragon.asset.store;

import io.ebean.Database;
import org.dragon.asset.enums.AssociationType;
import org.dragon.datasource.entity.AssetAssociationEntity;
import org.dragon.permission.enums.ResourceType;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * MySqlAssetAssociationStore 资产关联 MySQL 存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlAssetAssociationStore implements AssetAssociationStore {

    private final Database mysqlDb;

    public MySqlAssetAssociationStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(AssetAssociationEntity association) {
        mysqlDb.save(association);
    }

    @Override
    public void update(AssetAssociationEntity association) {
        mysqlDb.update(association);
    }

    @Override
    public void delete(Long id) {
        mysqlDb.delete(AssetAssociationEntity.class, id);
    }

    @Override
    public Optional<AssetAssociationEntity> findById(Long id) {
        AssetAssociationEntity entity = mysqlDb.find(AssetAssociationEntity.class, id);
        return Optional.ofNullable(entity);
    }

    @Override
    public List<AssetAssociationEntity> findBySource(AssociationType type, ResourceType sourceType, String sourceId) {
        return mysqlDb.find(AssetAssociationEntity.class)
                .where()
                .eq("associationType", type.name())
                .eq("sourceType", sourceType.name())
                .eq("sourceId", sourceId)
                .orderBy("createdAt desc")
                .findList();
    }

    @Override
    public List<AssetAssociationEntity> findByTarget(AssociationType type, ResourceType targetType, String targetId) {
        return mysqlDb.find(AssetAssociationEntity.class)
                .where()
                .eq("associationType", type.name())
                .eq("targetType", targetType.name())
                .eq("targetId", targetId)
                .orderBy("createdAt desc")
                .findList();
    }

    @Override
    public List<AssetAssociationEntity> findByType(AssociationType type) {
        return mysqlDb.find(AssetAssociationEntity.class)
                .where()
                .eq("associationType", type.name())
                .orderBy("createdAt desc")
                .findList();
    }

    @Override
    public boolean exists(AssociationType type, ResourceType sourceType, String sourceId,
                          ResourceType targetType, String targetId) {
        return mysqlDb.find(AssetAssociationEntity.class)
                .where()
                .eq("associationType", type.name())
                .eq("sourceType", sourceType.name())
                .eq("sourceId", sourceId)
                .eq("targetType", targetType.name())
                .eq("targetId", targetId)
                .exists();
    }

    @Override
    public void deleteBySourceAndTarget(AssociationType type, ResourceType sourceType, String sourceId,
                                        ResourceType targetType, String targetId) {
        mysqlDb.find(AssetAssociationEntity.class)
                .where()
                .eq("associationType", type.name())
                .eq("sourceType", sourceType.name())
                .eq("sourceId", sourceId)
                .eq("targetType", targetType.name())
                .eq("targetId", targetId)
                .delete();
    }
}
