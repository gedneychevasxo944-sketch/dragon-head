package org.dragon.skill.exception;

/**
 * 文件/内容校验失败异常。
 * HTTP 状态码：400 Bad Request
 */
public class SkillValidationException extends SkillException {

    public SkillValidationException(String message) {
        super(400, message);
    }
}

