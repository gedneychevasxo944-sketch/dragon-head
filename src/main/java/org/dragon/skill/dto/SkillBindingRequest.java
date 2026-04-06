package org.dragon.skill.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 绑定 Skill 到 Character / Workspace / Character+Workspace 的请求体。
 */
@Data
public class SkillBindingRequest {

    /**
     * 绑定类型：character / workspace / character_workspace
     */
    private String bindingType;

    /**
     * Character 主键
     */
    private String characterId;

    /**
     * Workspace 主键
     */
    private String workspaceId;

    /** 技能唯一标识（UUID） */
    @NotBlank(message = "skillId 不能为空")
    private String skillId;
}
