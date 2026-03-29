package org.dragon.skill.registry;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 运行时 Skill 注册表接口。
 * 管理所有已加载（ACTIVE 状态）的 SkillRuntimeEntry。
 *
 * @since 1.0
 */
public interface SkillRegistry {

    /**
     * 注册或更新一个运行时 Skill 条目。
     *
     * @param runtimeEntry 包含解析内容和运行时状态的完整条目
     */
    void register(SkillRuntimeEntry runtimeEntry);

    /**
     * 注销指定名称的 Skill。
     *
     * @param skillName Skill 名称
     */
    void unregister(String skillName);

    /**
     * 注册 Skill 到指定 workspace。
     *
     * @param workspaceId workspace ID
     * @param runtimeEntry 运行时条目
     */
    void registerForWorkspace(long workspaceId, SkillRuntimeEntry runtimeEntry);

    /**
     * 从指定 workspace 注销某个 Skill。
     *
     * @param skillName Skill 名称
     * @param workspaceId workspace ID
     */
    void unregisterForWorkspace(String skillName, long workspaceId);

    /**
     * 注册 Skill 到指定 character。
     *
     * @param characterId character ID
     * @param workspaceId workspace ID（可以为 null）
     * @param runtimeEntry 运行时条目
     */
    void registerForCharacter(String characterId, Long workspaceId, SkillRuntimeEntry runtimeEntry);

    /**
     * 从指定 character 注销某个 Skill。
     *
     * @param characterId character ID
     * @param skillName Skill 名称
     * @param workspaceId workspace ID（可以为 null）
     */
    void unregisterForCharacter(String characterId, String skillName, Long workspaceId);

    /**
     * 按工作空间 ID 获取所有 ACTIVE 状态的运行时条目。
     * workspaceId=0L 时返回所有内置 Skill。
     *
     * @param workspaceId 工作空间 ID
     * @return 该工作空间下所有可用的 SkillRuntimeEntry
     */
    Collection<SkillRuntimeEntry> findAllActiveByWorkspace(long workspaceId);

    /**
     * 按 character ID 和 workspace ID 获取所有 ACTIVE 状态的运行时条目。
     * 返回该 character 在该 workspace 下的有效技能（含优先级合并）。
     *
     * @param characterId character ID
     * @param workspaceId 工作空间 ID
     * @return 该 character 在该工作空间下所有可用的 SkillRuntimeEntry
     */
    Collection<SkillRuntimeEntry> findAllActiveByCharacter(String characterId, Long workspaceId);

    /**
     * 查询指定 workspace 下某个 Skill 的运行时条目。
     *
     * @param workspaceId workspace ID
     * @param skillName Skill 名称
     */
    Optional<SkillRuntimeEntry> findByWorkspaceAndName(long workspaceId, String skillName);

    /**
     * 按名称查找运行时条目。
     */
    Optional<SkillRuntimeEntry> findByName(String skillName);

    /**
     * 按 ID 查找运行时条目。
     */
    Optional<SkillRuntimeEntry> findById(long skillId);

    /**
     * 获取所有已注册的运行时条目。
     */
    Collection<SkillRuntimeEntry> findAll();

    //TODO 还需要处理一下渐进式披露的逻辑（根据上下文进行skill内容检索）

    /**
     * 获取指定工作空间的 System Prompt Fragment。
     * 包含：该工作空间专属 Skill + 所有内置 Skill（workspaceId=0）。
     *
     * @param workspaceId 工作空间 ID
     */
    String buildSystemPromptFragment(String characterId, long workspaceId);

    /**
     * 获取所有 Skill 的运行时状态快照（用于监控）。
     * Key: skillName, Value: SkillRuntimeState
     */
    Map<String, SkillRuntimeState> getRuntimeStateSnapshot();

    /**
     * 清空注册表。
     */
    void clear();

    /**
     * 获取当前注册的 Skill 数量。
     */
    int size();
}
