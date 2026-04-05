package org.dragon.skill.service;

import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.exception.SkillNotFoundException;
import org.dragon.skill.exception.SkillStatusException;
import org.dragon.skill.store.SkillStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Skill 生命周期（状态管理）服务。
 *
 * <p>状态流转规则：
 * <pre>
 *          注册/更新
 *             │
 *             ▼
 *          [draft]
 *             │  发布(publish)
 *             ▼
 *          [active] ◄────── 重新发布(republish)
 *             │                    ▲
 *          下架(disable)           │
 *             │                    │
 *             ▼                    │
 *         [disabled] ──────────────┘
 *             │
 *          删除(delete)
 *             ▼
 *          [deleted]  ← 终态，不可逆
 *
 * 注：draft/active/disabled 均可执行 delete
 * </pre>
 *
 * <p>状态变更作用范围：
 * <ul>
 *   <li>publish / disable / republish：只变更最新版本记录的 status</li>
 *   <li>delete：将该 skillId 的所有版本记录 status 均改为 deleted（软删除）</li>
 * </ul>
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class SkillLifecycleService {

    @Autowired
    private SkillStore skillStore;

    // ── 发布 ─────────────────────────────────────────────────────────

    /**
     * 发布技能：draft → active。
     * 仅对最新版本生效，同时记录 publishedAt。
     *
     * @param skillId    技能业务 UUID
     * @param operatorId 操作者 ID（用于审计日志，可扩展）
     */
    public void publish(String skillId, Long operatorId) {
        SkillDO latest = requireLatest(skillId);
        assertStatus(latest, SkillStatus.DRAFT, "发布");

        latest.setStatus(SkillStatus.ACTIVE);
        latest.setPublishedAt(LocalDateTime.now());
        skillStore.update(latest);
    }

    // ── 下架 ─────────────────────────────────────────────────────────

    /**
     * 下架技能：active → disabled。
     * Agent 端不再加载 disabled 状态的技能。
     */
    public void disable(String skillId, Long operatorId) {
        SkillDO latest = requireLatest(skillId);
        assertStatus(latest, SkillStatus.ACTIVE, "下架");

        latest.setStatus(SkillStatus.DISABLED);
        skillStore.update(latest);
    }

    // ── 重新发布 ──────────────────────────────────────────────────────

    /**
     * 重新发布：disabled → active。
     */
    public void republish(String skillId, Long operatorId) {
        SkillDO latest = requireLatest(skillId);
        assertStatus(latest, SkillStatus.DISABLED, "重新发布");

        latest.setStatus(SkillStatus.ACTIVE);
        latest.setPublishedAt(LocalDateTime.now());
        skillStore.update(latest);
    }

    // ── 软删除 ────────────────────────────────────────────────────────

    /**
     * 软删除技能：将该 skillId 所有版本记录均标记为 deleted。
     * draft / active / disabled 均可删除；deleted 状态再次删除视为幂等操作。
     */
    public void delete(String skillId, Long operatorId) {
        SkillDO latest = requireLatest(skillId);
        if (SkillStatus.DELETED == latest.getStatus()) {
            // 幂等：已删除则直接返回
            return;
        }
        // 将所有版本标记为 DELETED（逐条 update）
        List<SkillDO> allVersions = skillStore.findAllVersionsBySkillId(skillId);
        for (SkillDO version : allVersions) {
            if (SkillStatus.DELETED != version.getStatus()) {
                version.setStatus(SkillStatus.DELETED);
                skillStore.update(version);
            }
        }
    }

    // ── 私有辅助方法 ─────────────────────────────────────────────────

    /**
     * 查询最新版本，若不存在则抛出 SkillNotFoundException。
     */
    private SkillDO requireLatest(String skillId) {
        return skillStore.findLatestBySkillId(skillId)
                .orElseThrow(() -> new SkillNotFoundException(skillId));
    }

    /**
     * 断言当前状态符合操作前提，否则抛出 SkillStatusException。
     */
    private void assertStatus(SkillDO skill, SkillStatus requiredStatus, String operation) {
        if (requiredStatus != skill.getStatus()) {
            throw new SkillStatusException(skill.getStatus(), requiredStatus, operation);
        }
    }
}

