package org.dragon.template.store;

import io.ebean.Database;
import org.dragon.permission.enums.ResourceType;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.dragon.datasource.entity.TemplateMarkEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MySqlTemplateMarkStore 模板标记 MySQL 存储实现
 *
 * @author yijunw
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlTemplateMarkStore implements TemplateMarkStore {

    private final Database db;

    public MySqlTemplateMarkStore(@Qualifier("mysqlEbeanDatabase") Database db) {
        this.db = db;
    }

    @Override
    public void save(TemplateMarkEntity templateMark) {
        if (templateMark.getId() == null) {
            templateMark.setId(java.util.UUID.randomUUID().toString());
        }
        if (templateMark.getCreatedAt() == null) {
            templateMark.setCreatedAt(LocalDateTime.now());
        }
        templateMark.setUpdatedAt(LocalDateTime.now());
        db.save(templateMark);
    }

    @Override
    public void update(TemplateMarkEntity templateMark) {
        templateMark.setUpdatedAt(LocalDateTime.now());
        db.update(templateMark);
    }

    @Override
    public Optional<TemplateMarkEntity> findById(String id) {
        TemplateMarkEntity entity = db.find(TemplateMarkEntity.class, id);
        return Optional.ofNullable(entity);
    }

    @Override
    public Optional<TemplateMarkEntity> findByResource(ResourceType resourceType, String resourceId) {
        TemplateMarkEntity entity = db.find(TemplateMarkEntity.class)
                .where()
                .eq("resourceType", resourceType.name())
                .eq("resourceId", resourceId)
                .findOne();
        return Optional.ofNullable(entity);
    }

    @Override
    public List<TemplateMarkEntity> findAll() {
        return db.find(TemplateMarkEntity.class).findList();
    }

    @Override
    public List<TemplateMarkEntity> findByResourceType(ResourceType resourceType) {
        return db.find(TemplateMarkEntity.class)
                .where()
                .eq("resourceType", resourceType.name())
                .findList();
    }

    @Override
    public List<TemplateMarkEntity> findByCategory(String category) {
        return db.find(TemplateMarkEntity.class)
                .where()
                .eq("category", category)
                .findList();
    }

    @Override
    public void incrementUsageCount(String id) {
        TemplateMarkEntity mark = db.find(TemplateMarkEntity.class, id);
        if (mark != null) {
            mark.setUsageCount(mark.getUsageCount() + 1);
            mark.setUpdatedAt(LocalDateTime.now());
            db.update(mark);
        }
    }

    @Override
    public void delete(String id) {
        db.delete(TemplateMarkEntity.class, id);
    }

    @Override
    public void deleteByResource(ResourceType resourceType, String resourceId) {
        db.find(TemplateMarkEntity.class)
                .where()
                .eq("resourceType", resourceType.name())
                .eq("resourceId", resourceId)
                .delete();
    }
}
