package org.dragon.workspace.member;

import io.ebean.Database;
import org.dragon.datasource.entity.WorkspaceMemberEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlWorkspaceMemberStore 工作空间成员MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlWorkspaceMemberStore implements WorkspaceMemberStore {

    private final Database mysqlDb;

    public MySqlWorkspaceMemberStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(WorkspaceMember member) {
        WorkspaceMemberEntity entity = WorkspaceMemberEntity.fromWorkspaceMember(member);
        mysqlDb.save(entity);
    }

    @Override
    public Optional<WorkspaceMember> findById(String memberId) {
        WorkspaceMemberEntity entity = mysqlDb.find(WorkspaceMemberEntity.class, memberId);
        return entity != null ? Optional.of(entity.toWorkspaceMember()) : Optional.empty();
    }

    @Override
    public List<WorkspaceMember> findByWorkspaceId(String workspaceId) {
        return mysqlDb.find(WorkspaceMemberEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .findList()
                .stream()
                .map(WorkspaceMemberEntity::toWorkspaceMember)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<WorkspaceMember> findByWorkspaceIdAndCharacterId(String workspaceId, String characterId) {
        String memberId = WorkspaceMember.createId(workspaceId, characterId);
        return findById(memberId);
    }

    @Override
    public List<WorkspaceMember> findByCharacterId(String characterId) {
        return mysqlDb.find(WorkspaceMemberEntity.class)
                .where()
                .eq("characterId", characterId)
                .findList()
                .stream()
                .map(WorkspaceMemberEntity::toWorkspaceMember)
                .collect(Collectors.toList());
    }

    @Override
    public void update(WorkspaceMember member) {
        WorkspaceMemberEntity entity = WorkspaceMemberEntity.fromWorkspaceMember(member);
        mysqlDb.update(entity);
    }

    @Override
    public void delete(String memberId) {
        mysqlDb.delete(WorkspaceMemberEntity.class, memberId);
    }

    @Override
    public void deleteByWorkspaceId(String workspaceId) {
        mysqlDb.find(WorkspaceMemberEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .delete();
    }

    @Override
    public int countByWorkspaceId(String workspaceId) {
        return (int) mysqlDb.find(WorkspaceMemberEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .findCount();
    }
}
