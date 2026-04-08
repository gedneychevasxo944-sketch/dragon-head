package org.dragon.skill.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Skill 创建者类型。 */
@Getter
@RequiredArgsConstructor
public enum CreatorType {

    /** 个人用户创建 */
    PERSONAL("personal"),

    /** 平台官方创建 */
    OFFICIAL("official");

    private final String value;

    public static CreatorType fromValue(String value) {
        if (value == null) return null;
        for (CreatorType t : values()) {
            if (t.value.equals(value)) return t;
        }
        return null;
    }
}

