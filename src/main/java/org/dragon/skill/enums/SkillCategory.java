package org.dragon.skill.enums;

import io.ebean.annotation.DbEnumValue;
import lombok.RequiredArgsConstructor;

/**
 * Skill 功能分类。仅描述"这个 Skill 是干什么的"，与来源/加载策略无关。
 *
 * <p>"是否系统内置"由 {@link org.dragon.skill.domain.SkillDO#isBuiltin()} 字段单独表达，
 * 两者正交，一个内置 Skill 可以同时拥有任意功能分类（如 CODER、DATA_ANALYSIS 等）。
 */
@RequiredArgsConstructor
public enum SkillCategory {

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

