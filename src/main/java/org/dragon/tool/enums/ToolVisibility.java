package org.dragon.tool.enums;

import io.ebean.annotation.DbEnumValue;
import lombok.RequiredArgsConstructor;

/**
 * 工具可见性枚举。
 *
 * <pre>
 * PUBLIC    — 全局工具，所有 Character 默认可用，无需绑定
 * WORKSPACE — Workspace 工具，需绑定到 workspace 后可用
 * PRIVATE   — 私有工具，需绑定到 character 后才可用
 * </pre>
 */
@RequiredArgsConstructor
public enum ToolVisibility {

    /** 全局公开，所有 Character 默认可用 */
    PUBLIC("public"),

    /** Workspace 级别，绑定到 workspace 后可用 */
    WORKSPACE("workspace"),

    /** 私有，绑定到 character 后才可用 */
    PRIVATE("private");

    private final String value;

    @DbEnumValue
    public String getValue() { return value; }

    public static ToolVisibility fromValue(String value) {
        if (value == null) return PRIVATE;
        for (ToolVisibility v : values()) {
            if (v.value.equals(value)) return v;
        }
        return PRIVATE;
    }
}

