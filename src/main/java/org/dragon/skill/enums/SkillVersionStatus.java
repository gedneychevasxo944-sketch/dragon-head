package org.dragon.skill.enums;

import io.ebean.annotation.DbEnumValue;
import lombok.RequiredArgsConstructor;

/**
 * SkillVersion 生命周期状态。
 *
 * <pre>
 *   draft → published → deprecated
 * </pre>
 */
@RequiredArgsConstructor
public enum SkillVersionStatus {

    /** 草稿 */
    DRAFT("draft"),

    /** 已发布定稿 */
    PUBLISHED("published"),

    /** 已替代（被新版本替代） */
    DEPRECATED("deprecated");

    private final String value;

    @DbEnumValue
    public String getValue() { return value; }

    public static SkillVersionStatus fromValue(String value) {
        if (value == null) return null;
        for (SkillVersionStatus s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }
}