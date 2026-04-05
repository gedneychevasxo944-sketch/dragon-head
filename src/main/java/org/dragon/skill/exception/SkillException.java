package org.dragon.skill.exception;

/**
 * Skill 模块异常基类。
 */
public class SkillException extends RuntimeException {

    private final int code;

    public SkillException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

