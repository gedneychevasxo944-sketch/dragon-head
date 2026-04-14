package org.dragon.skill.store;

import org.dragon.skill.domain.SkillUsageDO;
import org.dragon.skill.service.SkillUsageService.SkillRankItem;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SkillUsageStore — skill_usage_log 表的存储抽象接口。
 */
public interface SkillUsageStore {

    // ── 写操作 ────────────────────────────────────────────────────────

    /**
     * 写入一条 Skill 调用记录（INSERT）。
     *
     * @param usage 调用记录领域对象（id 为 null，由数据库自增填充）
     */
    void save(SkillUsageDO usage);

    // ── 读操作 ────────────────────────────────────────────────────────

    /**
     * 查询指定时间窗口内各 Skill 的调用统计，按调用次数降序排列。
     *
     * <p>用于 Skill 推荐排序：高频 Skill 排前面。
     *
     * @param since  统计起始时间（含），null 表示不限
     * @param limit  返回条数上限
     * @return 排序后的统计列表，每项包含 skillId / skillName / totalCount / successCount / avgDurationMs
     */
    List<SkillRankItem> rankByUsageCount(LocalDateTime since, int limit);

    /**
     * 查询指定 Character 在时间窗口内各 Skill 的调用统计，按调用次数降序。
     *
     * <p>用于个人化推荐：针对特定 Character 的高频 Skill 排前。
     *
     * @param characterId 执行者 Character ID
     * @param since       统计起始时间（含），null 表示不限
     * @param limit       返回条数上限
     * @return 排序后的统计列表
     */
    List<SkillRankItem> rankByUsageCountForCharacter(String characterId, LocalDateTime since, int limit);

    /**
     * 查询指定 Workspace 在时间窗口内各 Skill 的调用统计，按调用次数降序。
     *
     * @param workspaceId Workspace ID
     * @param since       统计起始时间（含），null 表示不限
     * @param limit       返回条数上限
     * @return 排序后的统计列表
     */
    List<SkillRankItem> rankByUsageCountForWorkspace(String workspaceId, LocalDateTime since, int limit);

    /**
     * 查询指定 Skill 的最近调用记录，按调用时间降序。
     *
     * <p>用于故障排查和调用历史查看。
     *
     * @param skillId skillId（业务 UUID）
     * @param limit   返回条数上限
     * @return 调用记录列表
     */
    List<SkillUsageDO> findRecentBySkillId(String skillId, int limit);

    /**
     * 查询指定 Character 的最近调用记录，按调用时间降序。
     *
     * @param characterId 执行者 Character ID
     * @param limit       返回条数上限
     * @return 调用记录列表
     */
    List<SkillUsageDO> findRecentByCharacterId(String characterId, int limit);

    /**
     * 统计指定 skillId 在时间窗口内的调用总次数。
     *
     * @param skillId skillId（业务 UUID）
     * @param since   统计起始时间（含），null 表示不限
     * @return 调用总次数
     */
    long countBySkillId(String skillId, LocalDateTime since);
}

