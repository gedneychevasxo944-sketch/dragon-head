package org.dragon.skill.storage;

import lombok.extern.slf4j.Slf4j;
import org.dragon.skill.exception.SkillStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 本地文件系统存储后端实现。
 * 作为 S3 的临时替代方案，存储结构与 S3 路径规范保持一致，
 * 便于后续无缝切换。
 *
 * 本地存储路径：{rootDir}/{workspaceId}/{skillName}/{version}/
 * 例：/data/skills/0/weather-skill/3/
 *
 * @since 1.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "skill.storage.backend", havingValue = "local", matchIfMissing = true)
public class LocalFileSystemStorageBackend implements SkillStorageBackend {

    @Value("${skill.storage.local.root-dir:/data/skills}")
    private String rootDir;

    @Override
    public String store(long workspaceId, String skillName, int version, InputStream zipStream) {
        // 本地路径：{rootDir}/{workspaceId}/{skillName}/{version}/
        Path targetPath = Paths.get(rootDir, String.valueOf(workspaceId), skillName, String.valueOf(version));

        try {
            Files.createDirectories(targetPath);
            extractZip(zipStream, targetPath);
            String storagePath = targetPath.toAbsolutePath().toString();
            log.info("Skill 文件已存储到本地: {}", storagePath);
            return storagePath;
        } catch (IOException e) {
            throw new SkillStorageException("Skill 文件存储失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void download(String storagePath, Path localTarget) {
        // 本地实现：直接复制目录（storagePath 本身就是本地路径）
        Path source = Paths.get(storagePath);
        if (!Files.exists(source)) {
            throw new SkillStorageException("存储路径不存在: " + storagePath);
        }
        try {
            copyDirectory(source, localTarget);
            log.info("Skill 文件已复制到执行目录: {} -> {}", storagePath, localTarget);
        } catch (IOException e) {
            throw new SkillStorageException("Skill 文件复制失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream readFile(String storagePath, String relativePath) {
        Path filePath = Paths.get(storagePath, relativePath);
        try {
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            throw new SkillStorageException("读取文件失败: " + filePath, e);
        }
    }

    @Override
    public void delete(String storagePath) {
        Path path = Paths.get(storagePath);
        if (!Files.exists(path)) return;
        try {
            deleteDirectory(path);
            log.info("Skill 文件已删除: {}", storagePath);
        } catch (IOException e) {
            throw new SkillStorageException("删除文件失败: " + storagePath, e);
        }
    }

    @Override
    public boolean exists(String storagePath) {
        return Files.exists(Paths.get(storagePath));
    }

    @Override
    public String backendType() {
        return "LOCAL_FILESYSTEM";
    }

    private void extractZip(InputStream zipStream, Path targetPath) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(zipStream))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetPath.resolve(entry.getName()).normalize();
                // Zip Slip 安全检查
                if (!entryPath.startsWith(targetPath)) {
                    throw new SkillStorageException("ZIP 包含非法路径: " + entry.getName());
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
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        Files.walk(source).forEach(src -> {
            try {
                Path dest = target.resolve(source.relativize(src));
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private void deleteDirectory(Path path) throws IOException {
        Files.walk(path)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> {
                    try { Files.delete(p); } catch (IOException ignored) {}
                });
    }
}