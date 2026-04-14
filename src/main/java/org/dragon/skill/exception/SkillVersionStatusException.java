package org.dragon.skill.exception;

import org.dragon.skill.enums.SkillVersionStatus;

/**
 * 技能版本状态不允许当前操作异常。
 * HTTP 状态码：409 Conflict
 *
 * 例如：对 published 状态的版本执行"发布"操作。
 */
public class SkillVersionStatusException extends SkillException {

    public SkillVersionStatusException(SkillVersionStatus currentStatus, SkillVersionStatus requiredStatus, String operation) {
        super(409, String.format(
            "操作 [%s] 要求版本状态为 [%s]，当前状态为 [%s]",
            operation,
            requiredStatus != null ? requiredStatus.getValue() : "unknown",
            currentStatus  != null ? currentStatus.getValue()  : "unknown"
        ));
    }

    public SkillVersionStatusException(String message) {
        super(409, message);
    }
}