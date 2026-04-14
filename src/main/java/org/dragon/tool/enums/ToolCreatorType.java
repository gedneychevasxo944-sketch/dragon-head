package org.dragon.tool.enums;

import io.ebean.annotation.DbEnumValue;
import lombok.RequiredArgsConstructor;

/**
 * 工具创建者类型。
 */
@RequiredArgsConstructor
public enum ToolCreatorType {

    /** 个人用户创建 */
    PERSONAL("personal"),

    /** 平台官方创建 */
    OFFICIAL("official");

    private final String value;

    @DbEnumValue
    public String getValue() { return value; }

    public static ToolCreatorType fromValue(String value) {
        if (value == null) return null;
        for (ToolCreatorType t : values()) {
            if (t.value.equals(value)) return t;
        }
        return null;
    }
}

