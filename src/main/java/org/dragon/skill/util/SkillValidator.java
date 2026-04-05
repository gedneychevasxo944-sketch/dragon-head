package org.dragon.skill.util;

import org.dragon.skill.domain.ParsedSkillContent;
import org.dragon.skill.exception.SkillValidationException;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Skill 文件与内容校验工具类（无状态，全静态方法）。
 *
 * 校验规则：
 * ┌─────────────────────────────────────────────────────────┐
 * │ 文件级别                                                 │
 * │  - 单文件 ≤ 2MB                                         │
 * │  - ZIP 包本身 ≤ 10MB                                    │
 * │  - 解压后总大小 ≤ 50MB，文件数 ≤ 100                   │
 * │  - 扩展名白名单：.md .txt .json .yaml .yml              │
 * │  - 拒绝危险扩展名：.sh .exe .py .js .ts .rb .php 等    │
 * │  - 拒绝路径穿越（../）和软链接                          │
 * ├─────────────────────────────────────────────────────────┤
 * │ SKILL.md 内容级别                                        │
 * │  - frontmatter 格式合法（--- 包裹的 YAML）              │
 * │  - name 字段必填，格式：字母/数字/中划线/下划线/中文     │
 * │  - 正文（bodyContent）不能为空                          │
 * └─────────────────────────────────────────────────────────┘
 */
public final class SkillValidator {

    // ── 限制常量 ─────────────────────────────────────────────────────

    public static final long MAX_SINGLE_FILE_BYTES = 2L * 1024 * 1024;   // 2 MB
    public static final long MAX_ZIP_FILE_BYTES    = 10L * 1024 * 1024;  // 10 MB
    public static final long MAX_UNZIP_TOTAL_BYTES = 50L * 1024 * 1024;  // 50 MB
    public static final int  MAX_FILE_COUNT        = 100;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".md", ".txt", ".json", ".yaml", ".yml"
    );

    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
        ".sh", ".bash", ".zsh", ".fish",
        ".exe", ".dll", ".so", ".dylib",
        ".py", ".rb", ".php", ".js", ".ts", ".go",
        ".bat", ".cmd", ".ps1",
        ".jar", ".war", ".class"
    );

    private static final String NAME_PATTERN = "^[a-zA-Z0-9_\\-\u4e00-\u9fa5]{2,100}$";

    private SkillValidator() { /* 工具类，禁止实例化 */ }

    // ── ZIP 包校验 ───────────────────────────────────────────────────

    /**
     * 校验 ZIP 包安全性。
     * 注意：此方法只做安全检查，业务内容解析由 {@link SkillContentParser} 负责。
     *
     * @param file 上传的 ZIP 文件
     * @throws SkillValidationException 校验不通过时抛出
     */
    public static void validateZipFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new SkillValidationException("ZIP 文件不能为空");
        }
        if (file.getSize() > MAX_ZIP_FILE_BYTES) {
            throw new SkillValidationException(
                String.format("ZIP 包大小 %.1f MB 超过上限 10MB", file.getSize() / 1024.0 / 1024.0));
        }

        long    totalUnzipSize = 0;
        int     fileCount      = 0;
        boolean hasSkillMd     = false;

        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                validateZipEntry(entry, zis);

                if (!entry.isDirectory()) {
                    fileCount++;
                    if (fileCount > MAX_FILE_COUNT) {
                        throw new SkillValidationException("ZIP 包内文件数量超过 100 个");
                    }
                    long entrySize = entry.getSize() >= 0 ? entry.getSize() : readAndCountBytes(zis);
                    totalUnzipSize += entrySize;
                    if (totalUnzipSize > MAX_UNZIP_TOTAL_BYTES) {
                        throw new SkillValidationException("ZIP 解压后总大小超过 50MB（ZIP 炸弹防护）");
                    }
                    if ("SKILL.md".equals(entry.getName())) {
                        hasSkillMd = true;
                    }
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new SkillValidationException("ZIP 文件读取失败: " + e.getMessage());
        }

        if (!hasSkillMd) {
            throw new SkillValidationException("ZIP 包根目录中未找到 SKILL.md 文件");
        }
    }

    // ── 单文件校验（表单模式附件）────────────────────────────────────

    /**
     * 校验表单方式上传的单个附件文件。
     *
     * @param file 附件文件
     * @throws SkillValidationException 校验不通过时抛出
     */
    public static void validateAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) return;

        if (file.getSize() > MAX_SINGLE_FILE_BYTES) {
            throw new SkillValidationException(
                String.format("文件 %s 大小 %.1f MB 超过上限 2MB",
                    file.getOriginalFilename(), file.getSize() / 1024.0 / 1024.0));
        }
        String ext = getExtension(file.getOriginalFilename()).toLowerCase();
        if (DANGEROUS_EXTENSIONS.contains(ext)) {
            throw new SkillValidationException("不允许上传危险文件类型: " + file.getOriginalFilename());
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new SkillValidationException("不支持的文件类型: " + file.getOriginalFilename()
                + "，允许的类型：.md .txt .json .yaml .yml");
        }
    }

    // ── SKILL.md 内容校验 ─────────────────────────────────────────────

    /**
     * 校验 ParsedSkillContent 的合法性（由 {@link SkillContentParser} 解析后调用）。
     *
     * @param parsed 解析结果
     * @throws SkillValidationException 校验不通过时抛出
     */
    public static void validateSkillContent(ParsedSkillContent parsed) {
        if (parsed == null) {
            throw new SkillValidationException("技能内容解析失败");
        }
        if (!StringUtils.hasText(parsed.getName())) {
            throw new SkillValidationException("SKILL.md frontmatter 缺少必填字段: name");
        }
        if (!parsed.getName().matches(NAME_PATTERN)) {
            throw new SkillValidationException(
                "name 格式非法，只允许字母/数字/中划线/下划线/中文，长度 2-100，当前值: " + parsed.getName());
        }
        if (!StringUtils.hasText(parsed.getBodyContent())) {
            throw new SkillValidationException("SKILL.md 正文内容不能为空");
        }
        if (StringUtils.hasText(parsed.getExecutionContext())
                && !Set.of("inline", "fork").contains(parsed.getExecutionContext())) {
            throw new SkillValidationException("executionContext 非法值: " + parsed.getExecutionContext());
        }
        if (StringUtils.hasText(parsed.getEffort())
                && !Set.of("auto", "quick", "standard", "thorough").contains(parsed.getEffort())) {
            throw new SkillValidationException("effort 非法值: " + parsed.getEffort());
        }
    }

    // ── 私有工具方法 ─────────────────────────────────────────────────

    private static void validateZipEntry(ZipEntry entry, ZipInputStream zis) {
        String entryName = entry.getName();

        if (entryName.contains("../") || entryName.contains("..\\")
                || entryName.startsWith("/") || entryName.startsWith("\\")) {
            throw new SkillValidationException("ZIP 包含非法路径（路径穿越）: " + entryName);
        }
        if (entry.isDirectory()) return;

        String ext = getExtension(entryName).toLowerCase();
        if (DANGEROUS_EXTENSIONS.contains(ext)) {
            throw new SkillValidationException("ZIP 中包含不允许的危险文件类型: " + entryName);
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new SkillValidationException("ZIP 中包含不支持的文件类型: " + entryName
                + "，允许的类型：.md .txt .json .yaml .yml");
        }
        if (entry.getSize() > 0 && entry.getSize() > MAX_SINGLE_FILE_BYTES) {
            throw new SkillValidationException(
                String.format("文件 %s 大小 %.1f MB 超过单文件上限 2MB",
                    entryName, entry.getSize() / 1024.0 / 1024.0));
        }
    }

    private static String getExtension(String filename) {
        if (filename == null) return "";
        int dotIdx = filename.lastIndexOf('.');
        return dotIdx >= 0 ? filename.substring(dotIdx) : "";
    }

    private static long readAndCountBytes(ZipInputStream zis) throws IOException {
        byte[] buf = new byte[8192];
        long count = 0;
        int n;
        while ((n = zis.read(buf)) != -1) {
            count += n;
            if (count > MAX_SINGLE_FILE_BYTES) {
                throw new SkillValidationException("ZIP 中某文件超过单文件 2MB 限制");
            }
        }
        return count;
    }
}

