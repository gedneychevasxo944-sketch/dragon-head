package org.dragon.skill;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.skill.model.Skill;
import org.dragon.skill.model.SkillEntry;
import org.dragon.skill.model.SkillInstallSpec;
import org.dragon.skill.model.SkillInvocationPolicy;
import org.dragon.skill.model.SkillMetadata;
import org.dragon.skill.model.SkillRequires;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 技能 Frontmatter 解析器。
 * 解析 SKILL.md 文件中类似 YAML 的 frontmatter 并提取元数据。
 *
 * @since 1.0
 */
@Slf4j
public class SkillFrontmatterParser {

    /**
     * 解析结果封装（用于上传阶段，不含运行时信息）。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillParseResult {
        /** frontmatter 中的 name */
        private String name;
        /** frontmatter 中的 description（供 LLM 使用） */
        private String skillDescription;
        /** frontmatter 之后的正文内容 */
        private String skillContent;
        /** 解析后的依赖要求 */
        private SkillRequires requires;
        /** 解析后的安装规范列表 */
        private List<SkillInstallSpec> installSpecs;
        /** frontmatter 原始 YAML 字符串 */
        private String frontmatterRaw;
    }

    /**
     * 从输入流解析 SKILL.md 内容。
     * 在 ZIP 上传阶段调用，解析结果持久化到数据库。
     * 不再依赖本地文件路径。
     *
     * @param inputStream SKILL.md 文件的输入流
     * @return 解析结果（包含 frontmatter、content、metadata）
     */
    public static SkillParseResult parseFromStream(InputStream inputStream) {
        String rawContent;
        try {
            rawContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new org.dragon.skill.exception.SkillLoadException("读取 SKILL.md 内容失败", e);
        }
        return parseFromString(rawContent);
    }

    /**
     * 从字符串解析 SKILL.md 内容。
     * 在 ZIP 上传阶段调用，解析结果持久化到数据库。
     *
     * @param rawContent SKILL.md 完整原始内容
     * @return 解析结果（包含 frontmatter、content、metadata）
     */
    public static SkillParseResult parseFromString(String rawContent) {
        Map<String, String> frontmatter = parseFrontmatter(rawContent);
        String body = extractBody(rawContent);

        // 获取 frontmatter 原始 YAML
        String frontmatterRaw = "";
        if (rawContent.startsWith("---")) {
            int endIndex = rawContent.indexOf("\n---", 3);
            if (endIndex != -1) {
                frontmatterRaw = rawContent.substring(4, endIndex).trim();
            }
        }

        // 解析 requires 和 install
        SkillRequires requires = null;
        List<SkillInstallSpec> installSpecs = null;

        String metadataRaw = frontmatter.get("metadata");
        if (metadataRaw != null && !metadataRaw.isBlank()) {
            try {
                JsonElement root = JsonParser.parseString(metadataRaw);
                if (root != null && root.isJsonObject()) {
                    JsonObject metaObj = root.getAsJsonObject();
                    // 查找 dragonhead 节点
                    if (metaObj.has("dragonhead")) {
                        JsonElement dragonhead = metaObj.get("dragonhead");
                        if (dragonhead != null && dragonhead.isJsonObject()) {
                            JsonObject dhObj = dragonhead.getAsJsonObject();
                            requires = parseRequires(dhObj);
                            installSpecs = parseInstallSpecs(dhObj);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析 metadata 失败: {}", e.getMessage());
            }
        }

        return SkillParseResult.builder()
                .name(frontmatter.get("name"))
                .skillDescription(frontmatter.get("description"))
                .skillContent(body)
                .requires(requires)
                .installSpecs(installSpecs)
                .frontmatterRaw(frontmatterRaw)
                .build();
    }

    private static SkillRequires parseRequires(JsonObject meta) {
        if (!meta.has("requires")) return null;
        JsonElement req = meta.get("requires");
        if (req == null || !req.isJsonObject()) return null;
        JsonObject reqObj = req.getAsJsonObject();
        return new SkillRequires(
                stringList(reqObj, "bins"),
                stringList(reqObj, "anyBins"),
                stringList(reqObj, "env"),
                stringList(reqObj, "config"));
    }

    private static List<SkillInstallSpec> parseInstallSpecs(JsonObject meta) {
        if (!meta.has("install")) return null;
        JsonElement install = meta.get("install");
        if (install == null || !install.isJsonArray()) return null;
        // TODO: 完善 installSpecs 解析
        return null;
    }


    /**
     * 用于提取文件开头 --- 分隔符之间的 frontmatter 块的正则表达式。
     * 匹配：---\nkey: value\nkey: value\n---
     */
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("\\A---\s*\n(.*?)\n---\s*\n?",
            Pattern.DOTALL);

    /**
     * 用于 frontmatter 行的简单 key: value 模式。
     */
    private static final Pattern KV_PATTERN = Pattern.compile("^([a-zA-Z][a-zA-Z0-9_-]*)\s*:\s*(.*)$");

    // =========================================================================
    // Frontmatter 解析 (Frontmatter parsing)
    // =========================================================================

    /**
     * 从 SKILL.md 内容中解析类似 YAML 的 frontmatter。
     *
     * @param content 完整的 SKILL.md 文件内容
     * @return frontmatter 中的键值对映射；如果未找到则返回空映射
     */
    public static Map<String, String> parseFrontmatter(String content) {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        Matcher m = FRONTMATTER_PATTERN.matcher(content);
        if (!m.find()) {
            return Collections.emptyMap();
        }

        String block = m.group(1);
        Map<String, String> result = new LinkedHashMap<>();
        StringBuilder currentKey = null;
        StringBuilder currentValue = null;

        for (String line : block.split("\\n")) {
            Matcher kv = KV_PATTERN.matcher(line);
            if (kv.matches()) {
                // 刷新上一个键
                if (currentKey != null && currentValue != null) {
                    result.put(currentKey.toString(), currentValue.toString().trim());
                }
                currentKey = new StringBuilder(kv.group(1));
                currentValue = new StringBuilder(kv.group(2));
            } else if (currentKey != null && currentValue != null) {
                // 续行（用于多行值）
                currentValue.append("\n").append(line);
            }
        }

        // 刷新最后一个键
        if (currentKey != null && currentValue != null) {
            result.put(currentKey.toString(), currentValue.toString().trim());
        }

        return result;
    }

    /**
     * 从 SKILL.md 中提取正文内容（frontmatter 之后的部分）。
     */
    public static String extractBody(String content) {
        if (content == null || content.trim().isEmpty())
            return "";
        Matcher m = FRONTMATTER_PATTERN.matcher(content);
        if (m.find()) {
            return content.substring(m.end()).trim();
        }
        return content.trim();
    }

    // =========================================================================
    // 元数据解析 (Metadata resolution)
    // =========================================================================

    /**
     * 从解析后的 frontmatter 中解析 DragonHead 专属元数据。
     * 查找包含 "dragonhead" 子键的 JSON 对象的 "metadata" 键。
     *
     * @param frontmatter 解析后的 frontmatter 映射
     * @return 解析后的元数据，如果不存在则返回 null
     */
    public static SkillMetadata resolveMetadata(Map<String, String> frontmatter) {
        String raw = frontmatter.get("metadata");
        if (raw == null || raw.trim().isEmpty())
            return null;

        try {
            JsonElement root = JsonParser.parseString(raw);
            if (root == null || !root.isJsonObject())
                return null;

            // 查找 dragonhead的skill 元数据（尝试多个键以保持兼容性）
            JsonElement meta = findMetadataNode(root.getAsJsonObject());
            if (meta == null || !meta.isJsonObject())
                return null;

            JsonObject metaObj = meta.getAsJsonObject();
            return new SkillMetadata(
                    hasAndTrue(metaObj, "always") ? true : null,
                    textOrNull(metaObj, "skillKey"),
                    textOrNull(metaObj, "primaryEnv"),
                    textOrNull(metaObj, "emoji"),
                    textOrNull(metaObj, "homepage"),
                    stringList(metaObj, "os"),
                    resolveRequires(metaObj),
                    null);
        } catch (Exception e) {
            log.debug("解析技能元数据失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查 JSON 对象中是否存在某字段且值为 true
     */
    private static boolean hasAndTrue(JsonObject obj, String field) {
        if (!obj.has(field)) return false;
        JsonElement el = obj.get(field);
        return el != null && el.isJsonPrimitive() && el.getAsBoolean();
    }

    /**
     * 从 frontmatter 解析调用策略。
     */
    public static SkillInvocationPolicy resolveInvocationPolicy(
            Map<String, String> frontmatter) {
        boolean userInvocable = parseBool(frontmatter.get("user-invocable"), true);
        boolean disableModelInvocation = parseBool(
                frontmatter.get("disable-model-invocation"), false);
        return new SkillInvocationPolicy(userInvocable, disableModelInvocation);
    }

    /**
     * 从技能名称和条目中解析技能键（标识符）。
     */
    public static String resolveSkillKey(Skill skill, SkillEntry entry) {
        if (entry != null && entry.getMetadata() != null
                && entry.getMetadata().getSkillKey() != null) {
            return entry.getMetadata().getSkillKey();
        }
        return skill.getName();
    }

    // =========================================================================
    // 辅助方法 (Helpers)
    // =========================================================================

    private static JsonElement findMetadataNode(JsonObject root) {
        // metadata: { "dragonhead": { "always": true, "emoji": "🧪", "os": ["darwin", "linux"] } }
        for (String key : Arrays.asList("dragonhead")) {
            if (root.has(key)) {
                JsonElement node = root.get(key);
                if (node != null && node.isJsonObject())
                    return node;
            }
        }
        return null;
    }

    private static String textOrNull(JsonObject obj, String field) {
        if (!obj.has(field)) return null;
        JsonElement child = obj.get(field);
        return (child != null && !child.isJsonNull() && child.isJsonPrimitive()) ? child.getAsString() : null;
    }

    private static List<String> stringList(JsonObject obj, String field) {
        if (!obj.has(field)) return Collections.emptyList();
        JsonElement child = obj.get(field);
        if (child == null || child.isJsonNull())
            return Collections.emptyList();
        if (child.isJsonArray()) {
            List<String> result = new ArrayList<>();
            for (JsonElement el : child.getAsJsonArray()) {
                if (el != null && el.isJsonPrimitive())
                    result.add(el.getAsString().trim());
            }
            return result;
        }
        if (child.isJsonPrimitive()) {
            return Arrays.stream(child.getAsString().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static SkillRequires resolveRequires(JsonObject meta) {
        if (!meta.has("requires")) return null;
        JsonElement req = meta.get("requires");
        if (req == null || !req.isJsonObject())
            return null;
        JsonObject reqObj = req.getAsJsonObject();
        return new SkillRequires(
                stringList(reqObj, "bins"),
                stringList(reqObj, "anyBins"),
                stringList(reqObj, "env"),
                stringList(reqObj, "config"));
    }

    private static boolean parseBool(String value, boolean fallback) {
        if (value == null || value.trim().isEmpty())
            return fallback;
        String v = value.trim().toLowerCase();
        switch (v) {
            case "true":
            case "yes":
            case "1":
            case "on":
                return true;
            case "false":
            case "no":
            case "0":
            case "off":
                return false;
            default:
                return fallback;
        }
    }
}
