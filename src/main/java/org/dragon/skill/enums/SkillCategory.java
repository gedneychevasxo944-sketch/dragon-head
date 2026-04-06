package org.dragon.skill.enums;

import io.ebean.annotation.DbEnumValue;
import lombok.RequiredArgsConstructor;

/**
 * Skill 分类。{@link #BUILTIN} 为系统保留类型，不允许用户创建同类型 Skill。
 */
@RequiredArgsConstructor
public enum SkillCategory {

    BUILTIN("builtin"),
    CONVERSATION("conversation"),
    CODER("coder"),
    DATA_ANALYSIS("data_analysis"),
    IMAGE_GENERATION("image_generation"),
    KNOWLEDGE_RETRIEVAL("knowledge_retrieval"),
    TOOL_CALLING("tool_calling"),
    DATA_PROCESSING("data_processing"),
    TEXT_GENERATION("text_generation"),
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

