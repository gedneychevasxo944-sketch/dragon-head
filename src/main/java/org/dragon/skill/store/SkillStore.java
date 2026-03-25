package org.dragon.skill.store;

import org.dragon.skill.entity.SkillEntity;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillLifecycleState;
import org.dragon.skill.model.SkillSource;

import java.util.List;
import java.util.Optional;

/**
 * Skill 存储接口。
 *
 * @since 1.0
 */
public interface SkillStore {

    /**
     * 保存 Skill 实体。
     */
    SkillEntity save(SkillEntity entity);

    /**
     * 更新 Skill 实体。
     */
    SkillEntity update(SkillEntity entity);

    /**
     * 删除 Skill（软删除）。
     */
    void delete(Long id);

    /**
     * 按 ID 查找（排除软删除）。
     */
    Optional<SkillEntity> findById(Long id);

    /**
     * 按名称查找（排除软删除）。
     */
    Optional<SkillEntity> findByName(String name);

    /**
     * 查询所有未删除的 Skill。
     */
    List<SkillEntity> findAll();

    /**
     * 按来源查询。
     */
    List<SkillEntity> findBySource(SkillSource source);

    /**
     * 按分类查询。
     */
    List<SkillEntity> findByCategory(SkillCategory category);

    /**
     * 按生命周期状态查询。
     */
    List<SkillEntity> findByLifecycleState(SkillLifecycleState state);

    /**
     * 检查名称是否已存在（排除软删除）。
     */
    boolean existsByName(String name);

    /**
     * 检查名称是否已存在（排除软删除和指定 ID）。
     */
    boolean existsByNameExcludeId(String name, Long excludeId);

    /**
     * 更新生命周期状态。
     */
    void updateLifecycleState(Long id, SkillLifecycleState state, String error);

    /**
     * 软删除。
     */
    void softDelete(Long id);
}
