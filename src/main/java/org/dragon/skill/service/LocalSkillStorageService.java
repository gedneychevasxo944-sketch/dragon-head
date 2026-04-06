package org.dragon.skill.service;

import org.dragon.skill.domain.StorageInfoVO;
import org.dragon.skill.exception.SkillValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于本地文件系统的 Skill 文件存储实现（开发 / 测试环境）。
 *
 * <p>激活条件：配置项 {@code skill.storage.type=local}（默认值）。
 *
 * <p>存储路径约定：
 * <pre>
 * {skill.storage.local.base-path}/skills/{skillId}/v{version}/SKILL.md
 * </pre>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "skill.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalSkillStorageService implements SkillStorageService {

    /** 本地存储根目录，默认 /data/skills */
    // TODO [ConfigStore Migration]: 迁移到 ConfigStore GLOBAL scope，使用 ConfigKey.of("skill.storage.local.base-path")
    @Value("${skill.storage.local.base-path:/data/skills}")
    private String localBasePath;

    // ── 公共 API ─────────────────────────────────────────────────────

    @Override
    public StorageInfoVO upload(String skillId, int version, Map<String, byte[]> fileMap) {
        String basePath = resolveBasePath(skillId, version);
        List<StorageInfoVO.SkillFileItem> fileItems = new ArrayList<>();

        Path baseDir = Paths.get(basePath);

        for (Map.Entry<String, byte[]> entry : fileMap.entrySet()) {
            String relativePath = entry.getKey();
            byte[] content      = entry.getValue();

            // 路径遍历防护：校验 relativePath 不能逃逸 basePath
            validateRelativePath(relativePath, baseDir);

            Path targetPath = baseDir.resolve(relativePath).normalize();

            try {
                Files.createDirectories(targetPath.getParent());
                // CREATE_NEW 等价于 O_CREAT|O_EXCL：文件已存在时失败而非覆盖，防覆盖攻击
                Files.write(targetPath, content, StandardOpenOption.CREATE_NEW);
                log.debug("写入本地文件: {}", targetPath);
            } catch (IOException e) {
                throw new SkillValidationException(
                        "文件写入失败: " + relativePath + "，原因: " + e.getMessage());
            }

            StorageInfoVO.SkillFileItem item = new StorageInfoVO.SkillFileItem();
            item.setPath(relativePath);
            item.setSize((long) content.length);
            item.setType(resolveFileType(relativePath));
            fileItems.add(item);
        }

        StorageInfoVO vo = new StorageInfoVO();
        vo.setBucket(null);   // 本地模式无 bucket
        vo.setBasePath(basePath);
        vo.setRootFilePath(basePath + "/SKILL.md");
        vo.setFiles(fileItems);
        return vo;
    }

    @Override
    public byte[] download(StorageInfoVO storageInfo, String relativePath) {
        Path targetPath = Paths.get(storageInfo.getBasePath(), relativePath);
        try {
            return Files.readAllBytes(targetPath);
        } catch (IOException e) {
            throw new SkillValidationException(
                    "文件读取失败: " + relativePath + "，原因: " + e.getMessage());
        }
    }

    // ── 私有工具 ─────────────────────────────────────────────────────

    /**
     * 路径遍历防护：校验 relativePath 规范化后仍在 baseDir 内部。
     *
     * <p>对抗两类攻击：
     * <ol>
     *   <li>绝对路径：{@code /etc/passwd}</li>
     *   <li>目录逃逸：{@code ../../secret.txt}</li>
     * </ol>
     *
     * @param relativePath 待写入的相对路径（来自 fileMap key）
     * @param baseDir      存储根目录（已是绝对路径）
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

    /** 构建本地完整目录路径 */
    private String resolveBasePath(String skillId, int version) {
        return localBasePath + "/skills/" + skillId + "/v" + version;
    }

    /** 根据扩展名推断文件类型标签 */
    private String resolveFileType(String relativePath) {
        String lower = relativePath.toLowerCase();
        if (lower.endsWith(".md"))                              return "markdown";
        if (lower.endsWith(".json"))                            return "json";
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) return "yaml";
        return "text";
    }
}

