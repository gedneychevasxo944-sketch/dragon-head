package org.dragon.skill.store;

import io.ebean.Database;
import org.dragon.skill.entity.WorkspaceSkillEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * WorkspaceSkill 存储实现。
 * 使用 Ebean ORM 进行数据库操作。
 *
 * @since 1.0
 */
@Component
public class WorkspaceSkillStore {

    private final Database db;

    public WorkspaceSkillStore(@Qualifier("mysqlEbeanDatabase") Database db) {
        this.db = db;
    }

    /**
     * 保存 WorkspaceSkill 关联。
     */
    public WorkspaceSkillEntity save(WorkspaceSkillEntity entity) {
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        db.save(entity);
        return entity;
    }

    /**
     * 更新 WorkspaceSkill 关联。
     */
    public WorkspaceSkillEntity update(WorkspaceSkillEntity entity) {
        entity.setUpdatedAt(LocalDateTime.now());
        db.update(entity);
        return entity;
    }

    /**
     * 删除 WorkspaceSkill 关联。
     */
    public void delete(Long id) {
        db.delete(WorkspaceSkillEntity.class, id);
    }

    /**
     * 按 ID 查询。
     */
    public Optional<WorkspaceSkillEntity> findById(Long id) {
        WorkspaceSkillEntity entity = db.find(WorkspaceSkillEntity.class, id);
        return Optional.ofNullable(entity);
    }

    /**
     * 查询 workspace 圈选的所有启用 skill 关联记录。
     */
    public List<WorkspaceSkillEntity> findAllEnabledByWorkspace(Long workspaceId) {
        return db.find(WorkspaceSkillEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .eq("enabled", true)
                .eq("skill.enabled", true)
                .isNull("skill.deletedAt")
                .findList();
    }

    /**
     * 查询 workspace 中某个 skill 的关联记录。
     */
    public Optional<WorkspaceSkillEntity> findByWorkspaceIdAndSkillId(Long workspaceId, Long skillId) {
        WorkspaceSkillEntity entity = db.find(WorkspaceSkillEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .eq("skill.id", skillId)
                .findOne();
        return Optional.ofNullable(entity);
    }

    /**
     * 查询所有开启了"跟随最新版本"的关联记录。
     */
    public List<WorkspaceSkillEntity> findAllUseLatestBySkillId(Long skillId) {
        return db.find(WorkspaceSkillEntity.class)
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
        // 先查询所有 useLatest=true 的记录，再更新
        List<WorkspaceSkillEntity> entities = findAllUseLatestBySkillId(skillId);
        LocalDateTime now = LocalDateTime.now();
        for (WorkspaceSkillEntity entity : entities) {
            entity.setPinnedVersion(newVersion);
            entity.setUpdatedAt(now);
            db.update(entity);
        }
    }

    /**
     * 检查 workspace 是否已圈选某个 skill。
     */
    public boolean existsByWorkspaceIdAndSkillId(Long workspaceId, Long skillId) {
        return db.find(WorkspaceSkillEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .eq("skill.id", skillId)
                .exists();
    }

    /**
     * 查询圈选了指定 skill 的所有 workspaceId。
     */
    public List<Long> findWorkspaceIdsBySkillId(Long skillId) {
        return db.find(WorkspaceSkillEntity.class)
                .where()
                .eq("skill.id", skillId)
                .eq("enabled", true)
                .findList()
                .stream()
                .map(WorkspaceSkillEntity::getWorkspaceId)
                .distinct()
                .toList();
    }

    /**
     * 查询圈选了指定 skill 的所有启用关联记录（含 useLatest=false 的）。
     * 用于 skill 重新激活时，通知所有相关 workspace 重新加载。
     */
    public List<WorkspaceSkillEntity> findAllEnabledBySkillId(Long skillId) {
        return db.find(WorkspaceSkillEntity.class)
                .where()
                .eq("skill.id", skillId)
                .eq("enabled", true)
                .findList();
    }
}