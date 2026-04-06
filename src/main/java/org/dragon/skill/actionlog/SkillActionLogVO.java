package org.dragon.skill.actionlog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.skill.enums.SkillActionType;

import java.time.LocalDateTime;

/**
 * Skill 操作日志 VO，用于 API 返回。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillActionLogVO {

    private String id;
    private String skillId;
    private String skillName;
    private SkillActionType actionType;
    private String actionLabel;
    private String content;
    private Long operatorId;
    private String operatorName;
    private Integer version;
    private LocalDateTime createdAt;

    /**
     * 从领域对象转换为 VO。
     */
    public static SkillActionLogVO fromDomain(SkillActionLog log) {
        return SkillActionLogVO.builder()
                .id(log.getId())
                .skillId(log.getSkillId())
                .skillName(log.getSkillName())
                .actionType(log.getActionType())
                .actionLabel(log.getActionType() != null ? log.getActionType().getLabel() : null)
                .content(log.getContent())
                .operatorId(log.getOperatorId())
                .operatorName(log.getOperatorName())
                .version(log.getVersion())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
