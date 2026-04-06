package org.dragon.skill.store;

import org.dragon.datasource.entity.SkillActionLogEntity;
import org.dragon.skill.actionlog.SkillActionLog;
import org.dragon.skill.enums.SkillActionType;
import io.ebean.Database;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MySqlSkillActionLogStore — 基于 Ebean ORM 的 MySQL 存储实现。
 */
@Component
public class MySqlSkillActionLogStore implements SkillActionLogStore {

    private final Database mysqlDb;

    public MySqlSkillActionLogStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(SkillActionLog log) {
        SkillActionLogEntity entity = SkillActionLogEntity.fromDomain(log);
        mysqlDb.save(entity);
    }

    @Override
    public List<SkillActionLog> findBySkillId(String skillId, int offset, int limit) {
        return mysqlDb.find(SkillActionLogEntity.class)
                .where()
                .eq("skillId", skillId)
                .orderBy("createdAt desc")
                .setFirstRow(offset)
                .setMaxRows(limit)
                .findList()
                .stream()
                .map(SkillActionLogEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int countBySkillId(String skillId) {
        return (int) mysqlDb.find(SkillActionLogEntity.class)
                .where()
                .eq("skillId", skillId)
                .findCount();
    }

    @Override
    public List<SkillActionLog> findBySkillIdAndActionType(String skillId, SkillActionType actionType,
                                                           int offset, int limit) {
        return mysqlDb.find(SkillActionLogEntity.class)
                .where()
                .eq("skillId", skillId)
                .eq("actionType", actionType.name())
                .orderBy("createdAt desc")
                .setFirstRow(offset)
                .setMaxRows(limit)
                .findList()
                .stream()
                .map(SkillActionLogEntity::toDomain)
                .collect(Collectors.toList());
    }
}
