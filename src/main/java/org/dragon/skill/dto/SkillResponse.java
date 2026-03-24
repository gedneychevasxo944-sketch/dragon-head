package org.dragon.skill.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillLifecycleState;
import org.dragon.skill.model.SkillSource;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Skill 响应 DTO（管理视图）。
 *
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillResponse {
    private Long id;
    private String name;
    private SkillSource source;
    private SkillCategory category;
    private Integer version;
    private List<String> tags;
    private String description;
    private String skillDir;
    private SkillLifecycleState lifecycleState;
    private String loadError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
