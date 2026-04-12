package org.dragon.character.store;

import io.ebean.Database;
import org.dragon.asset.enums.AssociationType;
import org.dragon.asset.store.AssetAssociationStore;
import org.dragon.character.Character;
import org.dragon.character.profile.CharacterProfile;
import org.dragon.datasource.entity.AssetAssociationEntity;
import org.dragon.datasource.entity.CharacterEntity;
import org.dragon.permission.enums.ResourceType;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlCharacterStore Character MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlCharacterStore implements CharacterStore {

    private final Database mysqlDb;

    public MySqlCharacterStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(Character character) {
        CharacterEntity entity = CharacterEntity.fromCharacter(character);
        mysqlDb.save(entity);
    }

    @Override
    public void update(Character character) {
        CharacterEntity entity = CharacterEntity.fromCharacter(character);
        mysqlDb.update(entity);
    }

    @Override
    public void delete(String id) {
        mysqlDb.delete(CharacterEntity.class, id);
    }

    @Override
    public Optional<Character> findById(String id) {
        CharacterEntity entity = mysqlDb.find(CharacterEntity.class, id);
        return entity != null ? Optional.of(entity.toCharacter()) : Optional.empty();
    }

    @Override
    public List<Character> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mysqlDb.find(CharacterEntity.class)
                .where()
                .in("id", ids)
                .findList()
                .stream()
                .map(CharacterEntity::toCharacter)
                .collect(Collectors.toList());
    }

    @Override
    public List<Character> findAll() {
        return mysqlDb.find(CharacterEntity.class)
                .findList()
                .stream()
                .map(CharacterEntity::toCharacter)
                .collect(Collectors.toList());
    }

    @Override
    public List<Character> findByStatus(CharacterProfile.Status status) {
        return mysqlDb.find(CharacterEntity.class)
                .where()
                .eq("status", status.name())
                .findList()
                .stream()
                .map(CharacterEntity::toCharacter)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String id) {
        return mysqlDb.find(CharacterEntity.class, id) != null;
    }

    @Override
    public int count() {
        return (int) mysqlDb.find(CharacterEntity.class).findCount();
    }
}