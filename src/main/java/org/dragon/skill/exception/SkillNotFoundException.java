package org.dragon.skill.exception;

/**
 * 技能不存在或已删除异常。
 * HTTP 状态码：404 Not Found
 */
public class SkillNotFoundException extends SkillException {

    public SkillNotFoundException(String skillId) {
        super(404, "技能不存在或已删除: " + skillId);
    }
}

