package org.dragon.skill.validator;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dragon.skill.exception.SkillValidationException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Skill ZIP 包校验器。
 *
 * @since 1.0
 */
@Slf4j
@Component
public class SkillZipValidator {

    private static final String SKILL_MD_FILENAME = "SKILL.md";
    private static final long MAX_ZIP_SIZE = 50 * 1024 * 1024; // 50MB

    /**
     * ZIP 校验结果封装。
     */
    @Data
    @Builder
    public static class ZipValidationResult {
        /** 校验通过后提取的 skill name */
        private String skillName;
        /** SKILL.md 完整原始内容（供后续解析使用，避免重复读取 ZIP） */
        private String skillMdRawContent;
    }

    /**
     * 校验 ZIP 包并提取 SKILL.md 原始内容。
     * 将校验和内容提取合并为一次 ZIP 读取，避免重复 IO。
     *
     * @param file 上传的 ZIP 文件
     * @return 校验结果（包含 skillName 和 SKILL.md 原始内容）
     */
    public ZipValidationResult validateAndExtract(MultipartFile file) {
        // 1. 基础校验
        if (file == null || file.isEmpty()) {
            throw new SkillValidationException("上传文件不能为空");
        }
        if (file.getSize() > MAX_ZIP_SIZE) {
            throw new SkillValidationException("ZIP 包大小不能超过 50MB，当前大小: " + file.getSize() + " bytes");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new SkillValidationException("仅支持上传 .zip 格式的文件，当前文件名: " + originalFilename);
        }

        // 提取 zip 包名
        String zipBaseName = originalFilename.substring(0, originalFilename.length() - 4);

        // 2. 读取 ZIP 内容，查找 SKILL.md
        String skillMdContent = extractSkillMdContent(file, zipBaseName);

        // 3. 解析 frontmatter，提取 name 和 description
        String skillName = validateFrontmatter(skillMdContent, zipBaseName);

        log.info("ZIP 包校验通过，skill name: {}, zip 文件: {}", skillName, originalFilename);

        return ZipValidationResult.builder()
                .skillName(skillName)
                .skillMdRawContent(skillMdContent)
                .build();
    }

    /**
     * 校验 ZIP 包并提取 frontmatter 中的 name（兼容旧接口）。
     *
     * @param file 上传的 ZIP 文件
     * @return 校验通过后提取的 skill name
     */
    public String validateAndExtractName(MultipartFile file) {
        return validateAndExtract(file).getSkillName();
    }

    /**
     * 从 ZIP 包中提取 SKILL.md 内容。
     */
    private String extractSkillMdContent(MultipartFile file, String zipBaseName) {
        String standardPath = zipBaseName + "/" + SKILL_MD_FILENAME;
        String rootPath = SKILL_MD_FILENAME;

        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(file.getInputStream()), StandardCharsets.UTF_8)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                if (!entry.isDirectory() &&
                        (entryName.equals(standardPath) || entryName.equals(rootPath) ||
                         entryName.equalsIgnoreCase(standardPath) || entryName.equalsIgnoreCase(rootPath))) {

                    byte[] buffer = new byte[1024];
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int len;
                    long totalRead = 0;
                    long maxSkillMdSize = 1024 * 1024; // 1MB

                    while ((len = zis.read(buffer)) > 0) {
                        totalRead += len;
                        if (totalRead > maxSkillMdSize) {
                            throw new SkillValidationException("SKILL.md 文件过大，超过 1MB 限制");
                        }
                        baos.write(buffer, 0, len);
                    }
                    return baos.toString(StandardCharsets.UTF_8);
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new SkillValidationException("ZIP 包读取失败: " + e.getMessage(), e);
        }

        throw new SkillValidationException(
                "ZIP 包中未找到 SKILL.md 文件。期望路径: " + standardPath + " 或 " + rootPath);
    }

    /**
     * 校验 SKILL.md frontmatter，提取并验证 name 字段。
     */
    private String validateFrontmatter(String skillMdContent, String zipBaseName) {
        if (!skillMdContent.startsWith("---")) {
            throw new SkillValidationException("SKILL.md 缺少 frontmatter（文件必须以 --- 开头）");
        }

        int endIndex = skillMdContent.indexOf("\n---", 3);
        if (endIndex == -1) {
            throw new SkillValidationException("SKILL.md frontmatter 格式错误（未找到结束的 ---）");
        }

        String yamlContent = skillMdContent.substring(4, endIndex).trim();

        Yaml yaml = new Yaml();
        Map<String, Object> frontmatter;
        try {
            frontmatter = yaml.load(yamlContent);
        } catch (Exception e) {
            throw new SkillValidationException("SKILL.md frontmatter YAML 解析失败: " + e.getMessage(), e);
        }

        if (frontmatter == null || frontmatter.isEmpty()) {
            throw new SkillValidationException("SKILL.md frontmatter 为空");
        }

        // 校验 name 字段
        Object nameObj = frontmatter.get("name");
        if (nameObj == null || nameObj.toString().isBlank()) {
            throw new SkillValidationException("SKILL.md frontmatter 缺少 name 字段");
        }
        String name = nameObj.toString().trim();

        // 校验 description 字段
        Object descObj = frontmatter.get("description");
        if (descObj == null || descObj.toString().isBlank()) {
            throw new SkillValidationException("SKILL.md frontmatter 缺少 description 字段");
        }

        // 校验 zip 包名与 name 一致性
        if (!zipBaseName.equals(name)) {
            throw new SkillValidationException(
                    String.format("ZIP 包名 '%s' 与 SKILL.md frontmatter 中的 name '%s' 不一致", zipBaseName, name));
        }

        return name;
    }
}