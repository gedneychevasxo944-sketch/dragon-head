package org.dragon.skill.service;

import org.dragon.skill.dto.SkillBindingRequest;
import org.dragon.skill.dto.SkillBindingResponse;
import org.dragon.skill.dto.SkillBindingUpdateRequest;
import org.dragon.skill.entity.SkillBindingEntity;
import org.dragon.skill.entity.SkillEntity;
import org.dragon.skill.enums.BindType;
import org.dragon.skill.enums.SkillVisibility;
import org.dragon.skill.exception.SkillNotFoundException;
import org.dragon.skill.exception.SkillValidationException;
import org.dragon.skill.registry.SkillRegistry;
import org.dragon.skill.store.SkillBindingStore;
import org.dragon.skill.store.SkillStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Skill 统一绑定管理服务。
 *
 * 职责：
 * 1. workspace 圈选/取消圈选 skill
 * 2. character 绑定/取消绑定 skill
 * 3. 管理版本策略（useLatest / pinnedVersion）
 * 4. skill 发布新版本时，更新 useLatest=true 的关联记录
 * 5. 查询绑定关系列表
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillBindingService {

    private final SkillBindingStore skillBindingStore;
    private final SkillStore skillStore;
    private final SkillLoaderService loaderService;
    private final SkillRegistry skillRegistry;

    // ==================== Workspace 操作 ====================

    /**
     * Workspace 圈选一个 Skill。
     */
    public SkillBindingResponse bindWorkspaceSkill(Long workspaceId, SkillBindingRequest request) {
        return bindInternal(
                BindType.WORKSPACE,
                workspaceId,
                null,
                request.getSkillId(),
                request.getUseLatest(),
                request.getPinnedVersion(),
                () -> skillBindingStore.bindingExists(request.getSkillId(), BindType.WORKSPACE, workspaceId, null, false),
                "该 Skill 已在当前 workspace 中"
        );
    }

    /**
     * Workspace 取消圈选一个 Skill。
     */
    public void unbindWorkspaceSkill(Long workspaceId, Long skillId) {
        unbindInternal(
                BindType.WORKSPACE,
                workspaceId,
                null,
                skillId,
                () -> skillBindingStore.findBinding(BindType.WORKSPACE, workspaceId, null, skillId, false),
                "该 Skill 未在当前 workspace 中圈选"
        );
    }

    /**
     * 更新 Workspace Skill 关联配置。
     */
    public SkillBindingResponse updateWorkspaceBinding(Long workspaceId, Long skillId, SkillBindingUpdateRequest request) {
        return updateBindingInternal(
                BindType.WORKSPACE,
                workspaceId,
                null,
                skillId,
                request,
                () -> skillBindingStore.findBinding(BindType.WORKSPACE, workspaceId, null, skillId, false),
                "该 Skill 未在当前 workspace 中圈选",
                entity -> toSkillBindingResponse(entity, entity.getSkill())
        );
    }

    /**
     * 查询 workspace 已圈选的所有 Skill。
     */
    public List<SkillBindingResponse> listWorkspaceSkills(Long workspaceId) {
        return skillBindingStore.findEnabledBindings(BindType.WORKSPACE, workspaceId, null).stream()
                .map(entity -> toSkillBindingResponse(entity, entity.getSkill()))
                .collect(Collectors.toList());
    }

    /**
     * Skill 发布新版本后，更新所有 useLatest=true 的 workspace 关联记录。
     */
    public void onSkillVersionUpdatedForWorkspace(Long skillId, Integer newVersion) {
        hotUpdateInternal(BindType.WORKSPACE, skillId, newVersion);
    }

    /**
     * 查询圈选了指定 skill 的所有 workspaceId。
     */
    public List<Long> getAffectedWorkspaces(Long skillId) {
        return skillBindingStore.findWorkspaceIdsBySkillId(skillId);
    }

    // ==================== Character 操作 ====================

    /**
     * Character 绑定一个 Skill。
     * 支持 CHARACTER（全workspace）或 CHARACTER_WORKSPACE（指定workspace）两种类型。
     */
    public SkillBindingResponse bindCharacterSkill(String characterId, Long workspaceId,
                                                      SkillBindingRequest request) {
        BindType bindType = request.getWorkspaceId() != null ? BindType.CHARACTER_WORKSPACE : BindType.CHARACTER;
        boolean globalOnly = bindType == BindType.CHARACTER;

        return bindInternal(
                bindType,
                workspaceId,
                characterId,
                request.getSkillId(),
                request.getUseLatest(),
                request.getPinnedVersion(),
                () -> skillBindingStore.bindingExists(request.getSkillId(), bindType, workspaceId, characterId, globalOnly),
                "该 Skill 已在当前 character 中绑定"
        );
    }

    /**
     * Character 取消绑定一个 Skill。
     */
    public void unbindCharacterSkill(String characterId, Long workspaceId, Long skillId) {
        BindType bindType = workspaceId != null ? BindType.CHARACTER_WORKSPACE : BindType.CHARACTER;
        boolean globalOnly = bindType == BindType.CHARACTER;

        unbindInternal(
                bindType,
                workspaceId,
                characterId,
                skillId,
                () -> skillBindingStore.findBinding(bindType, workspaceId, characterId, skillId, globalOnly),
                globalOnly ? "该 Skill 未在当前 character 全局绑定下" : "该 Skill 未在当前 character+workspace 下绑定"
        );
    }

    /**
     * 更新 Character Skill 关联配置。
     */
    public SkillBindingResponse updateCharacterBinding(String characterId, Long workspaceId,
                                                        Long skillId, SkillBindingUpdateRequest request) {
        BindType bindType = workspaceId != null ? BindType.CHARACTER_WORKSPACE : BindType.CHARACTER;
        boolean globalOnly = bindType == BindType.CHARACTER;

        return updateBindingInternal(
                bindType,
                workspaceId,
                characterId,
                skillId,
                request,
                () -> skillBindingStore.findBinding(bindType, workspaceId, characterId, skillId, globalOnly),
                globalOnly ? "该 Skill 未在当前 character 全局绑定下" : "该 Skill 未在当前 character+workspace 下绑定",
                entity -> toSkillBindingResponse(entity, entity.getSkill())
        );
    }

    /**
     * 查询 character 绑定的所有 Skill。
     */
    public List<SkillBindingResponse> listCharacterSkills(String characterId) {
        return skillBindingStore.findEnabledBindings(null, null, characterId).stream()
                .map(entity -> toSkillBindingResponse(entity, entity.getSkill()))
                .collect(Collectors.toList());
    }

    /**
     * 获取 character 在特定 workspace 下的有效技能（合并优先级）。
     * 用于加载到运行时。
     */
    public List<SkillBindingResponse> getEffectiveSkills(String characterId, Long workspaceId) {
        return skillBindingStore.resolveEffectiveBinds(characterId, workspaceId).stream()
                .map(entity -> toSkillBindingResponse(entity, entity.getSkill()))
                .collect(Collectors.toList());
    }

    /**
     * Skill 发布新版本后，更新所有 useLatest=true 的 character 关联记录。
     */
    public void onSkillVersionUpdatedForCharacter(Long skillId, Integer newVersion) {
        hotUpdateInternal(BindType.CHARACTER, skillId, newVersion);
    }

    // ==================== 内部统一方法 ====================

    /**
     * 统一绑定入口。
     */
    private SkillBindingResponse bindInternal(BindType bindType, Long workspaceId, String characterId,
                               Long skillId, Boolean useLatest, Integer pinnedVersion,
                               java.util.function.Supplier<Boolean> existsChecker, String existsErrorMsg) {
        // 1. 校验 skill
        SkillEntity skill = validateSkill(skillId, bindType);

        // 2. 检查是否已存在
        if (existsChecker.get()) {
            throw new SkillValidationException(existsErrorMsg);
        }

        // 3. 确定版本
        int resolvedVersion = resolvePinnedVersion(skill, useLatest, pinnedVersion);

        // 4. 构建并保存实体
        SkillBindingEntity entity = buildBindingEntity(skill, bindType, workspaceId, characterId, resolvedVersion, useLatest);
        entity = skillBindingStore.save(entity);

        // 5. 加载
        loadSkill(skill, workspaceId, characterId, bindType, resolvedVersion);

        // 6. 日志
        logBindSuccess(bindType, workspaceId, characterId, skill.getId(), resolvedVersion, useLatest);

        return toSkillBindingResponse(entity, skill);
    }

    /**
     * 统一取消绑定入口。
     */
    private void unbindInternal(BindType bindType, Long workspaceId, String characterId, Long skillId,
                                java.util.function.Supplier<java.util.Optional<SkillBindingEntity>> finder, String notFoundErrorMsg) {
        SkillBindingEntity entity = finder.get()
                .orElseThrow(() -> new SkillNotFoundException(notFoundErrorMsg));

        skillBindingStore.delete(entity.getId());
        unregister(entity.getSkill().getName(), workspaceId, characterId, bindType);

        logUnbindSuccess(bindType, workspaceId, characterId, skillId);
    }

    /**
     * 统一更新绑定配置入口。
     */
    private <T> T updateBindingInternal(BindType bindType, Long workspaceId, String characterId, Long skillId,
                                        Object request,
                                        java.util.function.Supplier<java.util.Optional<SkillBindingEntity>> finder, String notFoundErrorMsg,
                                        Function<SkillBindingEntity, T> responseConverter) {
        SkillBindingEntity entity = finder.get()
                .orElseThrow(() -> new SkillNotFoundException(notFoundErrorMsg));

        boolean needReload = updateVersionAndEnabled(entity, entity.getSkill(), request, workspaceId, characterId, bindType);
        entity = skillBindingStore.update(entity);

        if (needReload) {
            reloadSkill(entity.getSkill(), workspaceId, characterId, bindType);
        }

        return responseConverter.apply(entity);
    }

    /**
     * 统一热更新入口。
     */
    private void hotUpdateInternal(BindType bindType, Long skillId, Integer newVersion) {
        // 1. 更新 pinnedVersion
        skillBindingStore.updatePinnedVersionForLatestFollowers(skillId, newVersion);

        // 2. 过滤订阅者
        List<SkillBindingEntity> followers = skillBindingStore.findAllUseLatestBySkillId(skillId).stream()
                .filter(bind -> bindType == BindType.WORKSPACE
                        ? bind.getBindType() == BindType.WORKSPACE
                        : (bind.getBindType() == BindType.CHARACTER || bind.getBindType() == BindType.CHARACTER_WORKSPACE))
                .toList();

        if (followers.isEmpty()) {
            log.info("Skill [{}] 无 useLatest=true 的 {} 订阅，跳过热更新", skillId, bindType);
            return;
        }

        log.info("Skill [{}] 新版本 {} 触发热更新，影响 {} 个 {}", skillId, newVersion, followers.size(), bindType);

        // 3. 执行热更新
        SkillEntity skill = skillStore.findById(skillId).orElse(null);
        if (skill == null) return;

        for (SkillBindingEntity bind : followers) {
            String targetId = bindType == BindType.WORKSPACE
                    ? String.valueOf(bind.getWorkspaceId())
                    : bind.getCharacterId();

            try {
                loadSkillForBindType(skill, bind.getWorkspaceId(), bind.getCharacterId(), bindType, newVersion);
                log.info("{} [{}] Skill 热更新完成", bindType, targetId);
            } catch (Exception e) {
                log.error("{} [{}] Skill 热更新失败: {}", bindType, targetId, e.getMessage());
            }
        }
    }

    // ==================== 公共辅助方法 ====================

    /**
     * 校验 skill。
     */
    private SkillEntity validateSkill(Long skillId, BindType bindType) {
        SkillEntity skill = skillStore.findById(skillId)
                .orElseThrow(() -> new SkillNotFoundException("Skill 不存在: id=" + skillId));

        if (skill.getVisibility() == SkillVisibility.PRIVATE) {
            String errorMsg = bindType == BindType.WORKSPACE
                    ? "私有 Skill 不可被 workspace 圈选"
                    : "私有 Skill 不可被 character 绑定";
            throw new SkillValidationException(errorMsg);
        }
        if (!skill.getEnabled()) {
            String errorMsg = bindType == BindType.WORKSPACE
                    ? "该 Skill 当前已被全局禁用，无法圈选"
                    : "该 Skill 当前已被全局禁用，无法绑定";
            throw new SkillValidationException(errorMsg);
        }
        return skill;
    }

    /**
     * 确定锁定版本。
     */
    private int resolvePinnedVersion(SkillEntity skill, Boolean useLatest, Integer pinnedVersion) {
        if (Boolean.TRUE.equals(useLatest)) {
            return skill.getVersion();
        }
        if (pinnedVersion == null) {
            throw new SkillValidationException("useLatest=false 时必须指定 pinnedVersion");
        }
        if (pinnedVersion > skill.getVersion()) {
            throw new SkillValidationException("指定版本 " + pinnedVersion + " 超过当前最新版本 " + skill.getVersion());
        }
        return pinnedVersion;
    }

    /**
     * 构建绑定实体。
     */
    private SkillBindingEntity buildBindingEntity(SkillEntity skill, BindType bindType,
                                                  Long workspaceId, String characterId,
                                                  int pinnedVersion, Boolean useLatest) {
        return SkillBindingEntity.builder()
                .skill(skill)
                .bindType(bindType)
                .workspaceId(workspaceId)
                .characterId(characterId)
                .pinnedVersion(pinnedVersion)
                .useLatest(useLatest)
                .enabled(true)
                .build();
    }

    /**
     * 加载 skill（根据 bindType 调用不同方法）。
     */
    private void loadSkill(SkillEntity skill, Long workspaceId, String characterId, BindType bindType, int pinnedVersion) {
        try {
            if (bindType == BindType.WORKSPACE) {
                loaderService.loadSkillForWorkspace(skill, workspaceId, pinnedVersion);
            } else {
                loaderService.loadSkillForCharacter(characterId, workspaceId, skill, pinnedVersion);
            }
        } catch (Exception e) {
            if (bindType == BindType.WORKSPACE) {
                log.warn("Skill 加载失败: workspaceId={}, skillId={}", workspaceId, skill.getId());
            } else {
                log.warn("Character Skill 加载失败: characterId={}, skillId={}", characterId, skill.getId());
            }
        }
    }

    /**
     * 热更新时加载 skill（根据 bindType 调用不同方法）。
     */
    private void loadSkillForBindType(SkillEntity skill, Long workspaceId, String characterId, BindType bindType, int version) {
        if (bindType == BindType.WORKSPACE) {
            loaderService.loadSkillForWorkspace(skill, workspaceId, version);
        } else {
            loaderService.loadSkillForCharacter(characterId, workspaceId, skill, version);
        }
    }

    /**
     * 注销注册。
     */
    private void unregister(String skillName, Long workspaceId, String characterId, BindType bindType) {
        if (bindType == BindType.WORKSPACE) {
            skillRegistry.unregisterForWorkspace(skillName, workspaceId);
        } else {
            skillRegistry.unregisterForCharacter(characterId, skillName, workspaceId);
        }
    }

    /**
     * 重新加载 skill。
     */
    private void reloadSkill(SkillEntity skill, Long workspaceId, String characterId, BindType bindType) {
        try {
            if (bindType == BindType.WORKSPACE) {
                loaderService.loadSkillForWorkspace(skill, workspaceId, skill.getVersion());
            } else {
                loaderService.loadSkillForCharacter(characterId, workspaceId, skill, skill.getVersion());
            }
        } catch (Exception e) {
            String targetType = bindType == BindType.WORKSPACE ? "workspace" : "character";
            log.error("Skill 重新加载失败: {}Id={}, skillId={}",
                    targetType, bindType == BindType.WORKSPACE ? workspaceId : characterId, skill.getId());
        }
    }

    /**
     * 更新版本策略和启用状态。
     */
    private boolean updateVersionAndEnabled(SkillBindingEntity entity, SkillEntity skill,
                                           Object request, Long workspaceId, String characterId, BindType bindType) {
        boolean needReload = false;

        // 获取请求参数
        Boolean useLatest = getUseLatest(request);
        Integer pinnedVersion = getPinnedVersion(request);
        Boolean enabled = getEnabled(request);

        // 更新版本策略
        if (useLatest != null) {
            entity.setUseLatest(useLatest);
            if (Boolean.TRUE.equals(useLatest)) {
                entity.setPinnedVersion(skill.getVersion());
                needReload = true;
            }
        }

        if (pinnedVersion != null && Boolean.FALSE.equals(entity.getUseLatest())) {
            if (pinnedVersion > skill.getVersion()) {
                throw new SkillValidationException("指定版本超过当前最新版本 " + skill.getVersion());
            }
            entity.setPinnedVersion(pinnedVersion);
            needReload = true;
        }

        // 更新启用状态
        if (enabled != null) {
            boolean wasEnabled = entity.getEnabled();
            entity.setEnabled(enabled);
            if (!wasEnabled && enabled) {
                needReload = true;
            } else if (wasEnabled && !enabled) {
                unregister(skill.getName(), workspaceId, characterId, bindType);
            }
        }

        return needReload;
    }

    private Boolean getUseLatest(Object request) {
        if (request instanceof SkillBindingUpdateRequest) {
            return ((SkillBindingUpdateRequest) request).getUseLatest();
        }
        return null;
    }

    private Integer getPinnedVersion(Object request) {
        if (request instanceof SkillBindingUpdateRequest) {
            return ((SkillBindingUpdateRequest) request).getPinnedVersion();
        }
        return null;
    }

    private Boolean getEnabled(Object request) {
        if (request instanceof SkillBindingUpdateRequest) {
            return ((SkillBindingUpdateRequest) request).getEnabled();
        }
        return null;
    }

    // ==================== 日志方法 ====================

    private void logBindSuccess(BindType bindType, Long workspaceId, String characterId, Long skillId,
                                int pinnedVersion, Boolean useLatest) {
        if (bindType == BindType.WORKSPACE) {
            log.info("Workspace 圈选 Skill 成功: workspaceId={}, skillId={}, version={}, useLatest={}",
                    workspaceId, skillId, pinnedVersion, useLatest);
        } else {
            log.info("Character 绑定 Skill 成功: characterId={}, workspaceId={}, skillId={}, version={}, useLatest={}",
                    characterId, workspaceId, skillId, pinnedVersion, useLatest);
        }
    }

    private void logUnbindSuccess(BindType bindType, Long workspaceId, String characterId, Long skillId) {
        if (bindType == BindType.WORKSPACE) {
            log.info("Workspace 取消圈选 Skill: workspaceId={}, skillId={}", workspaceId, skillId);
        } else {
            log.info("Character 取消绑定 Skill: characterId={}, workspaceId={}, skillId={}", characterId, workspaceId, skillId);
        }
    }

    // ==================== 响应转换方法 ====================

    private SkillBindingResponse toSkillBindingResponse(SkillBindingEntity entity, SkillEntity skill) {
        boolean hasNewVersion = !entity.getUseLatest() && skill.getVersion() > entity.getPinnedVersion();

        return SkillBindingResponse.builder()
                .id(entity.getId())
                .workspaceId(entity.getWorkspaceId())
                .characterId(entity.getCharacterId())
                .skillId(skill.getId())
                .skillName(skill.getName())
                .skillDescription(skill.getSkillDescription())
                .category(skill.getCategory() != null ? skill.getCategory().name() : null)
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