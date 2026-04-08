package org.dragon.sandbox.manager;

import org.dragon.sandbox.SandboxException;
import org.dragon.sandbox.domain.Sandbox;
import org.dragon.sandbox.domain.SandboxState;
import org.dragon.skill.runtime.SkillRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sandbox 生命周期管理器。
 *
 * 职责：
 * 1. 为每个 workspace 创建和维护 sandbox 目录结构
 * 2. 将该 workspace 可用的 Skill 文件同步到 sandbox
 * 3. 管理 sandbox 级别的环境变量
 * 4. 提供 sandbox 的查询和销毁能力
 *
 * 设计原则：
 * - 每个 workspace 只有一个 sandbox 实例（单例）
 * - sandbox 在 workspace 首次被 agent 使用时懒加载创建
 * - sandbox 中的 skill 文件在 skill 版本更新时自动同步
 *
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SandboxManager {

    // private final SkillRegistry skillRegistry;

    // @Value("${sandbox.root-dir:/tmp/agent-sandbox}")
    // private String sandboxRootDir;

    // /**
    //  * 已创建的 sandbox 实例缓存。
    //  * Key: workspaceId
    //  */
    // private final ConcurrentHashMap<Long, Sandbox> sandboxCache = new ConcurrentHashMap<>();

    // /**
    //  * 获取或创建指定 workspace 的 sandbox。
    //  * 若 sandbox 不存在则自动创建并初始化。
    //  *
    //  * @param workspaceId workspace ID
    //  * @return 就绪状态的 Sandbox
    //  */
    // public Sandbox getOrCreate(Long workspaceId) {
    //     return sandboxCache.computeIfAbsent(workspaceId, this::createSandbox);
    // }

    // /**
    //  * 创建并初始化 sandbox。
    //  */
    // private Sandbox createSandbox(Long workspaceId) {
    //     log.info("创建 Sandbox: workspaceId={}", workspaceId);

    //     Path rootDir    = Paths.get(sandboxRootDir, "ws-" + workspaceId);
    //     Path skillsDir  = rootDir.resolve("skills");
    //     Path workDir    = rootDir.resolve("workspace");
    //     Path tmpDir     = rootDir.resolve("tmp");

    //     try {
    //         // 创建目录结构
    //         Files.createDirectories(skillsDir);
    //         Files.createDirectories(workDir);
    //         Files.createDirectories(tmpDir);
    //     } catch (IOException e) {
    //         throw new SandboxException("Sandbox 目录创建失败: workspaceId=" + workspaceId, e);
    //     }

    //     // 同步该 workspace 可用的所有 Skill 文件
    //     syncSkillFiles(workspaceId, skillsDir);

    //     // 构建 sandbox 环境变量
    //     Map<String, String> envVars = buildEnvironmentVariables(workspaceId, rootDir);

    //     Sandbox sandbox = Sandbox.builder()
    //             .workspaceId(workspaceId)
    //             .rootDir(rootDir)
    //             .skillsDir(skillsDir)
    //             .workspaceDir(workDir)
    //             .tmpDir(tmpDir)
    //             .environmentVariables(envVars)
    //             .createdAt(LocalDateTime.now())
    //             .state(SandboxState.READY)
    //             .build();

    //     log.info("Sandbox 创建完成: workspaceId={}, rootDir={}", workspaceId, rootDir);
    //     return sandbox;
    // }

    // /**
    //  * 将指定 workspace 可用的所有 Skill 文件同步到 sandbox。
    //  * 包含：workspace 专属 Skill + 所有内置 Skill（workspaceId=0）。
    //  */
    // public void syncSkillFiles(Long workspaceId, Path skillsDir) {
    //     Collection<SkillRuntimeEntry> entries =
    //             skillRegistry.findAllActiveByWorkspace(workspaceId);

    //     log.info("同步 Skill 文件到 Sandbox: workspaceId={}, skillCount={}",
    //             workspaceId, entries.size());

    //     for (SkillRuntimeEntry entry : entries) {
    //         String skillName = entry.getSkillEntry().getSkill().getName();
    //         String storagePath = entry.getSkillEntry().getSkill().getStoragePath();
    //         Path skillTarget = skillsDir.resolve(skillName);

    //         try {
    //             // 若已存在，跳过同步
    //             if (Files.exists(skillTarget)) {
    //                 log.debug("Skill 文件已存在，跳过同步: {}", skillName);
    //                 continue;
    //             }
    //             storageBackend.download(storagePath, skillTarget);
    //             log.info("Skill 文件已同步: skillName={}", skillName);
    //         } catch (Exception e) {
    //             // 单个 Skill 同步失败不影响其他 Skill
    //             log.error("Skill 文件同步失败: skillName={}, error={}", skillName, e.getMessage());
    //         }
    //     }
    // }

    // /**
    //  * 当某个 Skill 版本更新时，刷新该 Skill 在所有相关 sandbox 中的文件。
    //  * 由 SkillChangeListener 调用。
    //  *
    //  * @param skillName   更新的 Skill 名称
    //  * @param workspaceId 归属的 workspace ID（0 表示内置，需刷新所有 sandbox）
    //  */
    // public void refreshSkillInSandboxes(String skillName, Long workspaceId) {
    //     if (workspaceId == 0L) {
    //         // 内置 Skill 更新，需刷新所有已创建的 sandbox
    //         sandboxCache.forEach((wsId, sandbox) -> {
    //             refreshSkillInSandbox(sandbox, skillName);
    //         });
    //     } else {
    //         // workspace 专属 Skill，只刷新对应 sandbox
    //         Sandbox sandbox = sandboxCache.get(workspaceId);
    //         if (sandbox != null) {
    //             refreshSkillInSandbox(sandbox, skillName);
    //         }
    //     }
    // }

    // /**
    //  * 刷新单个 sandbox 中指定 Skill 的文件。
    //  */
    // private void refreshSkillInSandbox(Sandbox sandbox, String skillName) {
    //     Path skillTarget = sandbox.getSkillsDir().resolve(skillName);
    //     try {
    //         // 删除旧版本
    //         if (Files.exists(skillTarget)) {
    //             deleteDirectory(skillTarget);
    //         }
    //         // 重新下载
    //         SkillRuntimeEntry entry = skillRegistry.findByName(skillName).orElse(null);
    //         if (entry != null) {
    //             String storagePath = entry.getSkillEntry().getSkill().getStoragePath();
    //             storageBackend.download(storagePath, skillTarget);
    //             log.info("Sandbox Skill 文件已刷新: workspaceId={}, skillName={}",
    //                     sandbox.getWorkspaceId(), skillName);
    //         }
    //     } catch (Exception e) {
    //         log.error("Sandbox Skill 文件刷新失败: skillName={}, error={}", skillName, e.getMessage());
    //     }
    // }

    // /**
    //  * 构建 sandbox 级别的环境变量。
    //  *
    //  * 包含以下来源（优先级从低到高）：
    //  * 1. 系统默认环境变量（PATH 等）
    //  * 2. Skill 声明的 requires.env 中定义的变量（从系统环境变量中继承）
    //  * 3. sandbox 内置变量（SANDBOX_ROOT、SKILLS_DIR 等路径变量）
    //  */
    // private Map<String, String> buildEnvironmentVariables(String workspaceId, Path rootDir) {
    //     Map<String, String> envVars = new LinkedHashMap<>();

    //     // 1. 继承当前进程的 PATH
    //     String systemPath = System.getenv("PATH");
    //     if (systemPath != null) {
    //         envVars.put("PATH", systemPath);
    //     }

    //     // 2. 收集该 workspace 下所有 Skill 声明的 env 变量
    //     skillRegistry.getSkills(null, workspaceId).forEach(entry -> {
    //         var requires = entry.getSkillEntry().getMetadata().getRequires();
    //         if (requires != null && requires.getEnv() != null) {
    //             requires.getEnv().forEach(envKey -> {
    //                 String value = System.getenv(envKey);
    //                 if (value != null) {
    //                     envVars.put(envKey, value);
    //                 } else {
    //                     log.warn("Skill 声明的环境变量在系统中不存在: {}", envKey);
    //                 }
    //             });
    //         }
    //     });

    //     // 3. sandbox 内置路径变量（供 Skill 脚本引用）
    //     envVars.put("SANDBOX_ROOT",  rootDir.toAbsolutePath().toString());
    //     envVars.put("SANDBOX_SKILLS_DIR", rootDir.resolve("skills").toAbsolutePath().toString());
    //     envVars.put("SANDBOX_WORKSPACE_DIR", rootDir.resolve("workspace").toAbsolutePath().toString());
    //     envVars.put("WORKSPACE_ID", String.valueOf(workspaceId));

    //     return envVars;
    // }

    // /**
    //  * 销毁指定 workspace 的 sandbox（清理文件和缓存）。
    //  */
    // public void destroy(Long workspaceId) {
    //     Sandbox sandbox = sandboxCache.remove(workspaceId);
    //     if (sandbox == null) return;

    //     sandbox.setState(SandboxState.DESTROYED);
    //     try {
    //         deleteDirectory(sandbox.getRootDir());
    //         log.info("Sandbox 已销毁: workspaceId={}", workspaceId);
    //     } catch (Exception e) {
    //         log.error("Sandbox 销毁失败: workspaceId={}", workspaceId, e);
    //     }
    // }

    // /**
    //  * 获取指定 workspace 的 sandbox（不创建）。
    //  */
    // public Optional<Sandbox> get(Long workspaceId) {
    //     return Optional.ofNullable(sandboxCache.get(workspaceId));
    // }

    // /**
    //  * 获取所有已创建的 sandbox 数量（用于监控）。
    //  */
    // public int activeSandboxCount() {
    //     return sandboxCache.size();
    // }

    // private void deleteDirectory(Path path) throws IOException {
    //     if (!Files.exists(path)) return;
    //     Files.walk(path)
    //             .sorted(Comparator.reverseOrder())
    //             .forEach(p -> {
    //                 try { Files.delete(p); } catch (IOException ignored) {}
    //             });
    // }
}