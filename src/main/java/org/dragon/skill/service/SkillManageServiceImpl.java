package org.dragon.skill.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.skill.dto.*;
import org.dragon.skill.entity.SkillEntity;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.event.SkillEventPublisher;
import org.dragon.skill.exception.SkillNotFoundException;
import org.dragon.skill.exception.SkillValidationException;
import org.dragon.skill.model.SkillSource;
import org.dragon.skill.registry.SkillRuntimeEntry;
import org.dragon.skill.registry.SkillRuntimeState;
import org.dragon.skill.registry.SkillRegistry;
import org.dragon.skill.store.SkillStore;
import org.dragon.skill.validator.SkillZipValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Skill 管理服务实现。
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillManageServiceImpl implements SkillManageService {

    private final SkillStore skillStore;
    private final SkillZipValidator zipValidator;
    private final SkillLoaderService loaderService;
    private final SkillEventPublisher eventPublisher;
    private final SkillRegistry skillRegistry;
    private final ObjectMapper objectMapper;

    @Value("${skill.storage.root-dir:/data/skills}")
    private String storageRootDir;

    @Override
    public SkillResponse createSkill(MultipartFile file, SkillCreateRequest request) {
        // 1. 校验 ZIP 包，提取 skill name
        String skillName = zipValidator.validateAndExtractName(file);

        // 2. 检查名称唯一性
        if (skillStore.existsByName(skillName)) {
            throw new SkillValidationException("Skill 名称已存在: " + skillName);
        }

        // 3. 校验 workspaceId
        Long workspaceId = request.getWorkspaceId();
        if (request.getSource() == SkillSource.WORKSPACE && (workspaceId == null || workspaceId == 0L)) {
            throw new SkillValidationException("WORKSPACE 来源的 Skill 必须指定有效的 workspaceId");
        }
        if (workspaceId == null) {
            workspaceId = 0L;
        }

        // 4. 解压 ZIP 到存储目录
        String skillDir = resolveSkillDir(request.getSource().name().toLowerCase(), skillName);
        extractZip(file, skillDir);

        // 5. 保存数据库记录
        SkillEntity entity = SkillEntity.builder()
                .name(skillName)
                .source(request.getSource())
                .category(request.getCategory())
                .version(1)
                .tags(serializeTags(request.getTags()))
                .description(request.getDescription())
                .skillDir(skillDir)
                .enabled(true)
                .workspaceId(workspaceId)
                .build();

        entity = skillStore.save(entity);

        // 6. 触发加载
        loaderService.loadSkill(entity);

        // 7. 发布创建事件
        eventPublisher.publishCreated(entity);

        log.info("Skill 创建成功: id={}, name={}", entity.getId(), entity.getName());
        return toResponse(skillStore.findById(entity.getId()).orElse(entity));
    }

    @Override
    public SkillResponse updateSkill(Long skillId, MultipartFile file, SkillUpdateRequest request) {
        SkillEntity entity = requireActiveSkill(skillId);

        boolean fileUpdated = false;

        // 1. 若上传了新 ZIP 包，处理文件更新
        if (file != null && !file.isEmpty()) {
            // 校验 ZIP 包
            String skillName = zipValidator.validateAndExtractName(file);

            // 校验 name 一致性
            if (!entity.getName().equals(skillName)) {
                throw new SkillValidationException(
                        String.format("更新时 ZIP 包中的 name '%s' 必须与当前 Skill name '%s' 一致",
                                skillName, entity.getName()));
            }

            // 清空旧目录，解压新文件
            String skillDir = entity.getSkillDir();
            clearDirectory(skillDir);
            extractZip(file, skillDir);

            // 版本号递增
            entity.setVersion(entity.getVersion() + 1);
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

        entity = skillStore.update(entity);

        // 3. 触发重新加载或发布事件
        if (fileUpdated) {
            loaderService.reloadSkill(skillId);
            eventPublisher.publishVersionUpdated(entity);
        } else {
            eventPublisher.publishMetaUpdated(entity);
        }

        log.info("Skill 更新成功: id={}, name={}, version={}, fileUpdated={}",
                entity.getId(), entity.getName(), entity.getVersion(), fileUpdated);
        return toResponse(skillStore.findById(skillId).orElse(entity));
    }

    @Override
    public void deleteSkill(Long skillId) {
        SkillEntity entity = requireActiveSkill(skillId);

        // 1. 注销运行时
        loaderService.unloadSkill(skillId, entity.getName());

        // 2. 发布删除事件
        eventPublisher.publishDeleted(entity);

        // 3. 软删除数据库记录
        skillStore.softDelete(skillId);

        log.info("Skill 已删除: id={}, name={}", skillId, entity.getName());
    }

    @Override
    public List<SkillResponse> listSkills(SkillQueryRequest request) {
        List<SkillEntity> entities = skillStore.findAll();

        // 过滤
        entities = entities.stream()
                .filter(e -> request.getName() == null ||
                        e.getName().toLowerCase().contains(request.getName().toLowerCase()))
                .filter(e -> request.getSource() == null || e.getSource() == request.getSource())
                .filter(e -> request.getCategory() == null || e.getCategory() == request.getCategory())
                .filter(e -> request.getEnabled() == null ||
                        Boolean.equals(e.getEnabled(), request.getEnabled()))
                .filter(e -> request.getTag() == null ||
                        (e.getTags() != null && e.getTags().contains(request.getTag())))
                .filter(e -> request.getWorkspaceId() == null ||
                        e.getWorkspaceId().equals(request.getWorkspaceId()))
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
        skillStore.update(entity);
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
        entity = skillStore.update(entity);
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
            skillStore.findByName(name).ifPresent(entity -> {
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
        return skillStore.findById(skillId)
                .orElseThrow(() -> new SkillNotFoundException("Skill 不存在: id=" + skillId));
    }

    private String resolveSkillDir(String source, String skillName) {
        return Paths.get(storageRootDir, source, skillName).toAbsolutePath().toString();
    }

    private void extractZip(MultipartFile file, String targetDir) {
        Path targetPath = Paths.get(targetDir);
        try {
            Files.createDirectories(targetPath);

            try (ZipInputStream zis = new ZipInputStream(
                    new BufferedInputStream(file.getInputStream()))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path entryPath = targetPath.resolve(entry.getName()).normalize();

                    // 安全检查：防止 Zip Slip 攻击
                    if (!entryPath.startsWith(targetPath)) {
                        throw new SkillValidationException("ZIP 包含非法路径: " + entry.getName());
                    }

                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        Files.createDirectories(entryPath.getParent());
                        Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new SkillValidationException("ZIP 解压失败: " + e.getMessage(), e);
        }
    }

    private void clearDirectory(String dirPath) {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) return;
        try {
            Files.walk(path)
                    .sorted(java.util.Comparator.reverseOrder())
                    .filter(p -> !p.equals(path))
                    .forEach(p -> {
                        try { Files.delete(p); } catch (IOException ignored) {}
                    });
        } catch (IOException e) {
            log.warn("清空目录失败: {}", dirPath, e);
        }
    }

    private String serializeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> deserializeTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private SkillResponse toResponse(SkillEntity entity) {
        SkillResponse response = SkillResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .source(entity.getSource())
                .category(entity.getCategory())
                .version(entity.getVersion())
                .tags(deserializeTags(entity.getTags()))
                .description(entity.getDescription())
                .skillDir(entity.getSkillDir())
                .enabled(entity.getEnabled())
                .workspaceId(entity.getWorkspaceId())
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
