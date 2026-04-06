package org.dragon.skill.dto;

import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.SkillVisibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Skill 列表摘要视图对象。
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillSummaryVO {

    private String id;

    /** 已发布的版本号 */
    private Integer publishedVersion;

    private String name;
    private String description;
    private SkillCategory category;
    private SkillVisibility visibility;
    private SkillStatus status;

    private Long creatorId;
    private String creatorName;

    private LocalDateTime createdAt;

    /** 标签列表 */
    private List<String> tags;
}
