package org.dragon.skill.enums;

import io.ebean.annotation.DbEnumValue;
import lombok.RequiredArgsConstructor;

/**
 * Skill 生命周期状态。
 *
 * <pre>
 *   draft → active ⇄ disabled
 * </pre>
 */
@RequiredArgsConstructor
public enum SkillStatus {

    /** 草稿 */
    DRAFT("draft"),

    /** 已发布 */
    ACTIVE("active"),

    /** 已下架 */
    DISABLED("disabled");

    private final String value;

    @DbEnumValue
    public String getValue() { return value; }

    public static SkillStatus fromValue(String value) {
        if (value == null) return null;
        for (SkillStatus s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }
}
