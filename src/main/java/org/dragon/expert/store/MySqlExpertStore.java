package org.dragon.expert.store;

import io.ebean.Database;
import org.dragon.permission.enums.ResourceType;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.dragon.datasource.entity.ExpertEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MySqlExpertStore Expert 标记 MySQL 存储实现
 *
 * @author yijunw
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlExpertStore implements ExpertStore {

    private final Database db;

    public MySqlExpertStore(@Qualifier("mysqlEbeanDatabase") Database db) {
        this.db = db;
    }

    @Override
    public void save(ExpertEntity expertMark) {
        if (expertMark.getId() == null) {
            expertMark.setId(java.util.UUID.randomUUID().toString());
        }
        if (expertMark.getCreatedAt() == null) {
            expertMark.setCreatedAt(LocalDateTime.now());
        }
        expertMark.setUpdatedAt(LocalDateTime.now());
        db.save(expertMark);
    }

    @Override
    public void update(ExpertEntity expertMark) {
        expertMark.setUpdatedAt(LocalDateTime.now());
        db.update(expertMark);
    }

    @Override
    public Optional<ExpertEntity> findById(String id) {
        ExpertEntity entity = db.find(ExpertEntity.class, id);
        return Optional.ofNullable(entity);
    }

    @Override
    public Optional<ExpertEntity> findByResource(ResourceType resourceType, String resourceId) {
        ExpertEntity entity = db.find(ExpertEntity.class)
                .where()
                .eq("resourceType", resourceType.name())
                .eq("resourceId", resourceId)
                .findOne();
        return Optional.ofNullable(entity);
    }

    @Override
    public List<ExpertEntity> findAll() {
        return db.find(ExpertEntity.class).findList();
    }

    @Override
    public List<ExpertEntity> findByResourceType(ResourceType resourceType) {
        return db.find(ExpertEntity.class)
                .where()
                .eq("resourceType", resourceType.name())
                .findList();
    }

    @Override
    public List<ExpertEntity> findByCategory(String category) {
        return db.find(ExpertEntity.class)
                .where()
                .eq("category", category)
                .findList();
    }

    @Override
    public void incrementUsageCount(String id) {
        ExpertEntity mark = db.find(ExpertEntity.class, id);
        if (mark != null) {
            mark.setUsageCount(mark.getUsageCount() + 1);
            mark.setUpdatedAt(LocalDateTime.now());
            db.update(mark);
        }
    }

    @Override
    public void delete(String id) {
        db.delete(ExpertEntity.class, id);
    }

    @Override
    public void deleteByResource(ResourceType resourceType, String resourceId) {
        db.find(ExpertEntity.class)
                .where()
                .eq("resourceType", resourceType.name())
                .eq("resourceId", resourceId)
                .delete();
    }
}