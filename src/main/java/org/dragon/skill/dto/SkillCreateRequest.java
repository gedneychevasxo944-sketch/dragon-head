package org.dragon.skill.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.model.SkillSource;

import java.util.List;

/**
 * 创建 Skill 请求 DTO。
 *
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillCreateRequest {
    /** 来源，默认 WORKSPACE */
    @Builder.Default
    SkillSource source = SkillSource.WORKSPACE;

    @Builder.Default
    SkillCategory category = SkillCategory.GENERAL;

    /** 标签列表 */
    List<String> tags;

    /** 管理页面填写的简介 */
    String description;

    /**
     * 归属的工作空间 ID。
     * WORKSPACE 来源的 Skill 必须指定（不能为 0）。
     * BUILTIN 来源的 Skill 固定为 0，前端无需传入。
     */
    Long workspaceId;
}
