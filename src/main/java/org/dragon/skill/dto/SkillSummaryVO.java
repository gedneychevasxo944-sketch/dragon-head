package org.dragon.skill.dto;

import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.SkillVisibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Skill 列表摘要视图对象（不含大字段 content）。
 *
 * <p>用于分页列表接口（{@code GET /api/skills}），
 * 刻意省略 content / aliases / allowedTools 等大字段，降低传输量。
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillSummaryVO {

    private String skillId;
    private Integer version;
    private String name;
    private String displayName;
    private String description;
    private String whenToUse;
    private String argumentHint;
    private SkillCategory category;
    private SkillVisibility visibility;
    private SkillStatus status;
    private ExecutionContext executionContext;

    private Long creatorId;
    private String creatorName;

    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;

    /** 调用总次数（来自 usage 聚合，可选） */
    private Long totalUsageCount;
}

