package org.dragon.skill.store;

import org.dragon.skill.domain.SkillDO;

import java.util.List;
import java.util.Optional;

/**
 * SkillStore — skills 表的存储抽象接口。
 *
 * <p>版本设计约定：
 * <ul>
 *   <li>每次更新技能时 INSERT 一条新记录，{@code skillId} 不变，{@code version} +1。</li>
 *   <li>"最新版本"指该 {@code skillId} 下 {@code version} 最大的那条记录。</li>
 *   <li>状态查询默认仅面向 {@code active} 状态，具体由实现方决定是否过滤。</li>
 * </ul>
 */
public interface SkillStore {

    // ── 写操作 ────────────────────────────────────────────────────────

    /**
     * 保存一条新的技能版本记录（INSERT）。
     *
     * @param skill 技能领域对象（id 为 null，由数据库自增填充）
     */
    void save(SkillDO skill);

    /**
     * 更新已有技能版本记录（UPDATE by id）。
     * 通常仅用于修改 status、publishedAt 等状态字段。
     *
     * @param skill 技能领域对象（id 不可为 null）
     */
    void update(SkillDO skill);

    // ── 读操作 ────────────────────────────────────────────────────────

    /**
     * 按物理主键查询（跨版本通用）。
     *
     * @param id 物理自增主键
     * @return Optional 包装的领域对象
     */
    Optional<SkillDO> findById(Long id);

    /**
     * 查询指定 skillId 的最新版本（version 最大）。
     *
     * @param skillId 技能业务 UUID
     * @return Optional 包装的最新版本领域对象
     */
    Optional<SkillDO> findLatestBySkillId(String skillId);

    /**
     * 查询指定 skillId + version 的具体版本。
     *
     * @param skillId 技能业务 UUID
     * @param version 版本号
     * @return Optional 包装的领域对象
     */
    Optional<SkillDO> findBySkillIdAndVersion(String skillId, int version);

    /**
     * 查询指定 skillId 的最新 active 版本（用于运行时 latest 绑定策略）。
     *
     * @param skillId 技能业务 UUID
     * @return Optional 包装的最新 active 版本领域对象
     */
    Optional<SkillDO> findLatestActiveBySkillId(String skillId);

    /**
     * 查询指定 skillId 的所有版本记录，按 version 升序排列。
     *
     * @param skillId 技能业务 UUID
     * @return 版本列表（可能为空）
     */
    List<SkillDO> findAllVersionsBySkillId(String skillId);

    /**
     * 分页查询所有技能的最新版本（每个 skillId 只取 version 最大的一条）。
     *
     * @param offset 偏移量（从 0 开始）
     * @param limit  每页数量
     * @return 最新版本列表
     */
    List<SkillDO> findLatestVersions(int offset, int limit);

    /**
     * 按状态分页查询最新版本列表。
     *
     * @param status 技能状态（draft / active / disabled / deleted）
     * @param offset 偏移量
     * @param limit  每页数量
     * @return 符合状态的最新版本列表
     */
    List<SkillDO> findLatestVersionsByStatus(String status, int offset, int limit);

    /**
     * 按 skillId 集合批量查询各 skillId 的最新 active 版本。
     * 用于运行时并集查询（SkillBindingService.listAvailableSkills）。
     *
     * @param skillIds skillId 集合
     * @return 最新 active 版本列表
     */
    List<SkillDO> findLatestActiveBySkillIds(List<String> skillIds);

    /**
     * 查询指定 skillId 下当前最大的版本号。
     * 用于生成下一个版本号（maxVersion + 1）。
     *
     * @param skillId 技能业务 UUID
     * @return 当前最大版本号，若不存在则返回 0
     */
    int findMaxVersionBySkillId(String skillId);

    /**
     * 统计所有技能（按 skillId 去重）的总数量。
     *
     * @return 技能总数
     */
    int countDistinctSkills();

    /**
     * 判断指定 skillId 是否存在（任意版本）。
     *
     * @param skillId 技能业务 UUID
     * @return true 表示存在
     */
    boolean existsBySkillId(String skillId);

    /**
     * 查询所有 category='builtin' 的最新 active 版本列表。
     *
     * <p>内置 Skill 不走绑定关系，运行时对所有 Character 全量可见；
     * 每次启动或缓存失效时由 SkillRegistry 直接调用此方法加载。
     *
     * @return 所有 builtin 最新 active 版本的 SkillDO 列表
     */
    List<SkillDO> findAllBuiltin();

    // ── 管理页查询 ────────────────────────────────────────────────────

    /**
     * 多条件分页检索（每个 skillId 只返回最新版本，deleted 状态始终排除）。
     *
     * <p>支持的过滤维度：keyword / status / category / visibility / creatorId。
     * keyword 模糊匹配 name、display_name、description 三个字段（LIKE '%keyword%'）。
     *
     * @param keyword    关键字，null/空则不过滤
     * @param status     状态过滤，null 则不过滤（deleted 始终排除）
     * @param category   分类过滤，null 则不过滤
     * @param visibility 可见性过滤，null 则不过滤
     * @param creatorId  创建者 ID，null 则不过滤
     * @param sortBy     排序字段：name / created_at / published_at（默认 created_at）
     * @param sortOrder  排序方向：asc / desc
     * @param offset     偏移量
     * @param limit      每页数量
     * @return 符合条件的最新版本列表
     */
    List<SkillDO> search(String keyword, String status, String category,
                         String visibility, Long creatorId,
                         String sortBy, String sortOrder,
                         int offset, int limit);

    /**
     * 统计多条件检索的总记录数（去重 skillId，deleted 始终排除）。
     *
     * <p>参数含义与 {@link #search} 相同，用于分页时计算总页数。
     */
    int countSearch(String keyword, String status, String category,
                    String visibility, Long creatorId);
}

