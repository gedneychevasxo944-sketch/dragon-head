package org.dragon.skill.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.skill.SkillFrontmatterParser;
import org.dragon.skill.entity.SkillEntity;
import org.dragon.skill.model.Skill;
import org.dragon.skill.model.SkillEntry;
import org.dragon.skill.model.SkillInstallSpec;
import org.dragon.skill.model.SkillMetadata;
import org.dragon.skill.model.SkillRequires;
import org.dragon.skill.registry.SkillRuntimeEntry;
import org.dragon.skill.registry.SkillRuntimeState;
import org.dragon.skill.registry.SkillRegistry;
import org.dragon.skill.store.SkillStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Skill 运行时加载服务实现。
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillLoaderServiceImpl implements SkillLoaderService {

    private final SkillStore skillStore;
    private final SkillRegistry skillRegistry;
    private final ObjectMapper objectMapper;
    private final org.dragon.skill.store.WorkspaceSkillStore workspaceSkillStore;

    @Override
    public void loadAll() {
        log.info("开始全量加载 Skill...");
        List<SkillEntity> entities = skillStore.findAllEnabled();

        int successCount = 0;
        int failCount = 0;

        for (SkillEntity entity : entities) {
            Optional<SkillRuntimeEntry> result = loadSkill(entity);
            if (result.isPresent()) {
                successCount++;
            } else {
                failCount++;
            }
        }

        log.info("全量加载完成：成功 {} 个，失败 {} 个，共 {} 个",
                successCount, failCount, entities.size());
    }

    @Override
    public void loadByWorkspace(Long workspaceId) {
        log.info("按工作空间加载 Skill: workspaceId={}", workspaceId);
        List<SkillEntity> entities = skillStore.findAllEnabledByWorkspace(workspaceId);
        for (SkillEntity entity : entities) {
            // 若该 Skill 已在注册表中，跳过重复加载
            if (skillRegistry.findById(entity.getId()).isPresent()) {
                log.debug("Skill 已在注册表中，跳过重复加载: name={}", entity.getName());
                continue;
            }
            loadSkill(entity);
        }
        log.info("工作空间 Skill 加载完成: workspaceId={}", workspaceId);
    }

    @Override
    public Optional<SkillRuntimeEntry> loadSkill(SkillEntity entity) {
        String skillName = entity.getName();
        Long skillId = entity.getId();

        log.info("从数据库加载 Skill: id={}, name={}, version={}", skillId, skillName, entity.getVersion());

        try {
            // ✅ 直接从数据库字段构建 SkillEntry，无需读取文件
            Skill skill = Skill.builder()
                    .id(skillId)
                    .version(entity.getVersion())
                    .name(skillName)
                    .description(entity.getSkillDescription())   // 来自数据库
                    .storagePath(entity.getStoragePath())        // 存储路径（替代原 filePath）
                    .content(entity.getSkillContent())           // 来自数据库
                    .build();

            // 反序列化 requires 和 install
            SkillRequires requires = deserializeObject(entity.getRequiresConfig(), SkillRequires.class);
            List<SkillInstallSpec> installSpecs = deserializeList(entity.getInstallConfig(),
                    new TypeReference<List<SkillInstallSpec>>() {});

            SkillMetadata metadata = SkillMetadata.builder()
                    .requires(requires)
                    .install(installSpecs)
                    .build();

            // frontmatter 从原始 YAML 反序列化
            Map<String, String> frontmatter = parseFrontmatterRaw(entity.getFrontmatterRaw());

            SkillEntry entry = SkillEntry.builder()
                    .skill(skill)
                    .frontmatter(frontmatter)
                    .metadata(metadata)
                    .build();

            // 依赖检查（仍然需要，检查运行环境）
            Optional<String> requiresError = checkRequires(entry);

            SkillRuntimeEntry runtimeEntry = SkillRuntimeEntry.builder()
                    .skillEntry(entry)
                    .workspaceId(null)  // 系统级别加载，不关联特定 workspace
                    .stateChangedAt(LocalDateTime.now())
                    .state(requiresError.isPresent() ? SkillRuntimeState.FAILED : SkillRuntimeState.ACTIVE)
                    .errorMessage(requiresError.orElse(null))
                    .build();

            skillRegistry.register(runtimeEntry);

            log.info("Skill [{}] 加载完成，状态: {}", skillName, runtimeEntry.getState());
            return Optional.of(runtimeEntry);

        } catch (Exception e) {
            log.error("Skill [{}] 加载失败: {}", skillName, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public void reloadSkill(Long skillId) {
        skillStore.findById(skillId).ifPresentOrElse(
                entity -> {
                    // 先注销旧版本
                    skillRegistry.unregister(entity.getName());
                    // 重新加载
                    loadSkill(entity);
                },
                () -> log.warn("尝试重新加载不存在的 Skill: id={}", skillId)
        );
    }

    @Override
    public void unloadSkill(Long skillId, String skillName) {
        skillRegistry.unregister(skillName);
        log.info("Skill [{}] 已从运行时注册表注销", skillName);
    }

    @Override
    public void loadSkillForWorkspace(SkillEntity skill, Long workspaceId, int version) {
        log.info("为 workspace 加载 Skill: workspaceId={}, skillName={}, version={}",
                workspaceId, skill.getName(), version);

        try {
            // 构建版本化的 storagePath
            String basePath = skill.getStoragePath();
            int lastSlash = basePath.lastIndexOf('/');
            String versionedStoragePath = basePath.substring(0, lastSlash + 1) + version;

            // 从数据库字段构建 SkillEntry
            Skill skillDomain = Skill.builder()
                    .id(skill.getId())
                    .version(version)
                    .name(skill.getName())
                    .description(skill.getSkillDescription())
                    .storagePath(versionedStoragePath)
                    .content(skill.getSkillContent())
                    .build();

            SkillRequires requires = deserializeObject(skill.getRequiresConfig(), SkillRequires.class);
            List<SkillInstallSpec> installSpecs = deserializeList(skill.getInstallConfig(),
                    new TypeReference<List<SkillInstallSpec>>() {});

            SkillMetadata metadata = SkillMetadata.builder()
                    .requires(requires)
                    .install(installSpecs)
                    .build();

            Map<String, String> frontmatter = parseFrontmatterRaw(skill.getFrontmatterRaw());

            SkillEntry entry = SkillEntry.builder()
                    .skill(skillDomain)
                    .frontmatter(frontmatter)
                    .metadata(metadata)
                    .build();

            Optional<String> requiresError = checkRequires(entry);

            SkillRuntimeEntry runtimeEntry = SkillRuntimeEntry.builder()
                    .skillEntry(entry)
                    .workspaceId(workspaceId)
                    .stateChangedAt(LocalDateTime.now())
                    .state(requiresError.isPresent() ? SkillRuntimeState.FAILED : SkillRuntimeState.ACTIVE)
                    .errorMessage(requiresError.orElse(null))
                    .build();

            // 注册到 workspace 分区
            skillRegistry.registerForWorkspace(workspaceId, runtimeEntry);

            log.info("Workspace Skill 加载完成: workspaceId={}, skillName={}, version={}",
                    workspaceId, skill.getName(), version);

        } catch (Exception e) {
            log.error("Workspace Skill 加载失败: workspaceId={}, skillName={}, error={}",
                    workspaceId, skill.getName(), e.getMessage(), e);
        }
    }

    @Override
    public void loadAllForWorkspace(Long workspaceId) {
        log.info("加载 workspace 圈选的 Skill: workspaceId={}", workspaceId);
        List<org.dragon.skill.entity.WorkspaceSkillEntity> bindings =
                workspaceSkillStore.findAllEnabledByWorkspace(workspaceId);

        log.info("加载 workspace 圈选的 Skill: workspaceId={}, count={}",
                workspaceId, bindings.size());

        for (org.dragon.skill.entity.WorkspaceSkillEntity binding : bindings) {
            loadSkillForWorkspace(
                    binding.getSkill(),
                    workspaceId,
                    binding.getPinnedVersion()
            );
        }
    }

    @Override
    public Optional<String> checkRequires(SkillEntry entry) {
        SkillMetadata metadata = entry.getMetadata();
        if (metadata == null || metadata.getRequires() == null) {
            return Optional.empty();
        }

        SkillRequires requires = metadata.getRequires();
        StringBuilder errors = new StringBuilder();

        // 检查 bins
        if (requires.getBins() != null) {
            for (String bin : requires.getBins()) {
                if (!isBinAvailable(bin)) {
                    errors.append("缺少必要的可执行文件: ").append(bin).append("; ");
                }
            }
        }

        // 检查 anyBins
        if (requires.getAnyBins() != null && !requires.getAnyBins().isEmpty()) {
            boolean anyFound = requires.getAnyBins().stream().anyMatch(this::isBinAvailable);
            if (!anyFound) {
                errors.append("anyBins 中没有可用的可执行文件: ")
                        .append(String.join(", ", requires.getAnyBins())).append("; ");
            }
        }

        // 检查 env
        if (requires.getEnv() != null) {
            for (String envVar : requires.getEnv()) {
                String value = System.getenv(envVar);
                if (value == null || value.isBlank()) {
                    errors.append("缺少必要的环境变量: ").append(envVar).append("; ");
                }
            }
        }

        String errorMsg = errors.toString().trim();
        return errorMsg.isEmpty() ? Optional.empty() : Optional.of(errorMsg);
    }

    /**
     * 从 frontmatter 原始 YAML 解析为 Map。
     */
    private Map<String, String> parseFrontmatterRaw(String frontmatterRaw) {
        if (frontmatterRaw == null || frontmatterRaw.isBlank()) {
            return Map.of();
        }
        try {
            return SkillFrontmatterParser.parseFrontmatter("---\n" + frontmatterRaw + "\n---");
        } catch (Exception e) {
            log.warn("解析 frontmatterRaw 失败: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * 反序列化 JSON 字符串为对象。
     */
    private <T> T deserializeObject(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("反序列化失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 反序列化 JSON 字符串为 List。
     */
    private <T> T deserializeList(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.warn("反序列化失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查可执行文件是否存在。
     */
    private boolean isBinAvailable(String bin) {
        // 方式1：通过 which 命令检查
        try {
            ProcessBuilder pb = new ProcessBuilder("which", bin);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) return true;
        } catch (Exception ignored) {
        }

        // 方式2：检查常见路径
        String[] commonPaths = {
                "/usr/bin/" + bin,
                "/usr/local/bin/" + bin,
                "/bin/" + bin,
                "/opt/homebrew/bin/" + bin
        };
        for (String path : commonPaths) {
            if (new java.io.File(path).exists()) return true;
        }

        return false;
    }
}