package org.dragon.skill.domain;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 解析 SKILL.md 后的中间结构。
 * <p>
 * 由 SkillContentParser 生成，供 SkillValidationService 校验，
 * 再由 SkillRegisterService 持久化到 DB 和 Storage。
 * </p>
 *
 * SKILL.md 结构：
 * <pre>
 * ---
 * name: my-skill
 * displayName: 我的技能
 * description: 做某件事
 * aliases: [sk, mysk]
 * whenToUse: 当需要...时使用
 * allowedTools: [bash, grep]
 * model: claude-3-5-sonnet
 * disableModelInvocation: false
 * userInvocable: true
 * context: inline
 * effort: standard
 * category: development
 * visibility: private
 * ---
 *
 * 正文内容（Prompt 模板）...
 * </pre>
 */
@Data
public class ParsedSkillContent {

    // ── frontmatter ─────────────────────────────────────────────────

    /**
     * SKILL.md 原始 frontmatter 内容（YAML 格式，不含正文）。
     * 用于存储原始 YAML，供前端展示和编辑。
     */
    private String frontmatter;

    // ── frontmatter 解析结果 ──────────────────────────────────────────

    private String name;
    private String displayName;
    private String description;
    private List<String> aliases;
    private String whenToUse;
    private String argumentHint;
    private List<String> allowedTools;
    private String model;
    private Boolean disableModelInvocation;
    private Boolean userInvocable;
    private String executionContext;
    private String effort;
    private String category;
    private String visibility;

    // ── 正文内容 ─────────────────────────────────────────────────────

    /** frontmatter 结束标记（第二个 ---）之后的全部文本，即 Prompt 正文 */
    private String bodyContent;

    // ── 文件集合（ZIP 模式下填充，表单模式下也会组装） ──────────────────

    /**
     * 文件映射：相对路径 → 文件字节内容
     * key 示例："SKILL.md"、"schema/input.json"
     */
    private Map<String, byte[]> fileMap;
}

