package org.dragon.trait.store;

import io.ebean.Database;
import org.dragon.datasource.entity.TraitEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * MySQL 实现 Trait 存储
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlTraitStore implements TraitStore {

    private final Database db;

    public MySqlTraitStore(@Qualifier("mysqlEbeanDatabase") Database db) {
        this.db = db;
    }

    @Override
    public void save(TraitEntity trait) {
        if (trait.getId() == null) {
            trait.setCreateTime(java.time.LocalDateTime.now());
        }
        trait.setUpdateTime(java.time.LocalDateTime.now());
        db.save(trait);
    }

    @Override
    public void update(TraitEntity trait) {
        trait.setUpdateTime(java.time.LocalDateTime.now());
        db.update(trait);
    }

    @Override
    public Optional<TraitEntity> findById(Long id) {
        TraitEntity entity = db.find(TraitEntity.class, id);
        return Optional.ofNullable(entity);
    }

    @Override
    public List<TraitEntity> findAll() {
        return db.find(TraitEntity.class)
                .orderBy("createTime desc")
                .findList();
    }

    @Override
    public List<TraitEntity> findByType(String type) {
        return db.find(TraitEntity.class)
                .where()
                .eq("type", type)
                .orderBy("createTime desc")
                .findList();
    }

    @Override
    public List<TraitEntity> findByCategory(String category) {
        return db.find(TraitEntity.class)
                .where()
                .eq("category", category)
                .orderBy("createTime desc")
                .findList();
    }

    @Override
    public List<TraitEntity> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        String pattern = "%" + keyword.toLowerCase() + "%";
        return db.find(TraitEntity.class)
                .where()
                .or()
                .ilike("name", pattern)
                .ilike("description", pattern)
                .endOr()
                .orderBy("createTime desc")
                .findList();
    }

    @Override
    public long count() {
        return db.find(TraitEntity.class).findCount();
    }

    @Override
    public void delete(Long id) {
        db.delete(TraitEntity.class, id);
    }

    @Override
    public void incrementUsedByCount(Long id) {
        findById(id).ifPresent(trait -> {
            trait.setUsedByCount(trait.getUsedByCount() + 1);
            update(trait);
        });
    }

    @Override
    public void decrementUsedByCount(Long id) {
        findById(id).ifPresent(trait -> {
            int newCount = Math.max(0, trait.getUsedByCount() - 1);
            trait.setUsedByCount(newCount);
            update(trait);
        });
    }
}
