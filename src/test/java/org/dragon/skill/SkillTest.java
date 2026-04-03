package org.dragon.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.dragon.skill.model.Skill;
import org.dragon.skill.model.SkillMetadata;

/**
 * Skill Entity 数据模型测试。
 * 测试 org.dragon.skill.model.Skill 实体类的基本功能。
 */
class SkillTest {

    @Test
    void testSkillCreation() {
        Skill skill = Skill.builder()
                .id(1L)
                .name("test-skill")
                .description("A test skill")
                .build();

        assertNotNull(skill);
        assertEquals(1L, skill.getId());
        assertEquals("test-skill", skill.getName());
        assertEquals("A test skill", skill.getDescription());
    }

    @Test
    void testSkillWithMetadata() {
        SkillMetadata metadata = SkillMetadata.builder()
                .always(true)
                .build();

        Skill skill = Skill.builder()
                .id(2L)
                .name("skill-with-metadata")
                .description("Skill with metadata")
                .source(org.dragon.skill.model.SkillSource.WORKSPACE)
                .build();

        assertNotNull(skill);
        assertEquals(true, metadata.getAlways());
    }

    @Test
    void testSkillSetters() {
        Skill skill = new Skill();
        skill.setId(3L);
        skill.setName("setter-test");
        skill.setDescription("Testing setters");

        assertEquals(3L, skill.getId());
        assertEquals("setter-test", skill.getName());
        assertEquals("Testing setters", skill.getDescription());
    }
}
