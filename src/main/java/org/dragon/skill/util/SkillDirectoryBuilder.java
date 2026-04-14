package org.dragon.skill.util;

import org.dragon.skill.runtime.SkillDefinition;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Skill 目录构建器（设计点 2 - 目录与内容分离）。
 *
 * <p>核心思想：system prompt 中只注入 Skill 的"目录"（name + whenToUse + argumentHint），
 * 不包含完整 prompt 正文。当 Agent 决定调用某个 Skill 时，再由 SkillExecutor 按需加载完整内容。
 *
 * <p>好处：
 * <ul>
 *   <li>100 个 Skill 注入目录仅多约 2000 token，而展开全部内容可能消耗数万 token</li>
 *   <li>让模型先通过 whenToUse 判断"是否需要调用"，降低无效展开</li>
 * </ul>
 *
 * <p>生成的目录 prompt 示例：
 * <pre>
 * ## Available Skills
 * Use the Skill tool to invoke any of the following skills:
 *
 * - **deploy-check** [scope]: 部署前自动检查代码质量和安全风险
 *   When to use: 在执行部署操作前，或需要检查代码是否符合部署标准时使用
 *
 * - **git-commit**: 生成规范的 commit message
 *   When to use: 当需要提交代码时，自动生成符合 Conventional Commits 规范的 message
 * </pre>
 */
public class SkillDirectoryBuilder {

    private SkillDirectoryBuilder() {}

    /**
     * 构建 Skill 目录 prompt 字符串，注入到 system prompt 中。
     *
     * @param skills 过滤后的可见 Skill 列表（已经过 SkillFilter）
     * @return 目录 prompt 字符串，列表为空时返回空字符串
     */
    public static String buildDirectoryPrompt(List<SkillDefinition> skills) {
        if (skills == null || skills.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## Available Skills\n");
        sb.append("Use the Skill tool to invoke any of the following skills:\n\n");

        for (SkillDefinition skill : skills) {
            appendSkillEntry(sb, skill);
        }

        return sb.toString().trim();
    }

    /**
     * 构建单个 Skill 的目录条目（仅含名称、参数提示、描述、whenToUse）。
     * 不包含完整 prompt 正文 —— 那是 SkillExecutor 的职责。
     */
    private static void appendSkillEntry(StringBuilder sb, SkillDefinition skill) {
        // 名称 + 可选参数提示
        sb.append("- **").append(skill.getName()).append("**");
        if (StringUtils.hasText(skill.getArgumentHint())) {
            sb.append(" ").append(skill.getArgumentHint());
        }

        // 描述
        if (StringUtils.hasText(skill.getDescription())) {
            sb.append(": ").append(skill.getDescription());
        }
        sb.append("\n");

        // whenToUse（关键：驱动模型自动判断何时调用）
        if (StringUtils.hasText(skill.getWhenToUse())) {
            sb.append("  When to use: ").append(skill.getWhenToUse()).append("\n");
        }

        sb.append("\n");
    }

    /**
     * 构建"已激活 Skill 约束"的留存注入片段（设计点 6 - Skill 持续留存）。
     *
     * <p>每轮对话开始时，将 session 中已调用且 persist=true 的 Skill 内容
     * 以 {@code <skill-context>} 格式追加到 system prompt，防止模型遗忘约束规则。
     *
     * @param persistedContents key=skillName, value=需要注入的内容片段
     * @return 留存注入 prompt，无内容时返回空字符串
     */
    public static String buildPersistedSkillPrompt(java.util.Map<String, String> persistedContents) {
        if (persistedContents == null || persistedContents.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## Active Skill Constraints\n");
        sb.append("The following skill constraints are active and MUST be followed:\n\n");

        for (java.util.Map.Entry<String, String> entry : persistedContents.entrySet()) {
            sb.append("<skill-context name=\"").append(entry.getKey()).append("\">\n");
            sb.append(entry.getValue().trim()).append("\n");
            sb.append("</skill-context>\n\n");
        }

        return sb.toString().trim();
    }

    /**
     * 从完整 prompt 正文中提取 summary 片段（persistMode=summary 时使用）。
     *
     * <p>提取规则：找到 {@code ## Constraints}、{@code ## Rules}、{@code ## 约束}、
     * {@code ## 规则} 等标题之后的内容，直到下一个同级标题或文档末尾。
     *
     * @param fullContent 完整 prompt 正文
     * @return 约束/规则部分的文本，找不到时返回原文（降级为 full）
     */
    public static String extractSummary(String fullContent) {
        if (!StringUtils.hasText(fullContent)) return "";

        // 匹配常见约束标题（支持中英文）
        String[] constraintHeaders = {
                "## Constraints", "## Rules", "## 约束", "## 规则",
                "## CONSTRAINTS", "## RULES", "## Requirements", "## 要求"
        };

        String[] lines = fullContent.split("\n");
        int startLine = -1;
        int endLine = lines.length;

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            // 找到约束标题
            if (startLine == -1) {
                for (String header : constraintHeaders) {
                    if (trimmed.equalsIgnoreCase(header) || trimmed.startsWith(header + " ")) {
                        startLine = i;
                        break;
                    }
                }
            } else {
                // 找到下一个同级 ## 标题时停止
                if (trimmed.startsWith("## ") && i > startLine) {
                    endLine = i;
                    break;
                }
            }
        }

        if (startLine == -1) {
            // 找不到约束标题，降级返回全文（最多前 500 字符）
            return fullContent.length() > 500 ? fullContent.substring(0, 500) + "\n..." : fullContent;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = startLine; i < endLine; i++) {
            sb.append(lines[i]).append("\n");
        }
        return sb.toString().trim();
    }
}

