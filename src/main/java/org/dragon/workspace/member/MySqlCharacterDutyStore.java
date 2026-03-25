package org.dragon.workspace.member;

import io.ebean.Database;
import org.dragon.datasource.entity.CharacterDutyEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlCharacterDutyStore Character职责MySQL存储实现
 */
@Component
public class MySqlCharacterDutyStore implements CharacterDutyStore {

    private final Database mysqlDb;

    public MySqlCharacterDutyStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(CharacterDuty duty) {
        CharacterDutyEntity entity = CharacterDutyEntity.fromCharacterDuty(duty);
        mysqlDb.save(entity);
    }

    @Override
    public void update(CharacterDuty duty) {
        CharacterDutyEntity entity = CharacterDutyEntity.fromCharacterDuty(duty);
        mysqlDb.update(entity);
    }

    @Override
    public void delete(String id) {
        mysqlDb.delete(CharacterDutyEntity.class, id);
    }

    @Override
    public Optional<CharacterDuty> findById(String id) {
        CharacterDutyEntity entity = mysqlDb.find(CharacterDutyEntity.class, id);
        return entity != null ? Optional.of(entity.toCharacterDuty()) : Optional.empty();
    }

    @Override
    public Optional<CharacterDuty> findByWorkspaceIdAndCharacterId(String workspaceId, String characterId) {
        String id = CharacterDuty.createId(workspaceId, characterId);
        return findById(id);
    }

    @Override
    public List<CharacterDuty> findByWorkspaceId(String workspaceId) {
        return mysqlDb.find(CharacterDutyEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .findList()
                .stream()
                .map(CharacterDutyEntity::toCharacterDuty)
                .collect(Collectors.toList());
    }
}
