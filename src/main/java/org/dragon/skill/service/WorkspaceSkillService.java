package org.dragon.skill.service;

import org.dragon.skill.dto.WorkspaceSkillBindRequest;
import org.dragon.skill.dto.WorkspaceSkillResponse;
import org.dragon.skill.dto.WorkspaceSkillUpdateRequest;
import org.dragon.skill.entity.SkillEntity;
import org.dragon.skill.entity.WorkspaceSkillEntity;
import org.dragon.skill.enums.SkillVisibility;
import org.dragon.skill.exception.SkillNotFoundException;
import org.dragon.skill.exception.SkillValidationException;
import org.dragon.skill.store.SkillStore;
import org.dragon.skill.store.WorkspaceSkillStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Workspace 与 Skill 关联管理服务。
 *
 * 职责：
 * 1. workspace 圈选/取消圈选 skill
 * 2. 管理版本策略（useLatest / pinnedVersion）
 * 3. skill 发布新版本时，更新 useLatest=true 的关联记录
 * 4. 查询 workspace 已圈选的 skill 列表
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceSkillService {

    private final WorkspaceSkillStore workspaceSkillStore;
    private final SkillStore skillStore;
    private final SkillLoaderService loaderService;
    private final org.dragon.skill.registry.SkillRegistry skillRegistry;

    /**
     * Workspace 圈选一个 Skill。
     */
    public WorkspaceSkillResponse bindSkill(Long workspaceId, WorkspaceSkillBindRequest request) {
        // 1. 检查 skill 是否存在且公开
        SkillEntity skill = skillStore.findById(request.getSkillId())
                .orElseThrow(() -> new SkillNotFoundException(
                        "Skill 不存在: id=" + request.getSkillId()));

        if (skill.getVisibility() == SkillVisibility.PRIVATE) {
            throw new SkillValidationException("私有 Skill 不可被 workspace 圈选");
        }
        if (!skill.getEnabled()) {
            throw new SkillValidationException("该 Skill 当前已被全局禁用，无法圈选");
        }

        // 2. 检查是否已圈选
        if (workspaceSkillStore.existsByWorkspaceIdAndSkillId(
                workspaceId, request.getSkillId())) {
            throw new SkillValidationException("该 Skill 已在当前 workspace 中");
        }

        // 3. 确定锁定版本
        int pinnedVersion;
        if (Boolean.TRUE.equals(request.getUseLatest())) {
            pinnedVersion = skill.getVersion();
        } else {
            if (request.getPinnedVersion() == null) {
                throw new SkillValidationException(
                        "useLatest=false 时必须指定 pinnedVersion");
            }
            if (request.getPinnedVersion() > skill.getVersion()) {
                throw new SkillValidationException(
                        "指定版本 " + request.getPinnedVersion()
                        + " 超过当前最新版本 " + skill.getVersion());
            }
            pinnedVersion = request.getPinnedVersion();
        }

        // 4. 保存关联记录
        WorkspaceSkillEntity entity = WorkspaceSkillEntity.builder()
                .workspaceId(workspaceId)
                .skill(skill)
                .pinnedVersion(pinnedVersion)
                .useLatest(request.getUseLatest())
                .enabled(true)
                .build();
        entity = workspaceSkillStore.save(entity);

        // 5. 触发该 workspace 的 skill 加载
        try {
            loaderService.loadSkillForWorkspace(skill, workspaceId, pinnedVersion);
        } catch (Exception e) {
            log.warn("Skill 加载失败: workspaceId={}, skillId={}", workspaceId, skill.getId());
        }

        log.info("Workspace 圈选 Skill 成功: workspaceId={}, skillId={}, version={}, useLatest={}",
                workspaceId, skill.getId(), pinnedVersion, request.getUseLatest());

        return toResponse(entity, skill);
    }

    /**
     * Workspace 取消圈选一个 Skill。
     */
    public void unbindSkill(Long workspaceId, Long skillId) {
        WorkspaceSkillEntity entity = workspaceSkillStore
                .findByWorkspaceIdAndSkillId(workspaceId, skillId)
                .orElseThrow(() -> new SkillNotFoundException(
                        "该 Skill 未在当前 workspace 中圈选"));

        workspaceSkillStore.delete(entity.getId());

        // 从注册表中注销
        skillRegistry.unregisterForWorkspace(entity.getSkill().getName(), workspaceId);

        log.info("Workspace 取消圈选 Skill: workspaceId={}, skillId={}", workspaceId, skillId);
    }

    /**
     * 更新 Workspace Skill 关联配置。
     */
    public WorkspaceSkillResponse updateBinding(Long workspaceId, Long skillId,
                                                 WorkspaceSkillUpdateRequest request) {
        WorkspaceSkillEntity entity = workspaceSkillStore
                .findByWorkspaceIdAndSkillId(workspaceId, skillId)
                .orElseThrow(() -> new SkillNotFoundException(
                        "该 Skill 未在当前 workspace 中圈选"));

        SkillEntity skill = entity.getSkill();
        boolean needReload = false;

        // 更新版本策略
        if (request.getUseLatest() != null) {
            entity.setUseLatest(request.getUseLatest());
            if (Boolean.TRUE.equals(request.getUseLatest())) {
                entity.setPinnedVersion(skill.getVersion());
                needReload = true;
            }
        }

        if (request.getPinnedVersion() != null
                && Boolean.FALSE.equals(entity.getUseLatest())) {
            if (request.getPinnedVersion() > skill.getVersion()) {
                throw new SkillValidationException(
                        "指定版本超过当前最新版本 " + skill.getVersion());
            }
            entity.setPinnedVersion(request.getPinnedVersion());
            needReload = true;
        }

        if (request.getEnabled() != null) {
            boolean wasEnabled = entity.getEnabled();
            entity.setEnabled(request.getEnabled());
            if (!wasEnabled && request.getEnabled()) {
                needReload = true;
            } else if (wasEnabled && !request.getEnabled()) {
                skillRegistry.unregisterForWorkspace(skill.getName(), workspaceId);
            }
        }

        entity = workspaceSkillStore.update(entity);

        if (needReload) {
            try {
                loaderService.loadSkillForWorkspace(skill, workspaceId, entity.getPinnedVersion());
            } catch (Exception e) {
                log.error("Skill 重新加载失败: workspaceId={}, skillId={}", workspaceId, skillId);
            }
        }

        return toResponse(entity, skill);
    }

    /**
     * 查询 workspace 已圈选的所有 Skill。
     */
    public List<WorkspaceSkillResponse> listWorkspaceSkills(Long workspaceId) {
        return workspaceSkillStore.findAllEnabledByWorkspace(workspaceId)
                .stream()
                .map(ws -> toResponse(ws, ws.getSkill()))
                .collect(Collectors.toList());
    }

    /**
     * Skill 发布新版本后，更新所有 useLatest=true 的关联记录。
     */
    public void onSkillVersionUpdated(Long skillId, Integer newVersion) {
        // 1. 更新 pinnedVersion
        workspaceSkillStore.updatePinnedVersionForLatestFollowers(skillId, newVersion);

        // 2. 找出所有需要热更新的 workspace
        List<WorkspaceSkillEntity> followers =
                workspaceSkillStore.findAllUseLatestBySkillId(skillId);

        if (followers.isEmpty()) {
            log.info("Skill [{}] 无 useLatest=true 的 workspace 订阅，跳过热更新", skillId);
            return;
        }

        log.info("Skill [{}] 新版本 {} 触发热更新，影响 {} 个 workspace",
                skillId, newVersion, followers.size());

        // 3. 逐个触发热更新
        SkillEntity skill = skillStore.findById(skillId).orElse(null);
        if (skill == null) return;

        for (WorkspaceSkillEntity ws : followers) {
            try {
                loaderService.loadSkillForWorkspace(skill, ws.getWorkspaceId(), newVersion);
                log.info("Workspace [{}] Skill 热更新完成", ws.getWorkspaceId());
            } catch (Exception e) {
                log.error("Workspace [{}] Skill 热更新失败: {}",
                        ws.getWorkspaceId(), e.getMessage());
            }
        }
    }

    /**
     * 查询圈选了指定 skill 的所有 workspaceId。
     */
    public List<Long> getAffectedWorkspaces(Long skillId) {
        return workspaceSkillStore.findWorkspaceIdsBySkillId(skillId);
    }

    private WorkspaceSkillResponse toResponse(WorkspaceSkillEntity entity, SkillEntity skill) {
        boolean hasNewVersion = !entity.getUseLatest()
                && skill.getVersion() > entity.getPinnedVersion();

        return WorkspaceSkillResponse.builder()
                .id(entity.getId())
                .workspaceId(entity.getWorkspaceId())
                .skillId(skill.getId())
                .skillName(skill.getName())
                .skillDescription(skill.getSkillDescription())
                .pinnedVersion(entity.getPinnedVersion())
                .useLatest(entity.getUseLatest())
                .latestVersion(skill.getVersion())
                .hasNewVersion(hasNewVersion)
                .enabled(entity.getEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}