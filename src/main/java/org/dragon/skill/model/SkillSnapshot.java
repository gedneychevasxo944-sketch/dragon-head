package org.dragon.skill.model;

import lombok.Value;

import java.util.List;

/**
 * 解析后的技能快照，用于嵌入到系统提示词中。
 *
 * @since 1.0
 */
@Value
public class SkillSnapshot {
    String prompt;
    List<SkillSummary> skills;
    List<Skill> resolvedSkills;
    Integer version;
}
