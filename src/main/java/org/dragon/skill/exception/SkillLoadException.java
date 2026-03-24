package org.dragon.skill.exception;

/**
 * Skill 加载异常。
 *
 * @since 1.0
 */
public class SkillLoadException extends SkillException {
    public SkillLoadException(String message) {
        super(message);
    }

    public SkillLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
