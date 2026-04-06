package org.dragon.skill.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Skill 绑定信息（用于 SkillDetailVO 内）。
 *
 * <p>仅包含绑定目标信息，不重复 skill 元数据。
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillBindingInfoVO {

    /** 绑定类型：character / workspace / character_workspace */
    private String bindingType;

    /** Character ID（bindingType 含 character 时有值） */
    private String characterId;

    /** Workspace ID（bindingType 含 workspace 时有值） */
    private String workspaceId;

    /** 绑定时间 */
    private LocalDateTime createdAt;
}