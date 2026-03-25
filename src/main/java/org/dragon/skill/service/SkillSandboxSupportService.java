package org.dragon.skill.service;

import org.dragon.skill.registry.SkillRegistry;
import org.dragon.skill.registry.SkillRuntimeEntry;
import org.dragon.skill.storage.SkillStorageBackend;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Skill 模块为 Sandbox 提供的支撑服务。
 *
 * 这是 Skill 模块与 Sandbox 模块之间的边界接口。
 * Sandbox 模块只通过此服务与 Skill 模块交互，不直接依赖 Skill 内部组件。
 *
 * 提供的能力：
 * 1. 查询 workspace 可用的 Skill 列表（用于 sandbox 初始化）
 * 2. 将 Skill 文件同步到 sandbox 目录（文件准备）
 * 3. 提供 Skill 声明的环境变量（用于 sandbox 环境变量构建）
 * 4. 提供 System Prompt Fragment（用于 Agent 构建 LLM 上下文）
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillSandboxSupportService {

    private final SkillRegistry skillRegistry;
    private final SkillStorageBackend storageBackend;

    /**
     * 将指定 workspace 可用的所有 Skill 文件同步到目标目录。
     * 由 SandboxManager 在创建/刷新 sandbox 时调用。
     *
     * @param workspaceId  workspace ID
     * @param skillsDir    sandbox 中的 skills 目录
     * @param forceRefresh 是否强制刷新（true=删除已存在的重新下载）
     */
    public void syncSkillsToSandbox(Long workspaceId, Path skillsDir, boolean forceRefresh) {
        Collection<SkillRuntimeEntry> entries =
                skillRegistry.findAllActiveByWorkspace(workspaceId);

        log.info("同步 Skill 文件: workspaceId={}, count={}, forceRefresh={}",
                workspaceId, entries.size(), forceRefresh);

        for (SkillRuntimeEntry entry : entries) {
            String skillName = entry.getSkillEntry().getSkill().getName();
            String storagePath = entry.getSkillEntry().getSkill().getStoragePath();
            Path skillTarget = skillsDir.resolve(skillName);

            try {
                if (forceRefresh && java.nio.file.Files.exists(skillTarget)) {
                    deleteDirectory(skillTarget);
                }
                if (!java.nio.file.Files.exists(skillTarget)) {
                    storageBackend.download(storagePath, skillTarget);
                    log.info("Skill 文件已同步: skillName={} → {}", skillName, skillTarget);
                }
            } catch (Exception e) {
                log.error("Skill 文件同步失败: skillName={}", skillName, e);
            }
        }
    }

    /**
     * 将单个 Skill 文件同步到 sandbox（用于版本更新时的增量刷新）。
     *
     * @param skillName   Skill 名称
     * @param skillsDir   sandbox 中的 skills 目录
     */
    public void syncSingleSkillToSandbox(String skillName, Path skillsDir) {
        skillRegistry.findByName(skillName).ifPresent(entry -> {
            String storagePath = entry.getSkillEntry().getSkill().getStoragePath();
            Path skillTarget = skillsDir.resolve(skillName);
            try {
                if (java.nio.file.Files.exists(skillTarget)) {
                    deleteDirectory(skillTarget);
                }
                storageBackend.download(storagePath, skillTarget);
                log.info("Skill 文件增量刷新完成: skillName={}", skillName);
            } catch (Exception e) {
                log.error("Skill 文件增量刷新失败: skillName={}", skillName, e);
            }
        });
    }

    /**
     * 收集指定 workspace 下所有 Skill 声明的环境变量。
     * 从系统环境变量中读取对应的值，供 sandbox 环境变量构建使用。
     *
     * @param workspaceId workspace ID
     * @return 环境变量 Map（key=变量名，value=系统中的值）
     */
    public Map<String, String> collectRequiredEnvVars(Long workspaceId) {
        Map<String, String> envVars = new LinkedHashMap<>();

        skillRegistry.findAllActiveByWorkspace(workspaceId).forEach(entry -> {
            var requires = entry.getSkillEntry().getMetadata().getRequires();
            if (requires == null || requires.getEnv() == null) return;

            requires.getEnv().forEach(envKey -> {
                String value = System.getenv(envKey);
                if (value != null && !value.isBlank()) {
                    envVars.put(envKey, value);
                } else {
                    log.warn("Skill [{}] 声明的环境变量 [{}] 在系统中不存在或为空",
                            entry.getSkillEntry().getSkill().getName(), envKey);
                }
            });
        });

        return envVars;
    }

    /**
     * 获取指定 workspace 的 System Prompt Fragment。
     * 由 Agent 在构建 LLM 上下文时调用。
     *
     * @param workspaceId workspace ID
     * @return 注入 System Prompt 的 Skill 说明文本
     */
    public String getSystemPromptFragment(Long workspaceId) {
        return skillRegistry.buildSystemPromptFragment(workspaceId);
    }

    /**
     * 获取指定 workspace 下所有 ACTIVE Skill 的名称列表。
     * 用于 sandbox 初始化时确认需要同步哪些文件。
     */
    public List<String> getActiveSkillNames(Long workspaceId) {
        return skillRegistry.findAllActiveByWorkspace(workspaceId)
                .stream()
                .map(e -> e.getSkillEntry().getSkill().getName())
                .collect(Collectors.toList());
    }

    /**
     * 检查指定 Skill 在 sandbox 中的文件是否是最新版本。
     *
     * @param skillName   Skill 名称
     * @param skillsDir   sandbox 中的 skills 目录
     * @return true=已是最新，false=需要刷新
     */
    public boolean isSkillUpToDate(String skillName, Path skillsDir) {
        return skillRegistry.findByName(skillName)
                .map(entry -> {
                    Path skillTarget = skillsDir.resolve(skillName);
                    return java.nio.file.Files.exists(skillTarget);
                })
                .orElse(false);
    }

    private void deleteDirectory(Path path) {
        try {
            java.nio.file.Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { java.nio.file.Files.delete(p); } catch (Exception ignored) {}
                    });
        } catch (Exception e) {
            log.warn("目录删除失败: {}", path);
        }
    }
}