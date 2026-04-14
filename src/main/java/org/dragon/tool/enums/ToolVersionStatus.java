package org.dragon.tool.enums;

import io.ebean.annotation.DbEnumValue;
import lombok.RequiredArgsConstructor;

/**
 * 工具版本生命周期状态。
 *
 * <pre>
 *   draft → published → deprecated
 * </pre>
 *
 * <ul>
 *   <li>{@link #DRAFT}      - 草稿，未对外发布</li>
 *   <li>{@link #PUBLISHED}  - 已发布定稿，LLM 调用使用此版本</li>
 *   <li>{@link #DEPRECATED} - 已被新版本替代</li>
 * </ul>
 */
@RequiredArgsConstructor
public enum ToolVersionStatus {

    /** 草稿，未对外发布 */
    DRAFT("draft"),

    /** 已发布定稿 */
    PUBLISHED("published"),

    /** 已被新版本替代 */
    DEPRECATED("deprecated");

    private final String value;

    @DbEnumValue
    public String getValue() { return value; }

    public static ToolVersionStatus fromValue(String value) {
        if (value == null) return null;
        for (ToolVersionStatus s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }
}

