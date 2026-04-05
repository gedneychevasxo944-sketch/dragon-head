package org.dragon.skill.enums;

import io.ebean.annotation.DbEnumValue;
import lombok.RequiredArgsConstructor;

/**
 * Skill 生命周期状态。
 *
 * <pre>
 *   draft → active ⇄ disabled → deleted
 *   draft / active / disabled → deleted（任意状态均可删除）
 * </pre>
 */
@RequiredArgsConstructor
public enum SkillStatus {

    /** 草稿，刚注册/更新，尚未发布，不可被 Agent 加载 */
    DRAFT("draft"),

    /** 已发布，可被 Agent 正常加载和调用 */
    ACTIVE("active"),

    /** 已下架，暂时不可被 Agent 加载，可重新发布恢复 */
    DISABLED("disabled"),

    /** 已删除（软删除），终态不可逆 */
    DELETED("deleted");

    private final String value;

    /** Ebean 使用此方法的返回值存入 DB */
    @DbEnumValue
    public String getValue() { return value; }

    /** 从 DB 字符串值解析枚举，未匹配返回 null */
    public static SkillStatus fromValue(String value) {
        if (value == null) return null;
        for (SkillStatus s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }
}

