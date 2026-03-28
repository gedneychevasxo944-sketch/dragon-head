package org.dragon.skill.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.skill.SkillFrontmatterParser;
import org.dragon.skill.dto.*;
import org.dragon.skill.entity.SkillEntity;
import org.dragon.skill.event.SkillEventPublisher;
import org.dragon.skill.exception.SkillNotFoundException;
import org.dragon.skill.exception.SkillValidationException;
import org.dragon.skill.registry.SkillRuntimeState;
import org.dragon.skill.registry.SkillRegistry;
import org.dragon.skill.storage.SkillStorageBackend;
import org.dragon.skill.store.SkillStore;
import org.dragon.skill.validator.SkillZipValidator;
import org.dragon.store.StoreFactory;
import org.dragon.util.GsonUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Skill 管理服务实现。
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillManageServiceImpl implements SkillManageService {

    private final SkillZipValidator zipValidator;
    private final SkillLoaderService loaderService;
    private final SkillEventPublisher eventPublisher;
    private final SkillRegistry skillRegistry;
    private final SkillStorageBackend storageBackend;
    private final StoreFactory storeFactory;

    private SkillStore getSkillStore() {
        return storeFactory.get(SkillStore.class);
    }

    @Override
    public SkillResponse createSkill(MultipartFile file, SkillCreateRequest request) {
        // 步骤1：校验 ZIP 包，同时提取 SKILL.md 原始内容
        SkillZipValidator.ZipValidationResult validationResult =
                zipValidator.validateAndExtract(file);
        String skillName = validationResult.getSkillName();

        // 步骤2：检查名称唯一性
        if (getSkillStore().existsByName(skillName)) {
            throw new SkillValidationException("Skill 名称已存在: " + skillName);
        }

        // 步骤3：解析 SKILL.md 内容（从内存字符串解析，无需读文件）
        SkillFrontmatterParser.SkillParseResult parseResult =
                SkillFrontmatterParser.parseFromString(validationResult.getSkillMdRawContent());

        // 步骤4：确定版本号（新建固定为 1）
        int version = 1;

        // 步骤5：上传文件到存储后端
        long creatorId = request.getCreatorId() != null ? request.getCreatorId() : 0L;
        String storagePath;
        try {
            storagePath = storageBackend.store(
                    creatorId, skillName, version, file.getInputStream());
        } catch (java.io.IOException e) {
            throw new SkillValidationException("文件存储失败: " + e.getMessage(), e);
        }

        // 步骤6：将解析结果持久化到数据库
        SkillEntity entity = SkillEntity.builder()
                .name(skillName)
                .category(request.getCategory())
                .version(version)
                .tags(serializeTags(request.getTags()))
                .description(request.getDescription())
                .storagePath(storagePath)
                // 直接存入解析结果，无需运行时再解析文件
                .skillDescription(parseResult.getSkillDescription())
                .skillContent(parseResult.getSkillContent())
                .requiresConfig(serializeObject(parseResult.getRequires()))
                .installConfig(serializeObject(parseResult.getInstallSpecs()))
                .frontmatterRaw(parseResult.getFrontmatterRaw())
                .enabled(true)
                .visibility(request.getVisibility())
                .creatorId(creatorId)
                .creatorType(request.getCreatorType())
                .build();

        entity = getSkillStore().save(entity);

        // 步骤7：触发运行时加载（直接从数据库字段构建，无需读文件）
        loaderService.loadSkill(entity);

        // 步骤8：发布事件
        eventPublisher.publishCreated(entity);

        log.info("Skill 创建成功: id={}, name={}", entity.getId(), entity.getName());
        return toResponse(getSkillStore().findById(entity.getId()).orElse(entity));
    }

    @Override
    public SkillResponse updateSkill(Long skillId, MultipartFile file, SkillUpdateRequest request) {
        SkillEntity entity = requireActiveSkill(skillId);

        boolean fileUpdated = false;

        // 1. 若上传了新 ZIP 包，处理文件更新
        if (file != null && !file.isEmpty()) {
            // 校验 ZIP 包
            SkillZipValidator.ZipValidationResult validationResult =
                    zipValidator.validateAndExtract(file);

            // 校验 name 一致性
            if (!entity.getName().equals(validationResult.getSkillName())) {
                throw new SkillValidationException(
                        String.format("更新时 ZIP 包中的 name '%s' 必须与当前 Skill name '%s' 一致",
                                validationResult.getSkillName(), entity.getName()));
            }

            // 解析新版本 SKILL.md
            SkillFrontmatterParser.SkillParseResult parseResult =
                    SkillFrontmatterParser.parseFromString(validationResult.getSkillMdRawContent());

            int newVersion = entity.getVersion() + 1;

            // 上传新版本到存储后端（新版本独立目录，旧版本删除）
            long creatorId = entity.getCreatorId();
            String newStoragePath;
            try {
                newStoragePath = storageBackend.store(
                        creatorId, entity.getName(), newVersion, file.getInputStream());
            } catch (java.io.IOException e) {
                throw new SkillValidationException("文件存储失败: " + e.getMessage(), e);
            }

            // 删除旧版本文件
            if (entity.getStoragePath() != null) {
                storageBackend.delete(entity.getStoragePath());
            }

            // 更新数据库字段
            entity.setVersion(newVersion);
            entity.setStoragePath(newStoragePath);
            entity.setSkillDescription(parseResult.getSkillDescription());
            entity.setSkillContent(parseResult.getSkillContent());
            entity.setRequiresConfig(serializeObject(parseResult.getRequires()));
            entity.setInstallConfig(serializeObject(parseResult.getInstallSpecs()));
            entity.setFrontmatterRaw(parseResult.getFrontmatterRaw());

            fileUpdated = true;
        }

        // 2. 更新管理元数据
        if (request.getCategory() != null) {
            entity.setCategory(request.getCategory());
        }
        if (request.getTags() != null) {
            entity.setTags(serializeTags(request.getTags()));
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }

        entity = getSkillStore().update(entity);

        // 3. 触发重新加载或发布事件
        if (fileUpdated) {
            loaderService.reloadSkill(skillId);
            eventPublisher.publishVersionUpdated(entity);
        } else {
            eventPublisher.publishMetaUpdated(entity);
        }

        log.info("Skill 更新成功: id={}, name={}, version={}, fileUpdated={}",
                entity.getId(), entity.getName(), entity.getVersion(), fileUpdated);
        return toResponse(getSkillStore().findById(skillId).orElse(entity));
    }

    @Override
    public void deleteSkill(Long skillId) {
        SkillEntity entity = requireActiveSkill(skillId);

        // 1. 注销运行时
        loaderService.unloadSkill(skillId, entity.getName());

        // 2. 删除存储后端文件
        if (entity.getStoragePath() != null) {
            storageBackend.delete(entity.getStoragePath());
        }

        // 3. 发布删除事件
        eventPublisher.publishDeleted(entity);

        // 4. 软删除数据库记录
        getSkillStore().softDelete(skillId);

        log.info("Skill 已删除: id={}, name={}", skillId, entity.getName());
    }

    @Override
    public List<SkillResponse> listSkills(SkillQueryRequest request) {
        List<SkillEntity> entities = getSkillStore().findAll();

        // 过滤
        entities = entities.stream()
                .filter(e -> request.getName() == null ||
                        e.getName().toLowerCase().contains(request.getName().toLowerCase()))
                .filter(e -> request.getCategory() == null || e.getCategory() == request.getCategory())
                .filter(e -> request.getVisibility() == null || e.getVisibility() == request.getVisibility())
                .filter(e -> request.getCreatorType() == null || e.getCreatorType() == request.getCreatorType())
                .filter(e -> request.getCreatorId() == null || e.getCreatorId().equals(request.getCreatorId()))
                .filter(e -> request.getEnabled() == null ||
                        Objects.equals(e.getEnabled(), request.getEnabled()))
                .filter(e -> request.getTag() == null ||
                        (e.getTags() != null && e.getTags().contains(request.getTag())))
                .collect(Collectors.toList());

        // 分页
        int page = Math.max(1, request.getPage());
        int size = request.getSize();
        int start = (page - 1) * size;
        int end = Math.min(start + size, entities.size());

        if (start >= entities.size()) {
            return new ArrayList<>();
        }

        return entities.subList(start, end).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SkillResponse getSkill(Long skillId) {
        return toResponse(requireActiveSkill(skillId));
    }

    @Override
    public void disableSkill(Long skillId) {
        SkillEntity entity = requireActiveSkill(skillId);
        if (!entity.getEnabled()) {
            throw new SkillValidationException("Skill 已经是禁用状态");
        }
        // 从运行时注销
        loaderService.unloadSkill(skillId, entity.getName());
        // 仅更新 enabled 字段
        entity.setEnabled(false);
        getSkillStore().update(entity);
        eventPublisher.publishDisabled(entity);
        log.info("Skill 已禁用: id={}, name={}", skillId, entity.getName());
    }

    @Override
    public void enableSkill(Long skillId) {
        SkillEntity entity = requireActiveSkill(skillId);
        if (entity.getEnabled()) {
            throw new SkillValidationException("Skill 已经是启用状态");
        }
        // 先更新 enabled 字段
        entity.setEnabled(true);
        entity = getSkillStore().update(entity);
        // 触发加载
        loaderService.loadSkill(entity);
        eventPublisher.publishActivated(entity);
        log.info("Skill 已启用: id={}, name={}", skillId, entity.getName());
    }

    @Override
    public void reloadSkill(Long skillId) {
        requireActiveSkill(skillId);
        loaderService.reloadSkill(skillId);
        log.info("Skill 手动重新加载触发: id={}", skillId);
    }

    @Override
    public Map<String, SkillRuntimeState> getRuntimeStateSnapshot() {
        return skillRegistry.getRuntimeStateSnapshot();
    }

    @Override
    public void retryFailedSkills() {
        Map<String, SkillRuntimeState> snapshot = skillRegistry.getRuntimeStateSnapshot();
        List<String> failedNames = snapshot.entrySet().stream()
                .filter(e -> e.getValue() == SkillRuntimeState.FAILED)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        log.info("开始重试 {} 个 FAILED 状态的 Skill", failedNames.size());
        for (String name : failedNames) {
            getSkillStore().findByName(name).ifPresent(entity -> {
                log.info("重试加载 Skill: id={}, name={}", entity.getId(), entity.getName());
                skillRegistry.unregister(name); // 先清除旧的 FAILED 记录
                loaderService.loadSkill(entity);
            });
        }
    }

    @Override
    public void fullReload() {
        log.info("开始全量重新加载 Skill...");
        skillRegistry.clear();
        loaderService.loadAll();
        log.info("全量重新加载完成，当前注册数量: {}", skillRegistry.size());
    }

    @Override
    public String getSystemPromptFragment(long workspaceId) {
        return skillRegistry.buildSystemPromptFragment(workspaceId);
    }

    // ==================== 私有工具方法 ====================

    private SkillEntity requireActiveSkill(Long skillId) {
        return getSkillStore().findById(skillId)
                .orElseThrow(() -> new SkillNotFoundException("Skill 不存在: id=" + skillId));
    }

    private String serializeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return "[]";
        try {
            return GsonUtils.toJson(tags);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> deserializeTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) return new ArrayList<>();
        try {
            return GsonUtils.fromJsonList(tagsJson, String.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String serializeObject(Object obj) {
        if (obj == null) return null;
        try {
            return GsonUtils.toJson(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private SkillResponse toResponse(SkillEntity entity) {
        SkillResponse response = SkillResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .category(entity.getCategory())
                .version(entity.getVersion())
                .tags(deserializeTags(entity.getTags()))
                .description(entity.getDescription())
                .storagePath(entity.getStoragePath())
                .skillDescription(entity.getSkillDescription())
                .skillContent(entity.getSkillContent())
                .enabled(entity.getEnabled())
                .visibility(entity.getVisibility())
                .creatorId(entity.getCreatorId())
                .creatorType(entity.getCreatorType())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        // 从运行时注册表补充运行时状态
        skillRegistry.findById(entity.getId()).ifPresent(runtimeEntry -> {
            response.setRuntimeState(runtimeEntry.getState());
            response.setRuntimeError(runtimeEntry.getErrorMessage());
        });

        return response;
    }
}