package org.dragon.skill.actionlog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.skill.enums.SkillActionType;

import java.time.LocalDateTime;

/**
 * Skill 操作日志领域对象。
 *
 * <p>对应 skill_action_log 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillActionLog {

    private String id;
    private String skillId;
    private String skillName;
    private SkillActionType actionType;
    private Long operatorId;
    private String operatorName;
    private Integer version;
    private ActionDetail detail;
    private LocalDateTime createdAt;

    /**
     * 生成日志描述，用于前端展示。
     *
     * @return 操作描述，如果 detail 不为空则使用 detail.getContent()，否则使用 actionType.getLabel()
     */
    public String getContent() {
        return detail != null ? detail.getContent() : actionType.getLabel();
    }
}
