package org.dragon.skill.service;

import org.dragon.skill.actionlog.PublishDetail;
import org.dragon.skill.actionlog.SkillActionLogService;
import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.domain.SkillVersionDO;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.SkillVersionStatus;
import org.dragon.skill.exception.SkillNotFoundException;
import org.dragon.skill.exception.SkillStatusException;
import org.dragon.skill.exception.SkillVersionStatusException;
import org.dragon.skill.store.SkillStore;
import org.dragon.skill.store.SkillVersionStore;
import org.dragon.util.bean.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Skill 生命周期（状态管理）服务。
 *
 * <p>状态流转规则：
 * <pre>
 *   draft → active → disabled
 *            ↑__________| (republish)
 *
 *   任意状态 → deleted（软删除）
 * </pre>
 *
 * <p>版本发布时：
 * <ul>
 *   <li>旧 published 版本标记为 DEPRECATED</li>
 *   <li>新版本标记为 PUBLISHED</li>
 *   <li>skill.publishedVersionId 指向新版本</li>
 *   <li>skill.status = ACTIVE</li>
 * </ul>
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class SkillLifecycleService {

    @Autowired
    private SkillStore skillStore;
    @Autowired
    private SkillVersionStore skillVersionStore;
    @Autowired
    private SkillActionLogService actionLogService;

    // ── 发布 ─────────────────────────────────────────────────────────

    /**
     * 发布技能。
     *
     * <p>将 draft 版本发布：version.status → PUBLISHED，旧版本 → DEPRECATED，
     * skill.status → ACTIVE，skill.publishedVersionId → 新版本。
     *
     * @param skillId     技能 UUID
     * @param version     要发布的版本号
     * @param releaseNote 发版备注
     * @param user        操作者用户信息
     */
    public void publish(String skillId, int version, String releaseNote, UserInfo user) {
        SkillDO skill = requireNotDeleted(skillId);

        // 获取要发布的版本
        SkillVersionDO targetVersion = skillVersionStore.findBySkillIdAndVersion(skillId, version)
                .orElseThrow(() -> new SkillNotFoundException(skillId + " v" + version));
        assertVersionStatus(targetVersion, SkillVersionStatus.DRAFT, "发布");

        // 旧已发布版本标记为 DEPRECATED
        Long oldPublishedId = skill.getPublishedVersionId();
        if (oldPublishedId != null) {
            SkillVersionDO oldVersion = skillVersionStore.findById(oldPublishedId).orElse(null);
            if (oldVersion != null && oldVersion.getStatus() == SkillVersionStatus.PUBLISHED) {
                oldVersion.setStatus(SkillVersionStatus.DEPRECATED);
                skillVersionStore.update(oldVersion);
            }
        }

        // 新版本标记为 PUBLISHED
        targetVersion.setStatus(SkillVersionStatus.PUBLISHED);
        targetVersion.setPublishedAt(LocalDateTime.now());
        targetVersion.setReleaseNote(releaseNote);
        skillVersionStore.update(targetVersion);

        // 更新技能元信息
        skill.setPublishedVersionId(targetVersion.getId());
        skill.setStatus(SkillStatus.ACTIVE);
        skillStore.update(skill);

        actionLogService.log(skillId, skill.getName(),
                org.dragon.skill.enums.SkillActionType.PUBLISH,
                parseUserId(user.getUserId()), user.getUsername(), version,
                new PublishDetail(releaseNote));
    }

    // ── 下架 ─────────────────────────────────────────────────────────

    /**
     * 下架技能：skill.status → DISABLED。
     * Agent 端不再加载此技能。
     */
    public void disable(String skillId, UserInfo user) {
        SkillDO skill = requireNotDeleted(skillId);
        assertSkillStatus(skill, SkillStatus.ACTIVE, "下架");

        skill.setStatus(SkillStatus.DISABLED);
        skillStore.update(skill);

        actionLogService.log(skillId, skill.getName(),
                org.dragon.skill.enums.SkillActionType.DISABLE,
                parseUserId(user.getUserId()), user.getUsername(), null);
    }

    // ── 重新发布 ──────────────────────────────────────────────────────

    /**
     * 重新发布：skill.status → ACTIVE（继续使用已发布的版本）。
     */
    public void republish(String skillId, UserInfo user) {
        SkillDO skill = requireNotDeleted(skillId);
        assertSkillStatus(skill, SkillStatus.DISABLED, "重新发布");

        skill.setStatus(SkillStatus.ACTIVE);
        skillStore.update(skill);

        Integer version = skill.getPublishedVersionId() != null
                ? skillVersionStore.findById(skill.getPublishedVersionId()).map(SkillVersionDO::getVersion).orElse(null)
                : null;

        actionLogService.log(skillId, skill.getName(),
                org.dragon.skill.enums.SkillActionType.REPUBLISH,
                parseUserId(user.getUserId()), user.getUsername(), version);
    }

    // ── 软删除 ────────────────────────────────────────────────────────

    /**
     * 软删除技能：skill.deletedAt = now。
     */
    public void delete(String skillId, UserInfo user) {
        SkillDO skill = requireNotDeleted(skillId);

        if (skill.getDeletedAt() != null) {
            return; // 幂等
        }

        skill.setDeletedAt(LocalDateTime.now());
        skillStore.update(skill);

        actionLogService.log(skillId, skill.getName(),
                org.dragon.skill.enums.SkillActionType.DELETE,
                parseUserId(user.getUserId()), user.getUsername(), null);
    }

    // ── 私有辅助方法 ─────────────────────────────────────────────────

    private SkillDO requireNotDeleted(String skillId) {
        SkillDO skill = skillStore.findBySkillId(skillId)
                .orElseThrow(() -> new SkillNotFoundException(skillId));
        if (skill.getDeletedAt() != null) {
            throw new SkillNotFoundException(skillId);
        }
        return skill;
    }

    private void assertSkillStatus(SkillDO skill, SkillStatus required, String operation) {
        if (required != skill.getStatus()) {
            throw new SkillStatusException(skill.getStatus(), required, operation);
        }
    }

    private void assertVersionStatus(SkillVersionDO version, SkillVersionStatus required, String operation) {
        if (required != version.getStatus()) {
            throw new SkillVersionStatusException(version.getStatus(), required, operation);
        }
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.isBlank()) return null;
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}