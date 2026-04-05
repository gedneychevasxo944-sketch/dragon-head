package org.dragon.skill.enums;

import io.ebean.annotation.DbEnumValue;
import lombok.RequiredArgsConstructor;

/**
 * Skill 执行上下文（对应 SKILL.md frontmatter 中的 {@code context} 字段）。
 */
@RequiredArgsConstructor
public enum ExecutionContext {

    /** 在当前对话中直接展开 prompt，共享父 Agent 的工具和上下文 */
    INLINE("inline"),

    /** 创建独立 sub-AgentTask 执行，父 Agent 等待结果返回 */
    FORK("fork");

    private final String value;

    @DbEnumValue
    public String getValue() { return value; }

    public static ExecutionContext fromValue(String value) {
        if (value == null) return INLINE;
        for (ExecutionContext c : values()) {
            if (c.value.equals(value)) return c;
        }
        return INLINE;
    }
}

