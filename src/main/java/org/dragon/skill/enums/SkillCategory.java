package org.dragon.skill.enums;

import io.ebean.annotation.DbEnumValue;
import lombok.RequiredArgsConstructor;

/**
 * Skill 分类。{@link #BUILTIN} 为系统保留类型，不允许用户创建同类型 Skill。
 */
@RequiredArgsConstructor
public enum SkillCategory {

    BUILTIN("builtin"),
    DEVELOPMENT("development"),
    DEPLOYMENT("deployment"),
    ANALYSIS("analysis"),
    UTILITY("utility"),
    INTEGRATION("integration"),
    OTHER("other");

    private final String value;

    @DbEnumValue
    public String getValue() { return value; }

    public static SkillCategory fromValue(String value) {
        if (value == null) return null;
        for (SkillCategory c : values()) {
            if (c.value.equals(value)) return c;
        }
        return null;
    }
}

