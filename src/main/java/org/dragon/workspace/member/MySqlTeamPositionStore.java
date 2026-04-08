package org.dragon.workspace.member;

import io.ebean.Database;
import org.dragon.datasource.entity.TeamPositionEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlTeamPositionStore 团队席位MySQL存储实现
 *
 * @author qieqie
 * @version 1.0
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlTeamPositionStore implements TeamPositionStore {

    private final Database mysqlDb;

    public MySqlTeamPositionStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(TeamPosition position) {
        TeamPositionEntity entity = TeamPositionEntity.fromTeamPosition(position);
        mysqlDb.save(entity);
    }

    @Override
    public void update(TeamPosition position) {
        TeamPositionEntity entity = TeamPositionEntity.fromTeamPosition(position);
        mysqlDb.update(entity);
    }

    @Override
    public void delete(String id) {
        mysqlDb.delete(TeamPositionEntity.class, id);
    }

    @Override
    public Optional<TeamPosition> findById(String id) {
        TeamPositionEntity entity = mysqlDb.find(TeamPositionEntity.class, id);
        return entity != null ? Optional.of(entity.toTeamPosition()) : Optional.empty();
    }

    @Override
    public List<TeamPosition> findByWorkspaceId(String workspaceId) {
        return mysqlDb.find(TeamPositionEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .findList()
                .stream()
                .map(TeamPositionEntity::toTeamPosition)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<TeamPosition> findByWorkspaceIdAndRoleName(String workspaceId, String roleName) {
        TeamPositionEntity entity = mysqlDb.find(TeamPositionEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .eq("roleName", roleName)
                .findOne();
        return entity != null ? Optional.of(entity.toTeamPosition()) : Optional.empty();
    }
}
