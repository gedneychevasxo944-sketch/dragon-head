package org.dragon.skill.runtime;

import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.enums.SkillCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SkillFilter 单元测试
 *
 * <p>测试动态可见性过滤逻辑：
 * <ol>
 *   <li>过滤 disableModelInvocation=true 的 Skill</li>
 *   <li>isEnabled 扩展点（始终返回 true）</li>
 * </ol>
 */
class SkillFilterTest {

    private SkillFilter skillFilter;

    @BeforeEach
    void setUp() {
        skillFilter = new SkillFilter();
    }

    /**
     * 测试正常 Skill 被保留
     */
    @Test
    void testFilter_KeepsNormalSkills() {
        List<SkillRuntime> skills = List.of(
                createSkill("skill-1", false),
                createSkill("skill-2", false)
        );

        List<SkillRuntime> result = skillFilter.filter(skills);

        assertEquals(2, result.size());
    }

    /**
     * 测试过滤 disableModelInvocation=true 的 Skill
     */
    @Test
    void testFilter_FiltersDisabledModelInvocation() {
        List<SkillRuntime> skills = List.of(
                createSkill("skill-1", false),
                createSkill("skill-2", true),  // 会被过滤
                createSkill("skill-3", false)
        );

        List<SkillRuntime> result = skillFilter.filter(skills);

        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(s -> s.getName().equals("skill-2")));
    }

    /**
     * 测试所有 Skill 都被过滤的情况
     */
    @Test
    void testFilter_AllFiltered() {
        List<SkillRuntime> skills = List.of(
                createSkill("skill-1", true),
                createSkill("skill-2", true)
        );

        List<SkillRuntime> result = skillFilter.filter(skills);

        assertEquals(0, result.size());
    }

    /**
     * 测试空列表
     */
    @Test
    void testFilter_EmptyList() {
        List<SkillRuntime> result = skillFilter.filter(List.of());
        assertEquals(0, result.size());
    }

    // ==================== 辅助方法 ====================

    private SkillRuntime createSkill(String name, boolean disableModelInvocation) {
        return SkillRuntime.builder()
                .name(name)
                .skillId("skill-id-" + name)
                .version(1)
                .category(SkillCategory.CODER)
                .executionContext(ExecutionContext.INLINE)
                .disableModelInvocation(disableModelInvocation)
                .userInvocable(true)
                .content("# Skill content")
                .build();
    }
}
