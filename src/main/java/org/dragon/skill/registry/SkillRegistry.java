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

    /**
     * 按工作空间 ID 获取所有 ACTIVE 状态的运行时条目。
     * workspaceId=0L 时返回所有内置 Skill。
     *
     * @param workspaceId 工作空间 ID
     * @return 该工作空间下所有可用的 SkillRuntimeEntry
     */
    Collection<SkillRuntimeEntry> findAllActiveByWorkspace(long workspaceId);

    /**
     * 获取指定工作空间的 System Prompt Fragment。
     * 包含：该工作空间专属 Skill + 所有内置 Skill（workspaceId=0）。
     *
     * @param workspaceId 工作空间 ID
     */
    String buildSystemPromptFragment(long workspaceId);

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
