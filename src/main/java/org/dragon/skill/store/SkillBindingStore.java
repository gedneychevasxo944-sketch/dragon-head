package org.dragon.skill.store;

import io.ebean.Database;
import org.dragon.skill.entity.SkillBindingEntity;
import org.dragon.skill.enums.BindType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * SkillBinding 存储实现。
 * 使用 Ebean ORM 进行数据库操作。
 *
 * @since 1.0
 */
@Component
public class SkillBindingStore {

    private final Database db;

    public SkillBindingStore(@Qualifier("mysqlEbeanDatabase") Database db) {
        this.db = db;
    }

    // ==================== 基础 CRUD ====================

    /**
     * 保存 SkillBinding 关联。
     */
    public SkillBindingEntity save(SkillBindingEntity entity) {
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        db.save(entity);
        return entity;
    }

    /**
     * 更新 SkillBinding 关联。
     */
    public SkillBindingEntity update(SkillBindingEntity entity) {
        entity.setUpdatedAt(LocalDateTime.now());
        db.update(entity);
        return entity;
    }

    /**
     * 删除 SkillBinding 关联。
     */
    public void delete(Long id) {
        db.delete(SkillBindingEntity.class, id);
    }

    /**
     * 按 ID 查询。
     */
    public Optional<SkillBindingEntity> findById(Long id) {
        SkillBindingEntity entity = db.find(SkillBindingEntity.class, id);
        return Optional.ofNullable(entity);
    }

    // ==================== 统一查询接口 ====================

    /**
     * 统一根据条件查询单个绑定记录。
     *
     * @param bindType 绑定类型（可选，用于过滤 WORKSPACE/CHARACTER/CHARACTER_WORKSPACE）
     * @param workspaceId workspace ID（可选）
     * @param characterId character ID（可选）
     * @param skillId skill ID（可选）
     * @param globalOnly 是否只查询 global（CHARACTER 类型不带 workspace 时为 true）
     */
    public Optional<SkillBindingEntity> findBinding(
            BindType bindType,
            Long workspaceId,
            String characterId,
            Long skillId,
            boolean globalOnly) {

        var query = db.find(SkillBindingEntity.class).where();

        if (bindType != null) {
            query.eq("bindType", bindType);
        }
        if (workspaceId != null && characterId == null) {
            // Workspace 类型
            query.eq("workspaceId", workspaceId);
        }
        if (characterId != null) {
            query.eq("characterId", characterId);
            if (globalOnly) {
                // global 查询：workspaceId 为 null
                query.isNull("workspaceId");
            } else if (workspaceId != null) {
                // 指定 workspace 的查询
                query.eq("workspaceId", workspaceId);
            }
        }
        if (skillId != null) {
            query.eq("skill.id", skillId);
        }

        return Optional.ofNullable(query.findOne());
    }

    /**
     * 查询启用状态的绑定列表。
     *
     * @param bindType 绑定类型（可选）
     * @param workspaceId workspace ID（可选）
     * @param characterId character ID（可选）
     */
    public List<SkillBindingEntity> findEnabledBindings(
            BindType bindType,
            Long workspaceId,
            String characterId) {

        var query = db.find(SkillBindingEntity.class).where()
                .eq("enabled", true)
                .eq("skill.enabled", true)
                .isNull("skill.deletedAt");

        if (bindType != null) {
            query.eq("bindType", bindType);
        }
        if (workspaceId != null && characterId == null) {
            query.eq("workspaceId", workspaceId);
        }
        if (characterId != null) {
            query.eq("characterId", characterId);
        }

        return query.findList();
    }

    /**
     * 检查绑定是否存在。
     *
     * @param skillId skill ID
     * @param bindType 绑定类型（可选）
     * @param workspaceId workspace ID（可选）
     * @param characterId character ID（可选）
     * @param globalOnly 是否只查询 global
     */
    public boolean bindingExists(
            Long skillId,
            BindType bindType,
            Long workspaceId,
            String characterId,
            boolean globalOnly) {

        var query = db.find(SkillBindingEntity.class).where()
                .eq("skill.id", skillId);

        if (bindType != null) {
            query.eq("bindType", bindType);
        }
        if (workspaceId != null && characterId == null) {
            query.eq("workspaceId", workspaceId);
        }
        if (characterId != null) {
            query.eq("characterId", characterId);
            if (globalOnly) {
                query.isNull("workspaceId");
            } else if (workspaceId != null) {
                query.eq("workspaceId", workspaceId);
            }
        }

        return query.exists();
    }

    // ==================== 兼容方法（可选保留，标识为废弃）====================

    /**
     * @deprecated 使用 {@link #findBinding(BindType, Long, String, Long, boolean)}
     */
    @Deprecated
    public Optional<SkillBindingEntity> findByWorkspaceAndSkill(Long workspaceId, Long skillId) {
        return findBinding(BindType.WORKSPACE, workspaceId, null, skillId, false);
    }

    /**
     * @deprecated 使用 {@link #findBinding(BindType, Long, String, Long, boolean)}
     */
    @Deprecated
    public Optional<SkillBindingEntity> findByCharacterAndSkill(String characterId, Long workspaceId, Long skillId) {
        return findBinding(null, workspaceId, characterId, skillId, false);
    }

    /**
     * @deprecated 使用 {@link #findBinding(BindType, Long, String, Long, boolean)}
     */
    @Deprecated
    public Optional<SkillBindingEntity> findByCharacterAndSkillGlobal(String characterId, Long skillId) {
        return findBinding(BindType.CHARACTER, null, characterId, skillId, true);
    }

    /**
     * @deprecated 使用 {@link #findEnabledBindings(BindType, Long, String)}
     */
    @Deprecated
    public List<SkillBindingEntity> findAllEnabledByWorkspace(Long workspaceId) {
        return findEnabledBindings(BindType.WORKSPACE, workspaceId, null);
    }

    /**
     * @deprecated 使用 {@link #findEnabledBindings(BindType, Long, String)}
     */
    @Deprecated
    public List<SkillBindingEntity> findAllEnabledByCharacter(String characterId) {
        return findEnabledBindings(null, null, characterId);
    }

    /**
     * @deprecated 使用 {@link #bindingExists(Long, BindType, Long, String, boolean)}
     */
    @Deprecated
    public boolean existsByWorkspaceAndSkill(Long workspaceId, Long skillId) {
        return bindingExists(skillId, BindType.WORKSPACE, workspaceId, null, false);
    }

    /**
     * @deprecated 使用 {@link #bindingExists(Long, BindType, Long, String, boolean)}
     */
    @Deprecated
    public boolean existsByCharacterAndSkill(String characterId, Long workspaceId, Long skillId) {
        return bindingExists(skillId, null, workspaceId, characterId, false);
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
    public List<SkillBindingEntity> resolveEffectiveBinds(String characterId, Long workspaceId) {
        List<SkillBindingEntity> allBinds = db.find(SkillBindingEntity.class)
                .where()
                .eq("characterId", characterId)
                .eq("enabled", true)
                .eq("skill.enabled", true)
                .isNull("skill.deletedAt")
                .findList();

        java.util.Map<Long, SkillBindingEntity> bestBySkill = new java.util.LinkedHashMap<>();

        for (SkillBindingEntity bind : allBinds) {
            Long skillId = bind.getSkill().getId();
            SkillBindingEntity existing = bestBySkill.get(skillId);

            if (existing == null) {
                bestBySkill.put(skillId, bind);
            } else if (getPriority(bind) > getPriority(existing)) {
                bestBySkill.put(skillId, bind);
            }
        }

        return new java.util.ArrayList<>(bestBySkill.values());
    }

    /**
     * 获取绑定的优先级。
     * 数值越高优先级越高。
     */
    private int getPriority(SkillBindingEntity bind) {
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
    public List<SkillBindingEntity> findAllUseLatestBySkillId(Long skillId) {
        return db.find(SkillBindingEntity.class)
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
        List<SkillBindingEntity> entities = findAllUseLatestBySkillId(skillId);
        LocalDateTime now = LocalDateTime.now();
        for (SkillBindingEntity entity : entities) {
            entity.setPinnedVersion(newVersion);
            entity.setUpdatedAt(now);
            db.update(entity);
        }
    }

    /**
     * 查询圈选了指定 skill 的所有 workspaceId。
     */
    public List<Long> findWorkspaceIdsBySkillId(Long skillId) {
        return db.find(SkillBindingEntity.class)
                .where()
                .eq("bindType", BindType.WORKSPACE)
                .eq("skill.id", skillId)
                .eq("enabled", true)
                .findList()
                .stream()
                .map(SkillBindingEntity::getWorkspaceId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 查询圈选了指定 skill 的所有启用关联记录（含 useLatest=false 的）。
     */
    public List<SkillBindingEntity> findAllEnabledBySkillId(Long skillId) {
        return db.find(SkillBindingEntity.class)
                .where()
                .eq("skill.id", skillId)
                .eq("enabled", true)
                .findList();
    }
}