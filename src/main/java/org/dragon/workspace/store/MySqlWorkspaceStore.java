package org.dragon.workspace.store;

import io.ebean.Database;
import org.dragon.datasource.entity.WorkspaceEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.dragon.workspace.Workspace;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlWorkspaceStore 工作空间MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlWorkspaceStore implements WorkspaceStore {

    private final Database mysqlDb;

    public MySqlWorkspaceStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(Workspace workspace) {
        WorkspaceEntity entity = WorkspaceEntity.fromWorkspace(workspace);
        mysqlDb.save(entity);
    }

    @Override
    public void update(Workspace workspace) {
        WorkspaceEntity entity = WorkspaceEntity.fromWorkspace(workspace);
        // Fetch existing entity first to preserve fields not being updated
        WorkspaceEntity existing = mysqlDb.find(WorkspaceEntity.class, workspace.getId());
        if (existing != null) {
            entity.setCreatedAt(existing.getCreatedAt());
            mergeIfNotNull(entity, existing);
        }
        mysqlDb.update(entity);
    }

    private void mergeIfNotNull(WorkspaceEntity target, WorkspaceEntity source) {
        if (target.getName() == null) {
            target.setName(source.getName());
        }
        if (target.getDescription() == null) {
            target.setDescription(source.getDescription());
        }
        if (target.getOwner() == null) {
            target.setOwner(source.getOwner());
        }
        if (target.getStatus() == null) {
            target.setStatus(source.getStatus());
        }
        if (target.getProperties() == null) {
            target.setProperties(source.getProperties());
        }
        if (target.getPersonality() == null) {
            target.setPersonality(source.getPersonality());
        }
    }

    @Override
    public void delete(String id) {
        mysqlDb.delete(WorkspaceEntity.class, id);
    }

    @Override
    public Optional<Workspace> findById(String id) {
        WorkspaceEntity entity = mysqlDb.find(WorkspaceEntity.class, id);
        return entity != null ? Optional.of(entity.toWorkspace()) : Optional.empty();
    }

    @Override
    public List<Workspace> findAll() {
        return mysqlDb.find(WorkspaceEntity.class)
                .findList()
                .stream()
                .map(WorkspaceEntity::toWorkspace)
                .collect(Collectors.toList());
    }

    @Override
    public List<Workspace> findByStatus(Workspace.Status status) {
        return mysqlDb.find(WorkspaceEntity.class)
                .where()
                .eq("status", status.name())
                .findList()
                .stream()
                .map(WorkspaceEntity::toWorkspace)
                .collect(Collectors.toList());
    }

    @Override
    public List<Workspace> findByOwner(String owner) {
        return mysqlDb.find(WorkspaceEntity.class)
                .where()
                .eq("owner", owner)
                .findList()
                .stream()
                .map(WorkspaceEntity::toWorkspace)
                .collect(Collectors.toList());
    }

    @Override
    public List<Workspace> findByIds(List<String> ids) {
        return mysqlDb.find(WorkspaceEntity.class)
                .where()
                .in("id", ids)
                .findList()
                .stream()
                .map(WorkspaceEntity::toWorkspace)
                .collect(Collectors.toList());
    }

    @Override
    public List<Workspace> findByIdsAndStatus(List<String> ids, Workspace.Status status) {
        return mysqlDb.find(WorkspaceEntity.class)
                .where()
                .in("id", ids)
                .eq("status", status.name())
                .findList()
                .stream()
                .map(WorkspaceEntity::toWorkspace)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String id) {
        return mysqlDb.find(WorkspaceEntity.class, id) != null;
    }

    @Override
    public int count() {
        return (int) mysqlDb.find(WorkspaceEntity.class).findCount();
    }
}
