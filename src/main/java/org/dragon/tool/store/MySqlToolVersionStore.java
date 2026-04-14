package org.dragon.tool.store;

import io.ebean.Database;
import org.dragon.datasource.entity.ToolVersionEntity;
import org.dragon.tool.domain.ToolVersionDO;
import org.dragon.tool.enums.ToolVersionStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlToolVersionStore — 基于 Ebean ORM 的 MySQL 存储实现。
 */
@Component
public class MySqlToolVersionStore implements ToolVersionStore {

    private final Database mysqlDb;

    public MySqlToolVersionStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(ToolVersionDO version) {
        ToolVersionEntity entity = ToolVersionEntity.fromDomain(version);
        mysqlDb.save(entity);
        version.setId(entity.getId());
    }

    @Override
    public void update(ToolVersionDO version) {
        ToolVersionEntity entity = ToolVersionEntity.fromDomain(version);
        mysqlDb.update(entity);
    }

    @Override
    public Optional<ToolVersionDO> findById(Long id) {
        ToolVersionEntity entity = mysqlDb.find(ToolVersionEntity.class, id);
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public Optional<ToolVersionDO> findByToolIdAndVersion(String toolId, int version) {
        ToolVersionEntity entity = mysqlDb.find(ToolVersionEntity.class)
                .where()
                .eq("toolId", toolId)
                .eq("version", version)
                .findOne();
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public List<ToolVersionDO> findAllByToolId(String toolId) {
        return mysqlDb.find(ToolVersionEntity.class)
                .where()
                .eq("toolId", toolId)
                .orderBy("version asc")
                .findList()
                .stream()
                .map(ToolVersionEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ToolVersionDO> findLatestByToolId(String toolId) {
        ToolVersionEntity entity = mysqlDb.find(ToolVersionEntity.class)
                .where()
                .eq("toolId", toolId)
                .orderBy("version desc")
                .setMaxRows(1)
                .findOne();
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public Optional<ToolVersionDO> findDraftByToolId(String toolId) {
        ToolVersionEntity entity = mysqlDb.find(ToolVersionEntity.class)
                .where()
                .eq("toolId", toolId)
                .eq("status", ToolVersionStatus.DRAFT.name())
                .orderBy("version desc")
                .setMaxRows(1)
                .findOne();
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public Optional<ToolVersionDO> findPublishedByToolId(String toolId) {
        ToolVersionEntity entity = mysqlDb.find(ToolVersionEntity.class)
                .where()
                .eq("toolId", toolId)
                .eq("status", ToolVersionStatus.PUBLISHED.name())
                .orderBy("version desc")
                .setMaxRows(1)
                .findOne();
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public int findMaxVersionByToolId(String toolId) {
        ToolVersionEntity entity = mysqlDb.find(ToolVersionEntity.class)
                .where()
                .eq("toolId", toolId)
                .orderBy("version desc")
                .setMaxRows(1)
                .findOne();
        return entity != null ? entity.getVersion() : 0;
    }

    @Override
    public List<ToolVersionDO> findByToolIdAndStatus(String toolId, ToolVersionStatus status) {
        return mysqlDb.find(ToolVersionEntity.class)
                .where()
                .eq("toolId", toolId)
                .eq("status", status.name())
                .orderBy("version asc")
                .findList()
                .stream()
                .map(ToolVersionEntity::toDomain)
                .collect(Collectors.toList());
    }
}

