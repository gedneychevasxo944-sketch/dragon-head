package org.dragon.skill.store;

import io.ebean.Database;
import org.dragon.skill.entity.SkillEntity;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * MySQL 实现 Skill 存储。
 * 使用 Ebean ORM 进行数据库操作。
 *
 * @since 1.0
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlSkillStore implements SkillStore {

    private final Database db;

    public MySqlSkillStore(@Qualifier("mysqlEbeanDatabase") Database db) {
        this.db = db;
    }

    @Override
    public SkillEntity save(SkillEntity entity) {
        if (entity.getId() == null) {
            entity.setCreatedAt(java.time.LocalDateTime.now());
        }
        entity.setUpdatedAt(java.time.LocalDateTime.now());
        db.save(entity);
        return entity;
    }

    @Override
    public SkillEntity update(SkillEntity entity) {
        entity.setUpdatedAt(java.time.LocalDateTime.now());
        db.update(entity);
        return entity;
    }

    @Override
    public void delete(Long id) {
        db.delete(SkillEntity.class, id);
    }

    @Override
    public Optional<SkillEntity> findById(Long id) {
        SkillEntity entity = db.find(SkillEntity.class, id);
        return entity != null && entity.getDeletedAt() == null
                ? Optional.of(entity)
                : Optional.empty();
    }

    @Override
    public Optional<SkillEntity> findByName(String name) {
        List<SkillEntity> list = db.find(SkillEntity.class)
                .where()
                .eq("name", name)
                .isNull("deletedAt")
                .findList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<SkillEntity> findAll() {
        return db.find(SkillEntity.class)
                .where()
                .isNull("deletedAt")
                .findList();
    }

    @Override
    public List<SkillEntity> findByCategory(SkillCategory category) {
        return db.find(SkillEntity.class)
                .where()
                .eq("category", category.name())
                .isNull("deletedAt")
                .findList();
    }

    @Override
    public boolean existsByName(String name) {
        return findByName(name).isPresent();
    }

    @Override
    public boolean existsByNameExcludeId(String name, Long excludeId) {
        List<SkillEntity> list = db.find(SkillEntity.class)
                .where()
                .eq("name", name)
                .ne("id", excludeId)
                .isNull("deletedAt")
                .findList();
        return !list.isEmpty();
    }

    @Override
    public void softDelete(Long id) {
        findById(id).ifPresent(entity -> {
            entity.setDeletedAt(java.time.LocalDateTime.now());
            entity.setUpdatedAt(java.time.LocalDateTime.now());
            db.update(entity);
        });
    }

    @Override
    public List<SkillEntity> findAllEnabled() {
        return db.find(SkillEntity.class)
                .where()
                .eq("enabled", true)
                .isNull("deletedAt")
                .findList();
    }

    @Override
    public List<SkillEntity> findByEnabled(Boolean enabled) {
        return db.find(SkillEntity.class)
                .where()
                .eq("enabled", enabled)
                .isNull("deletedAt")
                .findList();
    }
}