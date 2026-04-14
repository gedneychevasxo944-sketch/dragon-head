package org.dragon.skill.store;

import org.dragon.skill.domain.SkillVersionDO;
import org.dragon.datasource.entity.SkillVersionEntity;
import io.ebean.Database;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlSkillVersionStore — 基于 Ebean ORM 的 MySQL 存储实现。
 */
@Component
public class MySqlSkillVersionStore implements SkillVersionStore {

    private final Database mysqlDb;

    public MySqlSkillVersionStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(SkillVersionDO version) {
        SkillVersionEntity entity = SkillVersionEntity.fromDomain(version);
        mysqlDb.save(entity);
        version.setId(entity.getId());
    }

    @Override
    public void update(SkillVersionDO version) {
        SkillVersionEntity entity = SkillVersionEntity.fromDomain(version);
        mysqlDb.update(entity);
    }

    @Override
    public Optional<SkillVersionDO> findById(Long id) {
        SkillVersionEntity entity = mysqlDb.find(SkillVersionEntity.class, id);
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public Optional<SkillVersionDO> findBySkillIdAndVersion(String skillId, int version) {
        SkillVersionEntity entity = mysqlDb.find(SkillVersionEntity.class)
                .where()
                .eq("skillId", skillId)
                .eq("version", version)
                .findOne();
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public List<SkillVersionDO> findAllBySkillId(String skillId) {
        return mysqlDb.find(SkillVersionEntity.class)
                .where()
                .eq("skillId", skillId)
                .orderBy("version asc")
                .findList()
                .stream()
                .map(SkillVersionEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SkillVersionDO> findLatestBySkillId(String skillId) {
        SkillVersionEntity entity = mysqlDb.find(SkillVersionEntity.class)
                .where()
                .eq("skillId", skillId)
                .orderBy("version desc")
                .setMaxRows(1)
                .findOne();
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public Optional<SkillVersionDO> findDraftBySkillId(String skillId) {
        SkillVersionEntity entity = mysqlDb.find(SkillVersionEntity.class)
                .where()
                .eq("skillId", skillId)
                .eq("status", "draft")
                .orderBy("version desc")
                .setMaxRows(1)
                .findOne();
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public int findMaxVersionBySkillId(String skillId) {
        SkillVersionEntity entity = mysqlDb.find(SkillVersionEntity.class)
                .where()
                .eq("skillId", skillId)
                .orderBy("version desc")
                .setMaxRows(1)
                .findOne();
        return entity != null ? entity.getVersion() : 0;
    }
}
