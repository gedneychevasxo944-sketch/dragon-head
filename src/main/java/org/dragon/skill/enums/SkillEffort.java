package org.dragon.skill.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Skill 执行的努力程度（对应 SKILL.md frontmatter 中的 {@code effort} 字段）。
 * 覆盖 Agent 默认 effort 配置。
 */
@Getter
@RequiredArgsConstructor
public enum SkillEffort {

    /** 自动，由模型决定 */
    AUTO("auto"),

    /** 快速，优先速度，减少思考步骤 */
    QUICK("quick"),

    /** 标准，均衡质量与速度（默认） */
    STANDARD("standard"),

    /** 深度，优先质量，允许更多思考步骤 */
    THOROUGH("thorough");

    private final String value;

    public static SkillEffort fromValue(String value) {
        if (value == null) return null;
        for (SkillEffort e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}

