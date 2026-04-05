package org.dragon.skill.enums;

import io.ebean.annotation.DbEnumValue;
import lombok.RequiredArgsConstructor;

/** Skill 可见性。 */
@RequiredArgsConstructor
public enum SkillVisibility {

    PUBLIC("public"),
    PRIVATE("private");

    private final String value;

    @DbEnumValue
    public String getValue() { return value; }

    public static SkillVisibility fromValue(String value) {
        if (value == null) return PRIVATE;
        for (SkillVisibility v : values()) {
            if (v.value.equals(value)) return v;
        }
        return PRIVATE;
    }
}

