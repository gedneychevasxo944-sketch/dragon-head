package org.dragon.skill.dto;

import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.enums.PersistMode;
import org.dragon.skill.enums.SkillEffort;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Skill 运行时配置（从 frontmatter 解析）。
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillRuntimeConfigVO {

    private List<String> aliases;

    private String whenToUse;

    private String argumentHint;

    private List<String> allowedTools;

    private String model;

    private SkillEffort effort;

    private ExecutionContext executionContext;

    private Boolean disableModelInvocation;

    private Boolean userInvocable;

    private Boolean persist;

    private PersistMode persistMode;
}