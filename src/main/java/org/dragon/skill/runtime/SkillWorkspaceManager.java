package org.dragon.skill.runtime;

import org.dragon.skill.domain.StorageInfoVO;
import org.dragon.skill.exception.SkillValidationException;
import org.dragon.skill.service.SkillStorageService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * Skill 工作区管理器（两层设计：只读模板层 + 独立执行层）。
 *
 * <h3>两层设计</h3>
 * <pre>
 * 【模板层（只读缓存）】
 *   路径：{skill.workspace.template-dir}/{skillId}/v{version}/
 *   生命周期：进程级，TTL 自动过期或 Skill 失效时驱逐
 *   共享：进程内所有 Character 共享同一份，永不修改
 *   下载：同一版本只下载一次（双重检查 + 版本维度锁）
 *
 * 【执行层（独立工作目录）】
 *   路径：{skill.workspace.exec-dir}/{execId}/
 *   生命周期：单次执行，执行结束后立即删除
 *   共享：每次执行独占，沙箱只挂载此目录，互不干扰
 *   创建：从模板层复制（文件拷贝，非硬链接，保证修改隔离）
 * </pre>
 *
 * <h3>并发安全</h3>
 * <p>模板层首次物化时，按 {@code skillId:version} 维度加锁（每个版本独立锁），
 * 双重检查保证只下载一次，无文件竞争。
 * 执行层每次生成 UUID 路径，天然无竞争。
 */
@Slf4j
@Component
public class SkillWorkspaceManager {

    /** 模板目录根路径，存放只读的 Skill 包文件 */
    // TODO [ConfigStore Migration]: 迁移到 ConfigStore GLOBAL scope，使用 ConfigKey.of("skill.workspace.template-dir")
    @Value("${skill.workspace.template-dir:/var/skill-cache}")
    private String templateBaseDir;

    /** 执行目录根路径，存放每次执行的独立工作目录 */
    // TODO [ConfigStore Migration]: 迁移到 ConfigStore GLOBAL scope，使用 ConfigKey.of("skill.workspace.exec-dir")
    @Value("${skill.workspace.exec-dir:/tmp/skill-exec}")
    private String execBaseDir;

    @Autowired
    private SkillStorageService storageService;

    /**
     * 模板层 Caffeine 缓存：key = "skillId:version" → 模板目录绝对路径。
     * TTL=30分钟不访问自动过期；过期时通过 RemovalListener 异步删除磁盘目录。
     */
    private Cache<String, Path> templateCache;

    /**
     * 按版本维度的物化锁：key = "skillId:version" → ReentrantLock。
     * 保证同一版本同时只有一个线程执行下载，其余线程等待后直接命中缓存。
     */
    private final Map<String, ReentrantLock> materializeLocks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        templateCache = Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .maximumSize(500)
                .removalListener((RemovalListener<String, Path>) (key, templateDir, cause) -> {
                    if (templateDir != null) {
                        log.info("[SkillWorkspace] 模板目录过期清理: key={}, cause={}", key, cause);
                        deleteDirectory(templateDir);
                    }
                    if (key != null) {
                        materializeLocks.remove(key);
                    }
                })
                .build();

        // 注册 JVM shutdown hook，清理残留执行目录
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("[SkillWorkspace] JVM 关闭，清理残留目录...");
            cleanupExecBaseDir();
            cleanupTemplateBaseDir();
        }, "skill-workspace-cleanup"));

        log.info("[SkillWorkspaceManager] 初始化完成，templateDir={}, execDir={}",
                templateBaseDir, execBaseDir);
    }

    @PreDestroy
    public void destroy() {
        templateCache.invalidateAll();
    }

    // ── 核心公共 API ─────────────────────────────────────────────────

    /**
     * 为一次 Skill 执行准备独立的工作目录。
     *
     * <p>流程：
     * <ol>
     *   <li>检查模板层缓存，未命中则下载物化（加锁，双重检查）</li>
     *   <li>从模板目录复制文件，创建本次执行的独立工作目录</li>
     * </ol>
     *
     * @param skillId     Skill 业务 UUID
     * @param version     版本号
     * @param storageInfo 存储元信息（含文件列表）
     * @return 本次执行的工作目录路径（调用方执行结束后调用 {@link #releaseExecDir} 清理）
     */
    public Path prepareExecDir(String skillId, int version, StorageInfoVO storageInfo) {
        // 1. 获取或物化模板目录（只读，进程级共享）
        Path templateDir = getOrMaterializeTemplate(skillId, version, storageInfo);

        // 2. 从模板复制，创建本次执行专属工作目录
        String execId = skillId + "-v" + version + "-" + UUID.randomUUID().toString().substring(0, 8);
        Path execDir = Paths.get(execBaseDir, execId);

        try {
            copyDirectory(templateDir, execDir);
            log.debug("[SkillWorkspace] 执行目录已创建: {}", execDir);
            return execDir;
        } catch (IOException e) {
            throw new SkillValidationException(
                    "创建 Skill 执行工作目录失败: skillId=" + skillId + ", 原因: " + e.getMessage());
        }
    }

    /**
     * 释放（删除）执行工作目录。
     * 执行结束后由 SkillExecutor 在 finally 块中调用。
     *
     * @param execDir {@link #prepareExecDir} 返回的路径
     */
    public void releaseExecDir(Path execDir) {
        if (execDir == null) return;
        deleteDirectory(execDir);
        log.debug("[SkillWorkspace] 执行目录已释放: {}", execDir);
    }

    /**
     * 主动驱逐指定版本的模板缓存（Skill 删除/禁用时调用）。
     * Caffeine RemovalListener 会异步触发磁盘目录删除。
     */
    public void evictTemplate(String skillId, int version) {
        String key = buildKey(skillId, version);
        templateCache.invalidate(key);
        log.info("[SkillWorkspace] 模板缓存已驱逐: key={}", key);
    }

    /**
     * 判断一个 Skill 是否需要物化工作目录（有附属文件时才需要）。
     * 若只有 SKILL.md 一个文件，直接读文本即可，无需物化目录。
     *
     * @param storageInfo 存储元信息
     * @return true = 需要物化
     */
    public boolean needsMaterialization(StorageInfoVO storageInfo) {
        if (storageInfo == null) return false;
        List<StorageInfoVO.SkillFileItem> files = storageInfo.getFiles();
        // 有超过 1 个文件（即除了 SKILL.md 还有其他文件）才需要物化目录
        return files != null && files.size() > 1;
    }

    // ── 模板层：获取或物化 ───────────────────────────────────────────

    private Path getOrMaterializeTemplate(String skillId, int version, StorageInfoVO storageInfo) {
        String key = buildKey(skillId, version);

        // 快速路径：缓存命中，直接返回
        Path cached = templateCache.getIfPresent(key);
        if (cached != null && Files.exists(cached)) {
            log.debug("[SkillWorkspace] 模板缓存命中: key={}", key);
            return cached;
        }

        // 慢速路径：获取该版本的专属锁，防止并发重复下载
        ReentrantLock lock = materializeLocks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            // 双重检查：拿到锁后再查一次缓存
            cached = templateCache.getIfPresent(key);
            if (cached != null && Files.exists(cached)) {
                log.debug("[SkillWorkspace] 模板缓存命中（double-check）: key={}", key);
                return cached;
            }

            // 真正执行物化
            Path templateDir = materializeTemplate(skillId, version, storageInfo);
            templateCache.put(key, templateDir);
            log.info("[SkillWorkspace] 模板物化完成: key={}, dir={}", key, templateDir);
            return templateDir;

        } finally {
            lock.unlock();
        }
    }

    /**
     * 将 storageInfo 中所有文件下载到模板目录，还原原始目录结构。
     * 模板目录设置为只读（防止意外修改影响其他执行）。
     */
    private Path materializeTemplate(String skillId, int version, StorageInfoVO storageInfo) {
        Path templateDir = Paths.get(templateBaseDir, skillId, "v" + version);

        try {
            // 如果目录已存在（进程重启后残留），先删除重建，保证内容新鲜
            if (Files.exists(templateDir)) {
                deleteDirectory(templateDir);
            }
            Files.createDirectories(templateDir);

            List<StorageInfoVO.SkillFileItem> files = storageInfo.getFiles();
            if (files != null) {
                for (StorageInfoVO.SkillFileItem fileItem : files) {
                    // 路径遍历防护：校验 fileItem.getPath() 不能逃逸 templateDir
                    validateRelativePath(fileItem.getPath(), templateDir);

                    byte[] content = storageService.download(storageInfo, fileItem.getPath());
                    Path targetPath = templateDir.resolve(fileItem.getPath()).normalize();
                    Files.createDirectories(targetPath.getParent());
                    // CREATE_NEW 等价于 O_CREAT|O_EXCL：已存在时失败而非覆盖
                    Files.write(targetPath, content, StandardOpenOption.CREATE_NEW);
                    log.trace("[SkillWorkspace] 模板文件已写入: {}", targetPath);
                }
            }

            // 模板目录设为只读，防止执行层污染
            setReadOnly(templateDir);

            return templateDir;

        } catch (IOException e) {
            // 物化失败时清理不完整的目录
            deleteDirectory(templateDir);
            throw new SkillValidationException(
                    "Skill 模板物化失败: skillId=" + skillId + ", version=" + version
                            + ", 原因: " + e.getMessage());
        }
    }

    // ── 执行层：目录复制 ─────────────────────────────────────────────

    /**
     * 将模板目录递归复制到目标执行目录（文件复制，非硬链接）。
     * 使用文件复制而非硬链接，保证执行层对文件的修改不影响模板层。
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(src -> {
                Path dest = target.resolve(source.relativize(src));
                try {
                    if (Files.isDirectory(src)) {
                        Files.createDirectories(dest);
                    } else {
                        // 复制文件，同时设置为可写（模板是只读的，执行层需要可写）
                        Files.copy(src, dest);
                        dest.toFile().setWritable(true);
                    }
                } catch (IOException e) {
                    throw new SkillValidationException("复制文件失败: " + src + " → " + dest);
                }
            });
        }
    }

    // ── 安全工具 ─────────────────────────────────────────────────────

    /**
     * 路径遍历防护：校验 relativePath 规范化后仍在 baseDir 内部。
     *
     * <p>对抗两类攻击：
     * <ol>
     *   <li>绝对路径：{@code /etc/passwd}</li>
     *   <li>目录逃逸：{@code ../../secret.txt}</li>
     * </ol>
     *
     * @param relativePath 待写入的相对路径（来自 StorageInfoVO.fileItem.path）
     * @param baseDir      模板根目录（已是绝对路径）
     */
    private void validateRelativePath(String relativePath, Path baseDir) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new SkillValidationException("文件路径不能为空");
        }
        Path normalized = baseDir.resolve(relativePath).normalize();
        if (!normalized.startsWith(baseDir.normalize())) {
            throw new SkillValidationException("非法文件路径（路径遍历）: " + relativePath);
        }
    }

    // ── 目录权限与清理工具 ───────────────────────────────────────────

    /**
     * 递归设置目录及其所有文件为只读。
     * 仅在支持 POSIX 权限的系统上生效（Linux/macOS），Windows 跳过。
     */
    private void setReadOnly(Path dir) {
        try {
            Set<PosixFilePermission> readOnly = EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_EXECUTE,   // 目录需要 execute 权限才能进入
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE
            );
            try (Stream<Path> stream = Files.walk(dir)) {
                stream.forEach(p -> {
                    try {
                        if (Files.isDirectory(p)) {
                            Files.setPosixFilePermissions(p, EnumSet.of(
                                    PosixFilePermission.OWNER_READ,
                                    PosixFilePermission.OWNER_EXECUTE));
                        } else {
                            Files.setPosixFilePermissions(p, EnumSet.of(
                                    PosixFilePermission.OWNER_READ));
                        }
                    } catch (UnsupportedOperationException ignored) {
                        // Windows 不支持 POSIX 权限，跳过
                    } catch (IOException e) {
                        log.warn("[SkillWorkspace] 设置只读权限失败: {}", p);
                    }
                });
            }
        } catch (IOException e) {
            log.warn("[SkillWorkspace] 遍历目录设置只读失败: {}", dir);
        }
    }

    /** 递归删除目录（忽略不存在的情况） */
    private void deleteDirectory(Path dir) {
        if (dir == null || !Files.exists(dir)) return;
        try (Stream<Path> stream = Files.walk(dir)) {
            // 先恢复写权限（避免只读目录删除失败）
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    p.toFile().setWritable(true);
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    log.warn("[SkillWorkspace] 删除文件失败: {}", p);
                }
            });
        } catch (IOException e) {
            log.warn("[SkillWorkspace] 删除目录失败: {}, 原因: {}", dir, e.getMessage());
        }
    }

    private void cleanupExecBaseDir() {
        Path base = Paths.get(execBaseDir);
        if (!Files.exists(base)) return;
        try (Stream<Path> stream = Files.list(base)) {
            stream.forEach(this::deleteDirectory);
        } catch (IOException e) {
            log.warn("[SkillWorkspace] 清理执行目录失败: {}", e.getMessage());
        }
    }

    private void cleanupTemplateBaseDir() {
        templateCache.invalidateAll();
    }

    private String buildKey(String skillId, int version) {
        return skillId + ":" + version;
    }
}

