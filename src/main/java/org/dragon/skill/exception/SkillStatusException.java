package org.dragon.skill.exception;

import org.dragon.skill.enums.SkillStatus;

/**
 * 技能状态不允许当前操作异常。
 * HTTP 状态码：409 Conflict
 *
 * 例如：对 active 状态的技能执行"发布"操作。
 */
public class SkillStatusException extends SkillException {

    public SkillStatusException(SkillStatus currentStatus, SkillStatus requiredStatus, String operation) {
        super(409, String.format(
            "操作 [%s] 要求状态为 [%s]，当前状态为 [%s]",
            operation,
            requiredStatus != null ? requiredStatus.getValue() : "unknown",
            currentStatus  != null ? currentStatus.getValue()  : "unknown"
        ));
    }

    public SkillStatusException(String message) {
        super(409, message);
    }
}

