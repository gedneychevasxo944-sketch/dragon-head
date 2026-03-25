package org.dragon.skill.model;

import lombok.Value;

/**
 * 控制技能如何/是否可以被调用。
 *
 * @since 1.0
 */
@Value
public class SkillInvocationPolicy {
    /** 用户是否可以直接调用此技能 */
    boolean userInvocable;
    /** 模型是否应禁止自动调用此技能 */
    boolean disableModelInvocation;

    public static final SkillInvocationPolicy DEFAULT = new SkillInvocationPolicy(true, false);
}
