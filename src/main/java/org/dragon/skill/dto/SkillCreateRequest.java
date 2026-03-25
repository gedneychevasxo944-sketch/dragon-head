package org.dragon.skill.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillCreatorType;
import org.dragon.skill.enums.SkillVisibility;

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

    @Builder.Default
    SkillCategory category = SkillCategory.GENERAL;

    /** 标签列表 */
    List<String> tags;

    /** 管理页面填写的简介 */
    String description;

    /**
     * 可见性，默认 PUBLIC（广场公开）。
     */
    @Builder.Default
    SkillVisibility visibility = SkillVisibility.PUBLIC;

    /**
     * 创建人 ID（从登录上下文中获取，前端无需传入）。
     */
    Long creatorId;

    /**
     * 创建人类型（从登录上下文判断，前端无需传入）。
     */
    @Builder.Default
    SkillCreatorType creatorType = SkillCreatorType.PERSONAL;
}