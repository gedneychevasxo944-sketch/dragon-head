package org.dragon.skill.service;

import org.dragon.skill.domain.SkillUsageDO;
import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.runtime.AgentContext;
import org.dragon.skill.runtime.SkillDefinition;
import org.dragon.skill.store.SkillUsageStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SkillUsageService — Skill 使用频率追踪与排序服务（对应 TS {@code recordSkillUsage}）。
 *
 * <h3>核心职责</h3>
 * <ol>
 *   <li><b>异步写入</b>：每次 Skill 执行后，异步将调用记录写入 {@code skill_usage_logs} 表，
 *       不阻塞主流程</li>
 *   <li><b>排序查询</b>：提供全局 / 按 Character / 按 Workspace 三个维度的使用频率排序接口，
 *       供 Skill 列表页排序和推荐功能使用</li>
 * </ol>
 *
 * <h3>排序结果 {@link SkillRankItem}</h3>
 * <p>包含 skillId / skillName / totalCount / successCount / avgDurationMs，
 * 调用方（如 Controller）可进一步结合 {@link org.dragon.skill.store.SkillStore}
 * 查询 Skill 元信息后组装完整的展示 DTO。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillUsageService {

    private final SkillUsageStore usageStore;

    // ── 写入 ──────────────────────────────────────────────────────────

    /**
     * 异步记录 Skill 调用（成功场景）。
     *
     * <p>在 {@link org.dragon.skill.runtime.SkillTool#execute} 执行成功后调用，
     * 通过 {@code @Async} 不阻塞主流程。
     *
     * @param skill        执行的 Skill 定义
     * @param agentContext 当前 Agent 上下文
     * @param sessionKey   会话 Key
     * @param args         调用参数（可为 null）
     * @param durationMs   执行耗时（毫秒）
     */
    @Async
    public void recordSuccess(SkillDefinition skill,
                              AgentContext agentContext,
                              String sessionKey,
                              String args,
                              long durationMs) {
        try {
            SkillUsageDO usage = SkillUsageDO.builder()
                    .skillId(skill.getSkillId())
                    .skillName(skill.getName())
                    .skillVersion(skill.getVersion())
                    .characterId(agentContext.getCharacterId())
                    .workspaceId(agentContext.getWorkspaceId())
                    .agentId(agentContext.getAgentId())
                    .sessionKey(sessionKey)
                    .executionContext(skill.getExecutionContext() != null
                            ? skill.getExecutionContext() : ExecutionContext.INLINE)
                    .args(args)
                    .success(true)
                    .errorMessage(null)
                    .durationMs(durationMs)
                    .invokedAt(LocalDateTime.now())
                    .build();
            usageStore.save(usage);
        } catch (Exception e) {
            // 使用记录写入失败不影响 Skill 执行结果，只记录日志
            log.warn("[SkillUsage] 使用记录写入失败: skill={}, error={}",
                    skill.getName(), e.getMessage());
        }
    }

    /**
     * 异步记录 Skill 调用（失败场景）。
     *
     * <p>在 {@link org.dragon.skill.runtime.SkillTool#execute} 抛出异常时调用。
     *
     * @param skill        执行的 Skill 定义
     * @param agentContext 当前 Agent 上下文
     * @param sessionKey   会话 Key
     * @param args         调用参数（可为 null）
     * @param errorMessage 错误信息
     */
    @Async
    public void recordFailure(SkillDefinition skill,
                              AgentContext agentContext,
                              String sessionKey,
                              String args,
                              String errorMessage) {
        try {
            SkillUsageDO usage = SkillUsageDO.builder()
                    .skillId(skill.getSkillId())
                    .skillName(skill.getName())
                    .skillVersion(skill.getVersion())
                    .characterId(agentContext.getCharacterId())
                    .workspaceId(agentContext.getWorkspaceId())
                    .agentId(agentContext.getAgentId())
                    .sessionKey(sessionKey)
                    .executionContext(skill.getExecutionContext() != null
                            ? skill.getExecutionContext() : ExecutionContext.INLINE)
                    .args(args)
                    .success(false)
                    .errorMessage(errorMessage)
                    .durationMs(null)   // 失败时不统计耗时
                    .invokedAt(LocalDateTime.now())
                    .build();
            usageStore.save(usage);
        } catch (Exception e) {
            log.warn("[SkillUsage] 失败记录写入失败: skill={}, error={}",
                    skill.getName(), e.getMessage());
        }
    }

    // ── 排序查询 ──────────────────────────────────────────────────────

    /**
     * 全局使用频率排行（按调用总次数降序）。
     *
     * <p>适合 Skill 管理后台展示"全平台最热门技能"。
     *
     * @param since 统计起始时间，null 表示统计全量历史
     * @param limit 返回条数上限
     * @return 排序后的统计列表
     */
    public List<SkillRankItem> rankGlobal(LocalDateTime since, int limit) {
        return usageStore.rankByUsageCount(since, limit);
    }

    /**
     * 指定 Character 的个人化使用频率排行。
     *
     * <p>适合在 Skill 选择界面为当前用户推荐高频技能。
     *
     * @param characterId 执行者 Character ID
     * @param since       统计起始时间，null 表示统计全量历史
     * @param limit       返回条数上限
     * @return 排序后的统计列表
     */
    public List<SkillRankItem> rankByCharacter(String characterId, LocalDateTime since, int limit) {
        return usageStore.rankByUsageCountForCharacter(characterId, since, limit);
    }

    /**
     * 指定 Workspace 的使用频率排行。
     *
     * <p>适合在 Workspace 内展示"团队最常用技能"。
     *
     * @param workspaceId Workspace ID
     * @param since       统计起始时间，null 表示统计全量历史
     * @param limit       返回条数上限
     * @return 排序后的统计列表
     */
    public List<SkillRankItem> rankByWorkspace(String workspaceId, LocalDateTime since, int limit) {
        return usageStore.rankByUsageCountForWorkspace(workspaceId, since, limit);
    }

    /**
     * 查询指定 Skill 的最近调用记录（用于调用历史 / 故障排查）。
     *
     * @param skillId skillId（业务 UUID）
     * @param limit   返回条数上限
     * @return 调用记录列表，按调用时间降序
     */
    public List<SkillUsageDO> recentCallsBySkill(String skillId, int limit) {
        return usageStore.findRecentBySkillId(skillId, limit);
    }

    /**
     * 查询指定 Character 的最近调用记录。
     *
     * @param characterId Character ID
     * @param limit       返回条数上限
     * @return 调用记录列表，按调用时间降序
     */
    public List<SkillUsageDO> recentCallsByCharacter(String characterId, int limit) {
        return usageStore.findRecentByCharacterId(characterId, limit);
    }

    // ── 内部 DTO ──────────────────────────────────────────────────────

    /**
     * Skill 使用频率排行项（聚合结果）。
     *
     * <p>由 {@link SkillUsageStore} 的聚合 SQL 生成，直接返回给上层调用方。
     * 调用方如需完整 Skill 元信息（displayName / description 等），
     * 可用 {@code skillId} 再查 {@code SkillStore}。
     *
     * @param skillId       技能业务 UUID
     * @param skillName     技能调用名称
     * @param totalCount    统计时间窗口内的总调用次数
     * @param successCount  成功调用次数
     * @param avgDurationMs 成功调用的平均耗时（毫秒），null 表示无统计数据
     */
    public record SkillRankItem(
            String skillId,
            String skillName,
            long totalCount,
            long successCount,
            Long avgDurationMs
    ) {
        /** 成功率（0.0 ~ 1.0），totalCount 为 0 时返回 0.0 */
        public double successRate() {
            return totalCount == 0 ? 0.0 : (double) successCount / totalCount;
        }
    }
}

