package org.dragon.skill.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.skill.registry.SkillRuntimeEntry;
import org.dragon.skill.registry.SkillRegistry;
import org.dragon.skill.storage.SkillStorageBackend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Skill 执行环境文件准备服务。
 *
 * 职责：在 Agent 实际执行某个 Skill 之前，
 * 将该 Skill 的完整文件（脚本、数据等）从存储后端下载到执行环境的本地目录。
 *
 * 调用时机：Agent 判断需要执行某个 Skill 时，在实际执行前调用。
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillExecutionPrepareService {

    private final SkillStorageBackend storageBackend;
    private final SkillRegistry skillRegistry;

    /**
     * 执行环境的 Skill 工作目录根路径。
     * 例：/tmp/agent-sandbox/skills/
     */
    @Value("${skill.execution.sandbox-dir:/tmp/agent-sandbox/skills}")
    private String sandboxDir;

    /**
     * 为指定 Skill 准备执行环境。
     * 将 Skill 文件从存储后端下载到 sandbox 目录。
     *
     * @param skillName   Skill 名称
     * @param executionId 本次执行的唯一 ID（用于隔离不同执行实例的目录）
     * @return Skill 在执行环境中的本地根目录路径
     */
    public Path prepareForExecution(String skillName, String executionId) {
        SkillRuntimeEntry runtimeEntry = skillRegistry.findByName(skillName)
                .orElseThrow(() -> new IllegalArgumentException("Skill 未加载: " + skillName));

        String storagePath = runtimeEntry.getSkillEntry().getSkill().getStoragePath();

        // 执行目录：{sandboxDir}/{executionId}/{skillName}/
        Path localTarget = Paths.get(sandboxDir, executionId, skillName);

        log.info("准备 Skill 执行环境: skillName={}, executionId={}, target={}",
                skillName, executionId, localTarget);

        storageBackend.download(storagePath, localTarget);

        log.info("Skill 执行环境准备完成: {}", localTarget);
        return localTarget;
    }

    /**
     * 清理执行完成后的临时目录。
     *
     * @param executionId 执行 ID
     */
    public void cleanupAfterExecution(String executionId) {
        Path executionDir = Paths.get(sandboxDir, executionId);
        if (!executionDir.toFile().exists()) {
            log.info("执行目录不存在，无需清理: {}", executionDir);
            return;
        }

        try {
            java.nio.file.Files.walk(executionDir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try { java.nio.file.Files.delete(p); } catch (Exception ignored) {}
                    });
            log.info("Skill 执行环境已清理: executionId={}", executionId);
        } catch (Exception e) {
            log.warn("清理执行目录失败: executionId={}", executionId, e);
        }
    }
}