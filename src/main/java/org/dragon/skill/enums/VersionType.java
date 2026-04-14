package org.dragon.skill.enums;

import io.ebean.annotation.DbEnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Skill 绑定时的版本策略。
 */
@Getter
@RequiredArgsConstructor
public enum VersionType {

    /** 动态取最新 active 版本，Skill 更新后自动生效 */
    LATEST("latest"),

    /** 固定到指定版本号，Skill 更新后不受影响 */
    FIXED("fixed");

    private final String value;

    @DbEnumValue
    public String getValue() { return value; }

    public static VersionType fromValue(String value) {
        if (value == null) return LATEST;
        for (VersionType t : values()) {
            if (t.value.equals(value)) return t;
        }
        return LATEST;
    }
}
