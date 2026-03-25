package org.dragon.skill.service;

import org.dragon.skill.entity.SkillEntity;
import org.dragon.skill.registry.SkillRuntimeEntry;

import java.util.List;
import java.util.Optional;

/**
 * Skill 运行时加载服务接口。
 * 负责从文件系统读取 SKILL.md，解析并注册到运行时注册表。
 *
 * @since 1.0
 */
public interface SkillLoaderService {

    /**
     * 系统启动时全量加载所有【启用状态】的 Skill。
     */
    void loadAll();

    /**
     * 按工作空间 ID 加载该 workspace 下所有启用的 Skill。
     * 同时自动加载所有内置 Skill（workspaceId=0）。
     *
     * @param workspaceId 工作空间 ID
     */
    void loadByWorkspace(Long workspaceId);

    /**
     * 加载单个 Skill。
     *
     * @param entity 数据库中的 Skill 实体
     * @return 解析成功的 SkillRuntimeEntry，失败时返回 empty
     */
    Optional<SkillRuntimeEntry> loadSkill(SkillEntity entity);

    /**
     * 按 Skill ID 重新加载。
     *
     * @param skillId Skill ID
     */
    void reloadSkill(Long skillId);

    /**
     * 注销单个 Skill。
     *
     * @param skillId   Skill ID
     * @param skillName Skill 名称
     */
    void unloadSkill(Long skillId, String skillName);

    /**
     * 检查 Skill 的依赖是否满足。
     *
     * @param entry 已解析的 SkillEntry
     * @return 依赖检查结果，通过返回 empty，失败返回错误描述
     */
    Optional<String> checkRequires(org.dragon.skill.model.SkillEntry entry);
}
