package org.dragon.tool.enums;

import io.ebean.annotation.DbEnumValue;
import lombok.RequiredArgsConstructor;

/**
 * 工具状态枚举。
 *
 * <pre>
 *   draft → active ⇄ disabled
 * </pre>
 *
 * <ul>
 *   <li>{@link #DRAFT}    - 草稿状态，不对外可见，仅注册者可查看</li>
 *   <li>{@link #ACTIVE}   - 正常可用，可被 Character 调用</li>
 *   <li>{@link #DISABLED} - 已禁用，不可调用，调用时返回错误</li>
 * </ul>
 *
 * <p>可见性过滤规则：ToolRegistry 只返回 status = {@link #ACTIVE} 的工具。
 */
@RequiredArgsConstructor
public enum ToolStatus {

    /** 草稿，不对外可见 */
    DRAFT("draft"),

    /** 正常可用 */
    ACTIVE("active"),

    /** 已禁用，不可调用 */
    DISABLED("disabled");

    private final String value;

    @DbEnumValue
    public String getValue() { return value; }

    public static ToolStatus fromValue(String value) {
        if (value == null) return null;
        for (ToolStatus s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }
}
