package org.dragon.skill.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dragon.actionlog.ActionType;
import org.dragon.asset.enums.AssociationType;
import org.dragon.asset.service.AssetAssociationService;
import org.dragon.permission.enums.ResourceType;
import org.dragon.skill.domain.SkillRuntimeConfig;
import java.util.Map;
import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.domain.SkillVersionDO;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.SkillVersionStatus;
import org.dragon.skill.exception.SkillNotFoundException;
import org.dragon.skill.exception.SkillStatusException;
import org.dragon.skill.exception.SkillVersionStatusException;
import org.dragon.skill.store.SkillStore;
import org.dragon.skill.store.SkillVersionStore;
import org.dragon.tool.store.ToolStore;
import org.dragon.util.bean.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(SkillLifecycleService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private SkillStore skillStore;
    @Autowired
    private SkillVersionStore skillVersionStore;
    @Autowired
    private SkillActionLogService actionLogService;
    @Autowired
    private AssetAssociationService assetAssociationService;
    @Autowired
    private ToolStore toolStore;

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

        // 同步 TOOL_SKILL 关联：解析 allowedTools，将 tool 名称解析为 ID 并创建关联
        syncToolSkillAssociations(skillId, targetVersion);

        actionLogService.log(skillId, skill.getName(), skill.getDisplayName(),
                ActionType.SKILL_PUBLISH, user.getUsername(), version,
                releaseNote != null ? Map.of("releaseNote", releaseNote) : null);
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

        actionLogService.log(skillId, skill.getName(), skill.getDisplayName(),
                ActionType.SKILL_DISABLE, user.getUsername(), null);
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

        actionLogService.log(skillId, skill.getName(), skill.getDisplayName(),
                ActionType.SKILL_REPUBLISH, user.getUsername(), version);
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

        actionLogService.log(skillId, skill.getName(), skill.getDisplayName(),
                ActionType.SKILL_DELETE, user.getUsername(), null);
    }

    // ── 私有辅助方法 ─────────────────────────────────────────────────

    /**
     * 将发布版本中 allowedTools 字段的工具名解析为工具 ID，并同步创建 TOOL_SKILL 关联。
     *
     * <p>allowedTools 是工具名称列表（如 ["bash", "grep"]），通过 ToolStore.findByName 解析为 ID。
     * 无法解析的名称（工具不存在）会跳过并记录 warn 日志。
     */
    private void syncToolSkillAssociations(String skillId, SkillVersionDO version) {
        List<String> allowedTools = parseAllowedTools(version.getRuntimeConfig());
        if (allowedTools == null || allowedTools.isEmpty()) {
            return;
        }
        for (String toolName : allowedTools) {
            toolStore.findByName(toolName).ifPresentOrElse(
                    tool -> assetAssociationService.createAssociation(
                            AssociationType.TOOL_SKILL,
                            ResourceType.TOOL, tool.getId(),
                            ResourceType.SKILL, skillId),
                    () -> log.warn("[SkillLifecycleService] allowedTools 中工具名 '{}' 未找到对应工具，跳过关联", toolName)
            );
        }
        log.info("[SkillLifecycleService] 同步 TOOL_SKILL 关联完成: skillId={}, allowedTools={}", skillId, allowedTools);
    }

    @SuppressWarnings("unchecked")
    private List<String> parseAllowedTools(String runtimeConfigJson) {
        if (runtimeConfigJson == null || runtimeConfigJson.isBlank()) return List.of();
        try {
            SkillRuntimeConfig config = OBJECT_MAPPER.readValue(runtimeConfigJson, SkillRuntimeConfig.class);
            return config.getAllowedTools() != null ? config.getAllowedTools() : List.of();
        } catch (Exception e) {
            log.warn("[SkillLifecycleService] 解析 runtimeConfig 失败，跳过 TOOL_SKILL 同步: {}", e.getMessage());
            return List.of();
        }
    }

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

}