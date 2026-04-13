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
        // Fetch existing entity first to preserve fields not being updated
        CharacterEntity existing = mysqlDb.find(CharacterEntity.class, character.getId());
        if (existing != null) {
            entity.setCreatedAt(existing.getCreatedAt());
            // Merge non-null fields from incoming entity
            mergeIfNotNull(entity, existing);
        }
        mysqlDb.update(entity);
    }

    /**
     * 将源实体中的非null字段合并到目标实体
     */
    private void mergeIfNotNull(CharacterEntity target, CharacterEntity source) {
        if (target.getName() == null) {
            target.setName(source.getName());
        }
        if (target.getDescription() == null) {
            target.setDescription(source.getDescription());
        }
        if (target.getAvatar() == null) {
            target.setAvatar(source.getAvatar());
        }
        if (target.getSource() == null) {
            target.setSource(source.getSource());
        }
        if (target.getAllowedTools() == null) {
            target.setAllowedTools(source.getAllowedTools());
        }
        if (target.getPromptTemplate() == null) {
            target.setPromptTemplate(source.getPromptTemplate());
        }
        if (target.getDefaultTools() == null) {
            target.setDefaultTools(source.getDefaultTools());
        }
        if (target.getIsRunning() == null) {
            target.setIsRunning(source.getIsRunning());
        }
        if (target.getDeployedCount() == null) {
            target.setDeployedCount(source.getDeployedCount());
        }
        if (target.getMindConfig() == null) {
            target.setMindConfig(source.getMindConfig());
        }
        if (target.getExtensions() == null) {
            target.setExtensions(source.getExtensions());
        }
        if (target.getStatus() == null) {
            target.setStatus(source.getStatus());
        }
        if (target.getMbti() == null) {
            target.setMbti(source.getMbti());
        }
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