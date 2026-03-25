package org.dragon.skill.store;

import org.dragon.skill.entity.SkillEntity;
import org.dragon.skill.enums.SkillCategory;
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
     * 检查名称是否已存在（排除软删除）。
     */
    boolean existsByName(String name);

    /**
     * 检查名称是否已存在（排除软删除和指定 ID）。
     */
    boolean existsByNameExcludeId(String name, Long excludeId);

    /**
     * 软删除。
     */
    void softDelete(Long id);

    /**
     * 查询所有启用的 Skill（全量加载，用于系统启动）。
     */
    List<SkillEntity> findAllEnabled();

    /**
     * 查询指定工作空间下所有启用的 Skill。
     * 同时包含内置 Skill（workspaceId=0）。
     *
     * @param workspaceId 工作空间 ID
     */
    List<SkillEntity> findAllEnabledByWorkspace(Long workspaceId);

    /**
     * 查询所有内置 Skill（workspaceId=0，启用状态）。
     */
    List<SkillEntity> findAllBuiltin();

    /**
     * 按启用状态查询。
     */
    List<SkillEntity> findByEnabled(Boolean enabled);

    /**
     * 按工作空间 ID 查询。
     */
    List<SkillEntity> findByWorkspaceId(Long workspaceId);
}
