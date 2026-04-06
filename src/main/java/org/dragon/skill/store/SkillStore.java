package org.dragon.skill.store;

import org.dragon.skill.domain.SkillDO;

import java.util.List;

/**
 * SkillStore — skills 表的存储抽象接口。
 *
 * <p>设计说明：
 * <ul>
 *   <li>skill 表存储技能元信息，版本内容在 skill_versions 表</li>
 *   <li>publishedVersionId 指向当前已发布的版本</li>
 *   <li>状态流转：draft → active → disabled（软删除用 deleted_at）</li>
 * </ul>
 */
public interface SkillStore {

    // ── 写操作 ────────────────────────────────────────────────────────

    /**
     * 保存一个新的技能记录。
     */
    void save(SkillDO skill);

    /**
     * 更新技能元信息。
     */
    void update(SkillDO skill);

    // ── 读操作 ────────────────────────────────────────────────────────

    /**
     * 按物理主键查询。
     */
    java.util.Optional<SkillDO> findById(String id);

    /**
     * 按 skillId 查询技能。
     */
    java.util.Optional<SkillDO> findBySkillId(String skillId);

    /**
     * 查询已发布的技能（status=active 且 publishedVersionId 不为空）。
     * 用于 Agent 运行时加载。
     */
    java.util.Optional<SkillDO> findLatestActiveBySkillId(String skillId);

    /**
     * 查询所有已发布的 builtin 技能。
     */
    List<SkillDO> findAllBuiltin();

    /**
     * 判断 skillId 是否存在。
     */
    boolean existsBySkillId(String skillId);

    // ── 管理页查询 ────────────────────────────────────────────────────

    /**
     * 多条件分页检索（deleted_at IS NULL 的记录）。
     */
    List<SkillDO> search(String keyword, String status, String category,
                         String visibility, Long creatorId,
                         String sortBy, String sortOrder,
                         int offset, int limit);

    /**
     * 统计检索记录数。
     */
    int countSearch(String keyword, String status, String category,
                    String visibility, Long creatorId);
}
