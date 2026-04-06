package org.dragon.skill.store;

import org.dragon.skill.domain.SkillBindingDO;

import java.util.List;
import java.util.Optional;

/**
 * SkillBindingStore — skill_bindings 表的存储抽象接口。
 *
 * <p>三种绑定类型说明：
 * <ul>
 *   <li>{@code character}           — Character 自有 skill（characterId 非 NULL，workspaceId NULL）</li>
 *   <li>{@code workspace}           — Workspace 公共 skill（workspaceId 非 NULL，characterId NULL）</li>
 *   <li>{@code character_workspace} — Character 在特定 Workspace 下的专属 skill（两者均非 NULL）</li>
 * </ul>
 */
public interface SkillBindingStore {

    // ── 写操作 ────────────────────────────────────────────────────────

    /**
     * 保存一条绑定记录（INSERT）。
     *
     * @param binding 绑定领域对象（id 为 null，由数据库自增填充）
     */
    void save(SkillBindingDO binding);

    /**
     * 更新绑定记录（UPDATE by id），通常用于修改版本策略（versionType / fixedVersion）。
     *
     * @param binding 绑定领域对象（id 不可为 null）
     */
    void update(SkillBindingDO binding);

    /**
     * 按物理主键删除绑定记录（解绑）。
     *
     * @param id 绑定记录物理主键
     */
    void delete(Long id);

    // ── 读操作 ────────────────────────────────────────────────────────

    /**
     * 按物理主键查询绑定记录。
     *
     * @param id 物理主键
     * @return Optional 包装的绑定领域对象
     */
    Optional<SkillBindingDO> findById(Long id);

    /**
     * 查询某 Character 的全部自有 skill 绑定（bindingType = 'character'）。
     *
     * @param characterId Character 主键
     * @return 绑定列表（按 createdAt 降序）
     */
    List<SkillBindingDO> findByCharacterId(String characterId);

    /**
     * 查询某 Workspace 的全部公共 skill 绑定（bindingType = 'workspace'）。
     *
     * @param workspaceId Workspace 主键
     * @return 绑定列表（按 createdAt 降序）
     */
    List<SkillBindingDO> findByWorkspaceId(String workspaceId);

    /**
     * 查询某 Character 在某 Workspace 下的专属 skill 绑定
     * （bindingType = 'character_workspace'）。
     *
     * @param characterId Character 主键
     * @param workspaceId Workspace 主键
     * @return 绑定列表（按 createdAt 降序）
     */
    List<SkillBindingDO> findByCharacterIdAndWorkspaceId(String characterId, String workspaceId);

    /**
     * 查询某 Character 在指定 Workspace 下可用的所有 skill 绑定（并集）：
     * <ol>
     *   <li>Character 自有 skill（bindingType = 'character'，characterId = ?）</li>
     *   <li>Workspace 公共 skill（bindingType = 'workspace'，workspaceId = ?）</li>
     *   <li>Character 在该 Workspace 下的专属 skill（bindingType = 'character_workspace'，两者均匹配）</li>
     * </ol>
     *
     * @param characterId Character 主键
     * @param workspaceId Workspace 主键
     * @return 并集绑定列表（skillId 可能重复，调用方需去重）
     */
    List<SkillBindingDO> findAvailableByCharacterAndWorkspace(String characterId, String workspaceId);

    /**
     * 判断指定绑定是否已存在（防重复绑定）。
     *
     * @param bindingType 绑定类型
     * @param characterId Character 主键（可为 null）
     * @param workspaceId Workspace 主键（可为 null）
     * @param skillId     技能业务 UUID
     * @return true 表示已存在
     */
    boolean exists(String bindingType, String characterId, String workspaceId, String skillId);
}

