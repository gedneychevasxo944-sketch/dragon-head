package org.dragon.skill.exception;

/**
 * Skill 基础异常。
 *
 * @since 1.0
 */
public class SkillException extends RuntimeException {
    public SkillException(String message) {
        super(message);
    }

    public SkillException(String message, Throwable cause) {
        super(message, cause);
    }
}
