package org.dragon.skill.enums;

/**
 * Skill 权限规则行为枚举。
 *
 * <p>对应 TS 版本 {@code PermissionDecision.behavior} 的取值：
 * <ul>
 *   <li>{@link #ALLOW} — 明确放行</li>
 *   <li>{@link #DENY}  — 明确拒绝</li>
 * </ul>
 *
 * <p>注意：ask（弹出确认）不在此枚举中，它是 {@code SkillPermissionResult.Behavior.ASK}，
 * 由 {@code SkillPermissionChecker} 在规则均未命中且 Skill 含有"非安全属性"时返回。
 */
public enum SkillPermissionRule {

    /** 允许执行 */
    ALLOW,

    /** 拒绝执行 */
    DENY
}

