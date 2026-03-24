package org.dragon.skill.enums;

/**
 * Skill 生命周期状态枚举。
 *
 * 状态流转：
 * UNLOADED → LOADING → ACTIVE
 *                    → FAILED
 * ACTIVE   → DISABLED
 * DISABLED → LOADING（重新激活）
 * FAILED   → LOADING（重试）
 *
 * @since 1.0
 */
public enum SkillLifecycleState {
    /** 未加载（初始状态 / 已注销） */
    UNLOADED,
    /** 加载中 */
    LOADING,
    /** 已激活，可正常使用 */
    ACTIVE,
    /** 加载失败 */
    FAILED,
    /** 已禁用（手动停用） */
    DISABLED
}
