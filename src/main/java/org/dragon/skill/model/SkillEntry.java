package org.dragon.skill.model;

import lombok.Value;

import java.util.Map;

/**
 * 完整解析后的技能条目，包含 frontmatter、元数据和调用策略。
 *
 * @since 1.0
 */
@Value
public class SkillEntry {
    Skill skill;
    Map<String, String> frontmatter;
    SkillMetadata metadata;
    SkillInvocationPolicy invocation;
}
