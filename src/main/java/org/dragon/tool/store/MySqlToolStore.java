package org.dragon.tool.store;

import io.ebean.Database;
import org.dragon.datasource.entity.ToolEntity;
import org.dragon.tool.domain.ToolDO;
import org.dragon.tool.enums.ToolStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlToolStore — 基于 Ebean ORM 的 MySQL 存储实现。
 */
@Component
public class MySqlToolStore implements ToolStore {

    private final Database mysqlDb;

    public MySqlToolStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(ToolDO tool) {
        ToolEntity entity = ToolEntity.fromDomain(tool);
        mysqlDb.save(entity);
    }

    @Override
    public void update(ToolDO tool) {
        ToolEntity entity = ToolEntity.fromDomain(tool);
        mysqlDb.update(entity);
    }

    @Override
    public Optional<ToolDO> findById(String toolId) {
        ToolEntity entity = mysqlDb.find(ToolEntity.class, toolId);
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public List<ToolDO> findByIds(List<String> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return List.of();
        }
        return mysqlDb.find(ToolEntity.class)
                .where()
                .in("id", toolIds)
                .isNull("deletedAt")
                .findList()
                .stream()
                .map(ToolEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ToolDO> findAllBuiltin() {
        return mysqlDb.find(ToolEntity.class)
                .where()
                .eq("builtin", true)
                .eq("status", ToolStatus.ACTIVE.name())
                .isNotNull("publishedVersionId")
                .isNull("deletedAt")
                .orderBy("name asc")
                .findList()
                .stream()
                .map(ToolEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(String toolId) {
        return mysqlDb.find(ToolEntity.class)
                .where()
                .eq("id", toolId)
                .exists();
    }

    @Override
    public List<ToolDO> findAll() {
        return mysqlDb.find(ToolEntity.class)
                .where()
                .isNull("deletedAt")
                .orderBy("createdAt desc")
                .findList()
                .stream()
                .map(ToolEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ToolDO> findByName(String name) {
        ToolEntity entity = mysqlDb.find(ToolEntity.class)
                .where()
                .eq("name", name)
                .isNull("deletedAt")
                .findOne();
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public void disableByNamePrefix(String namePrefix) {
        mysqlDb.sqlUpdate(
                "UPDATE tool SET status = :status WHERE name LIKE :prefix AND status = :activeStatus"
        )
                .setParameter("status", ToolStatus.DISABLED.name())
                .setParameter("prefix", namePrefix + "%")
                .setParameter("activeStatus", ToolStatus.ACTIVE.name())
                .execute();
    }
}

