package org.dragon.skill.registry;

import org.dragon.skill.model.SkillEntry;

import java.util.Collection;
import java.util.Optional;

/**
 * 运行时 Skill 注册表接口。
 * 管理所有已加载（ACTIVE 状态）的 SkillEntry。
 *
 * @since 1.0
 */
public interface SkillRegistry {

    /**
     * 注册或更新一个 SkillEntry。
     * 若已存在同名 Skill，则覆盖更新。
     *
     * @param entry 已解析的 SkillEntry
     */
    void register(SkillEntry entry);

    /**
     * 注销指定名称的 Skill。
     *
     * @param skillName Skill 名称
     */
    void unregister(String skillName);

    /**
     * 按名称查找 SkillEntry。
     *
     * @param skillName Skill 名称
     * @return Optional<SkillEntry>
     */
    Optional<SkillEntry> findByName(String skillName);

    /**
     * 按 ID 查找 SkillEntry。
     *
     * @param skillId Skill ID
     * @return Optional<SkillEntry>
     */
    Optional<SkillEntry> findById(long skillId);

    /**
     * 获取所有已注册的 SkillEntry。
     *
     * @return 不可变集合
     */
    Collection<SkillEntry> findAll();

    /**
     * 获取所有已注册 Skill 的系统提示词片段。
     *
     * @return 拼接后的提示词字符串
     */
    String buildSystemPromptFragment();

    /**
     * 清空注册表。
     */
    void clear();

    /**
     * 获取当前注册的 Skill 数量。
     */
    int size();
}
