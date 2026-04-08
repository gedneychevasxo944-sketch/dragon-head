package org.dragon.skill.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Skill 持续留存模式（{@code persist=true} 时生效）。
 */
@Getter
@RequiredArgsConstructor
public enum PersistMode {

    /** 每轮注入完整正文内容 */
    FULL("full"),

    /** 只注入正文中 ## Constraints / ## Rules 部分（节省 token） */
    SUMMARY("summary");

    private final String value;

    public static PersistMode fromValue(String value) {
        if (value == null) return FULL; // 默认 full
        for (PersistMode m : values()) {
            if (m.value.equals(value)) return m;
        }
        return FULL;
    }
}

