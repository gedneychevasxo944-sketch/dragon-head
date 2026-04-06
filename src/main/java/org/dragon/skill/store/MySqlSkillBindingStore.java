package org.dragon.skill.store;

import org.dragon.skill.domain.SkillBindingDO;
import org.dragon.datasource.entity.SkillBindingEntity;
import io.ebean.Database;
import io.ebean.ExpressionList;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlSkillBindingStore — 基于 Ebean ORM 的 MySQL 存储实现。
 */
@Component
public class MySqlSkillBindingStore implements SkillBindingStore {

    private final Database mysqlDb;

    public MySqlSkillBindingStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    // ── 写操作 ────────────────────────────────────────────────────────

    @Override
    public void save(SkillBindingDO binding) {
        SkillBindingEntity entity = SkillBindingEntity.fromDomain(binding);
        mysqlDb.save(entity);
        // 回填自增主键
        binding.setId(entity.getId());
    }

    @Override
    public void update(SkillBindingDO binding) {
        SkillBindingEntity entity = SkillBindingEntity.fromDomain(binding);
        mysqlDb.update(entity);
    }

    @Override
    public void delete(Long id) {
        mysqlDb.delete(SkillBindingEntity.class, id);
    }

    // ── 读操作 ────────────────────────────────────────────────────────

    @Override
    public Optional<SkillBindingDO> findById(Long id) {
        SkillBindingEntity entity = mysqlDb.find(SkillBindingEntity.class, id);
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public List<SkillBindingDO> findByCharacterId(String characterId) {
        return mysqlDb.find(SkillBindingEntity.class)
                .where()
                .eq("bindingType", "character")
                .eq("characterId", characterId)
                .orderBy("createdAt desc")
                .findList()
                .stream()
                .map(SkillBindingEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SkillBindingDO> findByWorkspaceId(String workspaceId) {
        return mysqlDb.find(SkillBindingEntity.class)
                .where()
                .eq("bindingType", "workspace")
                .eq("workspaceId", workspaceId)
                .orderBy("createdAt desc")
                .findList()
                .stream()
                .map(SkillBindingEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SkillBindingDO> findByCharacterIdAndWorkspaceId(String characterId, String workspaceId) {
        return mysqlDb.find(SkillBindingEntity.class)
                .where()
                .eq("bindingType", "character_workspace")
                .eq("characterId", characterId)
                .eq("workspaceId", workspaceId)
                .orderBy("createdAt desc")
                .findList()
                .stream()
                .map(SkillBindingEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SkillBindingDO> findAvailableByCharacterAndWorkspace(String characterId, String workspaceId) {
        // 三路并集：character自有 + workspace公共 + character在workspace的专属
        // 使用 OR 查询合并三种条件
        List<SkillBindingEntity> results = mysqlDb.find(SkillBindingEntity.class)
                .where()
                .or()
                    // 1. Character 自有 skill
                    .and()
                        .eq("bindingType", "character")
                        .eq("characterId", characterId)
                    .endAnd()
                    // 2. Workspace 公共 skill
                    .and()
                        .eq("bindingType", "workspace")
                        .eq("workspaceId", workspaceId)
                    .endAnd()
                    // 3. Character 在该 Workspace 下的专属 skill
                    .and()
                        .eq("bindingType", "character_workspace")
                        .eq("characterId", characterId)
                        .eq("workspaceId", workspaceId)
                    .endAnd()
                .endOr()
                .orderBy("createdAt desc")
                .findList();

        return results.stream()
                .map(SkillBindingEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String bindingType, String characterId, String workspaceId, String skillId) {
        ExpressionList<SkillBindingEntity> query = mysqlDb.find(SkillBindingEntity.class)
                .where()
                .eq("bindingType", bindingType)
                .eq("skillId", skillId);

        if (characterId != null) {
            query = query.eq("characterId", characterId);
        } else {
            query = query.isNull("characterId");
        }

        if (workspaceId != null) {
            query = query.eq("workspaceId", workspaceId);
        } else {
            query = query.isNull("workspaceId");
        }

        return query.exists();
    }

    @Override
    public List<SkillBindingDO> findBySkillId(String skillId) {
        List<SkillBindingEntity> results = mysqlDb.find(SkillBindingEntity.class)
                .where()
                .eq("skillId", skillId)
                .orderBy("createdAt desc")
                .findList();

        return results.stream()
                .map(SkillBindingEntity::toDomain)
                .collect(Collectors.toList());
    }
}

