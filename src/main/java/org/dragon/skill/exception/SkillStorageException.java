package org.dragon.skill.exception;

/**
 * Skill 存储相关异常。
 *
 * @since 1.0
 */
public class SkillStorageException extends SkillException {

    public SkillStorageException(String message) {
        super(message);
    }

    public SkillStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}