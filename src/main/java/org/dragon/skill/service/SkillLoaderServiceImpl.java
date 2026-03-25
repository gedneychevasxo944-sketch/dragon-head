package org.dragon.skill.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.skill.entity.SkillEntity;
import org.dragon.skill.exception.SkillLoadException;
import org.dragon.skill.model.Skill;
import org.dragon.skill.model.SkillEntry;
import org.dragon.skill.model.SkillMetadata;
import org.dragon.skill.model.SkillRequires;
import org.dragon.skill.registry.SkillRuntimeEntry;
import org.dragon.skill.registry.SkillRuntimeState;
import org.dragon.skill.registry.SkillRegistry;
import org.dragon.skill.store.SkillStore;
import org.dragon.skill.SkillFrontmatterParser;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private static final String SKILL_FILE = "SKILL.md";

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

        log.info("开始加载 Skill: id={}, name={}, version={}", skillId, skillName, entity.getVersion());

        try {
            // 1. 定位 SKILL.md 文件
            Path skillDir = Paths.get(entity.getSkillDir());
            Path skillMdPath = skillDir.resolve(SKILL_FILE);

            if (!Files.exists(skillMdPath)) {
                throw new SkillLoadException("SKILL.md 文件不存在: " + skillMdPath);
            }

            // 2. 解析 SKILL.md
            String content = new String(Files.readAllBytes(skillMdPath));
            Map<String, String> frontmatter = SkillFrontmatterParser.parseFrontmatter(content);
            String description = frontmatter.getOrDefault("description", "");
            String body = SkillFrontmatterParser.extractBody(content);

            // 3. 构建 Skill 对象
            Skill skill = Skill.builder()
                    .id(skillId)
                    .version(entity.getVersion())
                    .name(skillName)
                    .description(description)
                    .source(entity.getSource())
                    .filePath(skillMdPath.toAbsolutePath().toString())
                    .baseDir(skillDir.toAbsolutePath().toString())
                    .content(body)
                    .build();

            // 4. 解析元数据
            SkillMetadata metadata = SkillFrontmatterParser.resolveMetadata(frontmatter);

            // 5. 构建 SkillEntry
            SkillEntry entry = new SkillEntry(skill, frontmatter, metadata,
                    SkillFrontmatterParser.resolveInvocationPolicy(frontmatter));

            // 6. 依赖检查
            Optional<String> requiresError = checkRequires(entry);

            // 7. 构建 SkillRuntimeEntry
            SkillRuntimeEntry runtimeEntry = SkillRuntimeEntry.builder()
                    .skillEntry(entry)
                    .workspaceId(entity.getWorkspaceId())
                    .stateChangedAt(LocalDateTime.now())
                    .build();

            if (requiresError.isPresent()) {
                String errorMsg = "依赖检查失败: " + requiresError.get();
                log.warn("Skill [{}] 依赖检查失败: {}", skillName, requiresError.get());
                runtimeEntry.setState(SkillRuntimeState.FAILED);
                runtimeEntry.setErrorMessage(errorMsg);
                skillRegistry.register(runtimeEntry);
                return Optional.empty();
            }

            runtimeEntry.setState(SkillRuntimeState.ACTIVE);
            skillRegistry.register(runtimeEntry);

            log.info("Skill [{}] 加载成功，已激活", skillName);
            return Optional.of(runtimeEntry);

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            log.error("Skill [{}] 加载失败: {}", skillName, errorMsg, e);
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
            if (new File(path).exists()) return true;
        }

        return false;
    }
}
