package org.dragon.permission.store;

import io.ebean.Database;
import org.dragon.permission.entity.AssetMemberEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.dragon.permission.enums.ResourceType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * MySqlAssetMemberStore 资产成员MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlAssetMemberStore implements AssetMemberStore {

    private final Database mysqlDb;

    public MySqlAssetMemberStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(AssetMemberEntity member) {
        mysqlDb.save(member);
    }

    @Override
    public void update(AssetMemberEntity member) {
        mysqlDb.update(member);
    }

    @Override
    public void delete(Long id) {
        mysqlDb.delete(AssetMemberEntity.class, id);
    }

    @Override
    public Optional<AssetMemberEntity> findById(Long id) {
        AssetMemberEntity entity = mysqlDb.find(AssetMemberEntity.class, id);
        return Optional.ofNullable(entity);
    }

    @Override
    public List<AssetMemberEntity> findByResource(ResourceType resourceType, String resourceId) {
        return mysqlDb.find(AssetMemberEntity.class)
                .where()
                .eq("resourceType", resourceType.name())
                .eq("resourceId", resourceId)
                .findList();
    }

    @Override
    public Optional<AssetMemberEntity> findByResourceAndUser(ResourceType resourceType, String resourceId, Long userId) {
        AssetMemberEntity entity = mysqlDb.find(AssetMemberEntity.class)
                .where()
                .eq("resourceType", resourceType.name())
                .eq("resourceId", resourceId)
                .eq("userId", userId)
                .findOne();
        return Optional.ofNullable(entity);
    }

    @Override
    public List<AssetMemberEntity> findByUserId(Long userId) {
        return mysqlDb.find(AssetMemberEntity.class)
                .where()
                .eq("userId", userId)
                .findList();
    }

    @Override
    public List<AssetMemberEntity> findPendingInvitationsByUserId(Long userId) {
        return mysqlDb.find(AssetMemberEntity.class)
                .where()
                .eq("userId", userId)
                .eq("accepted", false)
                .findList();
    }

    @Override
    public void deleteByResource(ResourceType resourceType, String resourceId) {
        mysqlDb.find(AssetMemberEntity.class)
                .where()
                .eq("resourceType", resourceType.name())
                .eq("resourceId", resourceId)
                .delete();
    }

    @Override
    public boolean exists(ResourceType resourceType, String resourceId, Long userId) {
        return findByResourceAndUser(resourceType, resourceId, userId).isPresent();
    }
}
