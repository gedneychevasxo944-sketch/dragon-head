package org.dragon.skill.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.skill.enums.SkillCategory;

import java.util.List;

/**
 * 更新 Skill 请求 DTO。
 * 所有字段均为可选，null 表示不更新。
 *
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillUpdateRequest {
    private SkillCategory category;
    private List<String> tags;
    private String description;
}
