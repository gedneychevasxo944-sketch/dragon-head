package org.dragon.skill.registry;

/**
 * Skill 运行时状态枚举。
 * 仅存在于内存中，不持久化到数据库。
 * 描述 Skill 在当前进程生命周期内的加载状态。
 *
 * 状态流转：
 * UNLOADED → LOADING → ACTIVE
 *                    → FAILED
 * ACTIVE   → UNLOADED（禁用/删除触发注销）
 * FAILED   → LOADING（重试）
 */
public enum SkillRuntimeState {
    /** 未加载（初始状态 / 已注销） */
    UNLOADED,
    /** 加载中 */
    LOADING,
    /** 已激活，可正常使用 */
    ACTIVE,
    /** 加载失败（含依赖检查失败） */
    FAILED
}
