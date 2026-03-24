package org.dragon.skill.exception;

/**
 * Skill 校验异常。
 *
 * @since 1.0
 */
public class SkillValidationException extends SkillException {
    public SkillValidationException(String message) {
        super(message);
    }

    public SkillValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
