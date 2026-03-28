package org.dragon.skill.store;

import io.ebean.Database;
import org.dragon.skill.entity.SkillBindEntity;
import org.dragon.skill.enums.BindType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * SkillBind 存储实现。
 * 使用 Ebean ORM 进行数据库操作。
 *
 * @since 1.0
 */
@Component
public class SkillBindStore {

    private final Database db;

    public SkillBindStore(@Qualifier("mysqlEbeanDatabase") Database db) {
        this.db = db;
    }

    /**
     * 保存 SkillBind 关联。
     */
    public SkillBindEntity save(SkillBindEntity entity) {
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        db.save(entity);
        return entity;
    }

    /**
     * 更新 SkillBind 关联。
     */
    public SkillBindEntity update(SkillBindEntity entity) {
        entity.setUpdatedAt(LocalDateTime.now());
        db.update(entity);
        return entity;
    }

    /**
     * 删除 SkillBind 关联。
     */
    public void delete(Long id) {
        db.delete(SkillBindEntity.class, id);
    }

    /**
     * 按 ID 查询。
     */
    public Optional<SkillBindEntity> findById(Long id) {
        SkillBindEntity entity = db.find(SkillBindEntity.class, id);
        return Optional.ofNullable(entity);
    }

    // ==================== WORKSPACE 绑定查询 ====================

    /**
     * 查询 workspace 圈选的所有启用 skill 关联记录。
     */
    public List<SkillBindEntity> findAllEnabledByWorkspace(Long workspaceId) {
        return db.find(SkillBindEntity.class)
                .where()
                .eq("bindType", BindType.WORKSPACE)
                .eq("workspaceId", workspaceId)
                .eq("enabled", true)
                .eq("skill.enabled", true)
                .isNull("skill.deletedAt")
                .findList();
    }

    /**
     * 查询 workspace 中某个 skill 的关联记录。
     */
    public Optional<SkillBindEntity> findByWorkspaceAndSkill(Long workspaceId, Long skillId) {
        SkillBindEntity entity = db.find(SkillBindEntity.class)
                .where()
                .eq("bindType", BindType.WORKSPACE)
                .eq("workspaceId", workspaceId)
                .eq("skill.id", skillId)
                .findOne();
        return Optional.ofNullable(entity);
    }

    // ==================== CHARACTER 绑定查询 ====================

    /**
     * 查询 character 绑定的所有启用 skill 关联记录。
     * 含 CHARACTER 和 CHARACTER_WORKSPACE 类型。
     */
    public List<SkillBindEntity> findAllEnabledByCharacter(String characterId) {
        return db.find(SkillBindEntity.class)
                .where()
                .eq("characterId", characterId)
                .eq("enabled", true)
                .eq("skill.enabled", true)
                .isNull("skill.deletedAt")
                .findList();
    }

    /**
     * 查询 character 中某个 skill 的关联记录（指定 workspace）。
     */
    public Optional<SkillBindEntity> findByCharacterAndSkill(String characterId, Long workspaceId, Long skillId) {
        SkillBindEntity entity = db.find(SkillBindEntity.class)
                .where()
                .eq("characterId", characterId)
                .eq("workspaceId", workspaceId)
                .eq("skill.id", skillId)
                .findOne();
        return Optional.ofNullable(entity);
    }

    /**
     * 查询 character 绑定某个 skill 的记录（不指定 workspace）。
     */
    public Optional<SkillBindEntity> findByCharacterAndSkillGlobal(String characterId, Long skillId) {
        SkillBindEntity entity = db.find(SkillBindEntity.class)
                .where()
                .eq("characterId", characterId)
                .isNull("workspaceId")
                .eq("skill.id", skillId)
                .findOne();
        return Optional.ofNullable(entity);
    }

    // ==================== 有效技能解析（合并优先级）====================

    /**
     * 解析 character 在特定 workspace 下的有效技能（合并优先级）。
     * 优先级：CHARACTER_WORKSPACE > CHARACTER > WORKSPACE > GLOBAL
     *
     * @param characterId character ID
     * @param workspaceId workspace ID
     * @return 合并后的有效 skill 列表
     */
    public List<SkillBindEntity> resolveEffectiveBinds(String characterId, Long workspaceId) {
        // 获取所有相关绑定
        List<SkillBindEntity> allBinds = db.find(SkillBindEntity.class)
                .where()
                .eq("characterId", characterId)
                .eq("enabled", true)
                .eq("skill.enabled", true)
                .isNull("skill.deletedAt")
                .findList();

        // 按 skill 分组，取最高优先级
        java.util.Map<Long, SkillBindEntity> bestBySkill = new java.util.LinkedHashMap<>();

        for (SkillBindEntity bind : allBinds) {
            Long skillId = bind.getSkill().getId();
            SkillBindEntity existing = bestBySkill.get(skillId);

            if (existing == null) {
                bestBySkill.put(skillId, bind);
            } else {
                // 比较优先级
                if (getPriority(bind) > getPriority(existing)) {
                    bestBySkill.put(skillId, bind);
                }
            }
        }

        return new java.util.ArrayList<>(bestBySkill.values());
    }

    /**
     * 获取绑定的优先级。
     * 数值越高优先级越高。
     */
    private int getPriority(SkillBindEntity bind) {
        return switch (bind.getBindType()) {
            case CHARACTER_WORKSPACE -> 3;
            case CHARACTER -> 2;
            case WORKSPACE -> 1;
        };
    }

    // ==================== 热更新相关 ====================

    /**
     * 查询所有开启了"跟随最新版本"的关联记录。
     */
    public List<SkillBindEntity> findAllUseLatestBySkillId(Long skillId) {
        return db.find(SkillBindEntity.class)
                .where()
                .eq("skill.id", skillId)
                .eq("useLatest", true)
                .eq("enabled", true)
                .findList();
    }

    /**
     * 批量更新 pinnedVersion。
     */
    public void updatePinnedVersionForLatestFollowers(Long skillId, Integer newVersion) {
        List<SkillBindEntity> entities = findAllUseLatestBySkillId(skillId);
        LocalDateTime now = LocalDateTime.now();
        for (SkillBindEntity entity : entities) {
            entity.setPinnedVersion(newVersion);
            entity.setUpdatedAt(now);
            db.update(entity);
        }
    }

    // ==================== 存在性检查 ====================

    /**
     * 检查 workspace 是否已绑定某个 skill。
     */
    public boolean existsByWorkspaceAndSkill(Long workspaceId, Long skillId) {
        return db.find(SkillBindEntity.class)
                .where()
                .eq("bindType", BindType.WORKSPACE)
                .eq("workspaceId", workspaceId)
                .eq("skill.id", skillId)
                .exists();
    }

    /**
     * 检查 character 是否已绑定某个 skill。
     */
    public boolean existsByCharacterAndSkill(String characterId, Long workspaceId, Long skillId) {
        return db.find(SkillBindEntity.class)
                .where()
                .eq("characterId", characterId)
                .eq("workspaceId", workspaceId)
                .eq("skill.id", skillId)
                .exists();
    }

    /**
     * 查询圈选了指定 skill 的所有 workspaceId。
     */
    public List<Long> findWorkspaceIdsBySkillId(Long skillId) {
        return db.find(SkillBindEntity.class)
                .where()
                .eq("bindType", BindType.WORKSPACE)
                .eq("skill.id", skillId)
                .eq("enabled", true)
                .findList()
                .stream()
                .map(SkillBindEntity::getWorkspaceId)
                .distinct()
                .toList();
    }

    /**
     * 查询圈选了指定 skill 的所有启用关联记录（含 useLatest=false 的）。
     */
    public List<SkillBindEntity> findAllEnabledBySkillId(Long skillId) {
        return db.find(SkillBindEntity.class)
                .where()
                .eq("skill.id", skillId)
                .eq("enabled", true)
                .findList();
    }
}