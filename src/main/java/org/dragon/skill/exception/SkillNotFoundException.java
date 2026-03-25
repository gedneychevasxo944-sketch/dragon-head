package org.dragon.skill.exception;

/**
 * Skill 不存在异常。
 *
 * @since 1.0
 */
public class SkillNotFoundException extends SkillException {
    public SkillNotFoundException(String message) {
        super(message);
    }
}
