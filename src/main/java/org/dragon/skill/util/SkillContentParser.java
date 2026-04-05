package org.dragon.skill.util;

import org.dragon.skill.domain.ParsedSkillContent;
import org.dragon.skill.dto.SkillRegisterRequest;
import org.dragon.skill.exception.SkillValidationException;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * SKILL.md 内容解析工具类（无状态，全静态方法）。
 *
 * <p>负责将两种输入来源转换为统一的 {@link ParsedSkillContent}：
 * <ol>
 *   <li>表单模式：直接从 SkillRegisterRequest 字段组装</li>
 *   <li>ZIP 模式：从 ZIP 包中读取 SKILL.md，解析 frontmatter + 正文</li>
 * </ol>
 *
 * <p>SKILL.md 格式规范：
 * <pre>
 * ---
 * name: my-skill
 * displayName: 我的技能
 * description: 做某件事
 * aliases:
 *   - sk
 *   - mysk
 * whenToUse: 当需要...时使用
 * argumentHint: "[scope]"
 * allowedTools:
 *   - bash
 *   - grep
 * model: claude-3-5-sonnet
 * disableModelInvocation: false
 * userInvocable: true
 * context: inline
 * effort: standard
 * category: development
 * visibility: private
 * ---
 *
 * Prompt 正文内容...
 * </pre>
 */
public final class SkillContentParser {

    private static final String FRONTMATTER_DELIMITER = "---";

    private SkillContentParser() { /* 工具类，禁止实例化 */ }

    // ── 表单模式 ─────────────────────────────────────────────────────

    /**
     * 从表单请求直接组装 ParsedSkillContent。
     * content 字段即为 SKILL.md 正文（不含 frontmatter）。
     * 同时将元信息 + 正文序列化为完整 SKILL.md 字节写入 fileMap，
     * 保证后续存储流程与 ZIP 模式完全一致。
     *
     * @param req 表单注册请求
     * @return 解析后的中间结构
     */
    public static ParsedSkillContent parseFromForm(SkillRegisterRequest req) {
        ParsedSkillContent parsed = new ParsedSkillContent();
        parsed.setName(req.getName());
        parsed.setDisplayName(req.getDisplayName());
        parsed.setDescription(req.getDescription());
        parsed.setAliases(req.getAliases());
        parsed.setWhenToUse(req.getWhenToUse());
        parsed.setArgumentHint(req.getArgumentHint());
        parsed.setAllowedTools(req.getAllowedTools());
        parsed.setModel(req.getModel());
        parsed.setDisableModelInvocation(
            req.getDisableModelInvocation() != null ? req.getDisableModelInvocation() : Boolean.FALSE);
        parsed.setUserInvocable(
            req.getUserInvocable() != null ? req.getUserInvocable() : Boolean.TRUE);
        parsed.setExecutionContext(
            req.getExecutionContext() != null ? req.getExecutionContext() : "inline");
        parsed.setEffort(req.getEffort());
        parsed.setCategory(req.getCategory());
        parsed.setVisibility(
            req.getVisibility() != null ? req.getVisibility() : "private");
        parsed.setBodyContent(req.getContent());

        // 将元信息 + 正文序列化为完整 SKILL.md 写入 fileMap
        Map<String, byte[]> fileMap = new LinkedHashMap<>();
        fileMap.put("SKILL.md", serializeToMarkdown(parsed).getBytes(StandardCharsets.UTF_8));

        // 附件文件
        if (req.getAttachments() != null) {
            for (MultipartFile attachment : req.getAttachments()) {
                try {
                    String filename = Objects.requireNonNull(attachment.getOriginalFilename());
                    // 路径遍历防护：附件文件名同样需要校验
                    validateZipEntryPath(filename);
                    fileMap.put(filename, attachment.getBytes());
                } catch (IOException e) {
                    throw new SkillValidationException("读取附件文件失败: " + attachment.getOriginalFilename());
                }
            }
        }
        parsed.setFileMap(fileMap);
        return parsed;
    }

    // ── ZIP 模式 ─────────────────────────────────────────────────────

    /**
     * 从 ZIP 包解析 ParsedSkillContent。
     * 读取 ZIP 中所有文件到 fileMap，并解析根目录的 SKILL.md。
     *
     * <p>注意：调用前应先通过 {@link SkillValidator#validateZipFile} 做安全校验。
     *
     * @param zipFile 上传的 ZIP 文件
     * @return 解析后的中间结构
     */
    public static ParsedSkillContent parseFromZip(MultipartFile zipFile) {
        Map<String, byte[]> fileMap = new LinkedHashMap<>();

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    // 路径遍历防护：校验 ZIP 条目路径不能逃逸根目录
                    validateZipEntryPath(entry.getName());
                    fileMap.put(entry.getName(), zis.readAllBytes());
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new SkillValidationException("ZIP 文件解析失败: " + e.getMessage());
        }

        byte[] skillMdBytes = fileMap.get("SKILL.md");
        if (skillMdBytes == null) {
            throw new SkillValidationException("ZIP 包根目录中未找到 SKILL.md 文件");
        }

        String rawContent = new String(skillMdBytes, StandardCharsets.UTF_8);
        ParsedSkillContent parsed = parseMarkdown(rawContent);
        parsed.setFileMap(fileMap);
        return parsed;
    }

    // ── SKILL.md 解析 ─────────────────────────────────────────────────

    /**
     * 将原始 SKILL.md 文本解析为 ParsedSkillContent（不含 fileMap）。
     *
     * @param rawContent SKILL.md 原始文本
     * @return 解析结果
     * @throws SkillValidationException frontmatter 格式错误时抛出
     */
    public static ParsedSkillContent parseMarkdown(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new SkillValidationException("SKILL.md 内容为空");
        }
        if (!rawContent.startsWith(FRONTMATTER_DELIMITER)) {
            throw new SkillValidationException("SKILL.md 必须以 --- 开头（缺少 frontmatter）");
        }

        int bodyStart = rawContent.indexOf("\n" + FRONTMATTER_DELIMITER, FRONTMATTER_DELIMITER.length());
        if (bodyStart < 0) {
            throw new SkillValidationException("SKILL.md frontmatter 格式错误：缺少结束标记 ---");
        }

        String yamlBlock   = rawContent.substring(FRONTMATTER_DELIMITER.length(), bodyStart).trim();
        String bodyContent = rawContent.substring(bodyStart + FRONTMATTER_DELIMITER.length() + 1).trim();

        Map<String, Object> fm = parseYaml(yamlBlock);

        ParsedSkillContent result = new ParsedSkillContent();
        result.setName(getString(fm, "name"));
        result.setDisplayName(getString(fm, "displayName"));
        result.setDescription(getString(fm, "description"));
        result.setAliases(toStringList(fm.get("aliases")));
        result.setWhenToUse(getString(fm, "whenToUse"));
        result.setArgumentHint(getString(fm, "argumentHint"));
        result.setAllowedTools(toStringList(fm.get("allowedTools")));
        result.setModel(getString(fm, "model"));
        result.setDisableModelInvocation(toBool(fm.get("disableModelInvocation"), false));
        result.setUserInvocable(toBool(fm.get("userInvocable"), true));
        result.setExecutionContext(getStringOrDefault(fm, "context", "inline"));
        result.setEffort(getString(fm, "effort"));
        result.setCategory(getString(fm, "category"));
        result.setVisibility(getStringOrDefault(fm, "visibility", "private"));
        result.setBodyContent(bodyContent);
        return result;
    }

    // ── SKILL.md 序列化 ───────────────────────────────────────────────

    /**
     * 将 ParsedSkillContent 序列化为完整的 SKILL.md 文本（frontmatter + 正文）。
     * 供表单模式使用：在线编辑后将元信息重新写成文件，保持与 ZIP 模式存储一致。
     */
    public static String serializeToMarkdown(ParsedSkillContent parsed) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        appendYamlField(sb, "name",                   parsed.getName());
        appendYamlField(sb, "displayName",            parsed.getDisplayName());
        appendYamlField(sb, "description",            parsed.getDescription());
        appendYamlList (sb, "aliases",                parsed.getAliases());
        appendYamlField(sb, "whenToUse",              parsed.getWhenToUse());
        appendYamlField(sb, "argumentHint",           parsed.getArgumentHint());
        appendYamlList (sb, "allowedTools",           parsed.getAllowedTools());
        appendYamlField(sb, "model",                  parsed.getModel());
        appendYamlField(sb, "disableModelInvocation", String.valueOf(
            Boolean.TRUE.equals(parsed.getDisableModelInvocation())));
        appendYamlField(sb, "userInvocable",          String.valueOf(
            !Boolean.FALSE.equals(parsed.getUserInvocable())));
        appendYamlField(sb, "context",               parsed.getExecutionContext());
        appendYamlField(sb, "effort",                 parsed.getEffort());
        appendYamlField(sb, "category",               parsed.getCategory());
        appendYamlField(sb, "visibility",             parsed.getVisibility());
        sb.append("---\n\n");
        sb.append(parsed.getBodyContent() != null ? parsed.getBodyContent() : "");
        return sb.toString();
    }

    // ── 私有工具方法 ─────────────────────────────────────────────────

    /**
     * 路径遍历防护：校验相对路径不能逃逸根目录。
     *
     * <p>防护规则：
     * <ul>
     *   <li>不允许绝对路径（{@code /foo} 或 Windows {@code C:\foo}）</li>
     *   <li>不允许路径组件包含 {@code ..}（防止 {@code ../../../etc/passwd} 类攻击）</li>
     *   <li>不允许空路径</li>
     * </ul>
     *
     * @param relPath 待校验的相对路径（ZIP entry 名称 或 附件文件名）
     * @throws SkillValidationException 路径不合法时抛出
     */
    static void validateZipEntryPath(String relPath) {
        if (relPath == null || relPath.isBlank()) {
            throw new SkillValidationException("文件路径不能为空");
        }
        // normalize() 将 a/../b 折叠为 b、去掉多余 /，便于统一检查
        Path normalized = Paths.get(relPath).normalize();
        if (normalized.isAbsolute()) {
            throw new SkillValidationException("非法文件路径（绝对路径）: " + relPath);
        }
        // normalize 后首段为 ".." 说明路径逃逸了根目录
        String first = normalized.getName(0).toString();
        if ("..".equals(first)) {
            throw new SkillValidationException("非法文件路径（路径遍历）: " + relPath);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseYaml(String yamlText) {
        try {
            Yaml yaml = new Yaml();
            Object obj = yaml.load(yamlText);
            if (!(obj instanceof Map)) {
                throw new SkillValidationException("SKILL.md frontmatter 格式错误：不是合法的 YAML 映射");
            }
            return (Map<String, Object>) obj;
        } catch (SkillValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SkillValidationException("SKILL.md frontmatter YAML 解析失败: " + e.getMessage());
        }
    }

    private static String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString().trim() : null;
    }

    private static String getStringOrDefault(Map<String, Object> map, String key, String defaultVal) {
        String v = getString(map, key);
        return v != null && !v.isEmpty() ? v : defaultVal;
    }

    @SuppressWarnings("unchecked")
    private static List<String> toStringList(Object val) {
        if (val == null) return Collections.emptyList();
        if (val instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) val) result.add(item.toString());
            return result;
        }
        return List.of(val.toString());
    }

    private static Boolean toBool(Object val, boolean defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Boolean) return (Boolean) val;
        return Boolean.parseBoolean(val.toString());
    }

    private static void appendYamlField(StringBuilder sb, String key, String value) {
        if (value != null && !value.isEmpty()) {
            String escaped = value.contains(":") || value.contains("#")
                ? "\"" + value.replace("\"", "\\\"") + "\""
                : value;
            sb.append(key).append(": ").append(escaped).append("\n");
        }
    }

    private static void appendYamlList(StringBuilder sb, String key, List<String> list) {
        if (list != null && !list.isEmpty()) {
            sb.append(key).append(":\n");
            for (String item : list) {
                sb.append("  - ").append(item).append("\n");
            }
        }
    }
}

