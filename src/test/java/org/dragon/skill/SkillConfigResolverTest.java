package org.dragon.skill;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillConfigResolverTest {

    @Test
    void testShouldIncludeSkill_AlwaysTrue() {
        SkillTypes.Skill skill = new SkillTypes.Skill("test", "desc", SkillTypes.SkillSource.WORKSPACE, "path", "dir", "content");
        SkillTypes.SkillMetadata metadata = new SkillTypes.SkillMetadata(true, null, null, null, null, null, null, null);
        SkillTypes.SkillEntry entry = new SkillTypes.SkillEntry(skill, null, metadata, null);

        assertTrue(SkillConfigResolver.shouldIncludeSkill(entry, null));
    }

    @Test
    void testShouldIncludeSkill_OsMismatch() {
        SkillTypes.Skill skill = new SkillTypes.Skill("test", "desc", SkillTypes.SkillSource.WORKSPACE, "path", "dir", "content");
        // Assuming tests run on something other than TempleOS
        SkillTypes.SkillMetadata metadata = new SkillTypes.SkillMetadata(null, null, null, null, null, Collections.singletonList("templeos"), null, null);
        SkillTypes.SkillEntry entry = new SkillTypes.SkillEntry(skill, null, metadata, null);

        assertFalse(SkillConfigResolver.shouldIncludeSkill(entry, null));
    }

    @Test
    void testShouldIncludeSkill_MissingBinary() {
        SkillTypes.Skill skill = new SkillTypes.Skill("test", "desc", SkillTypes.SkillSource.WORKSPACE, "path", "dir", "content");
        SkillTypes.SkillRequires requires = new SkillTypes.SkillRequires(Collections.singletonList("non_existent_binary_12345"), null, null, null);
        SkillTypes.SkillMetadata metadata = new SkillTypes.SkillMetadata(null, null, null, null, null, null, requires, null);
        SkillTypes.SkillEntry entry = new SkillTypes.SkillEntry(skill, null, metadata, null);

        assertFalse(SkillConfigResolver.shouldIncludeSkill(entry, null));
    }

    @Test
    void testShouldIncludeSkill_MissingEnv() {
        SkillTypes.Skill skill = new SkillTypes.Skill("test", "desc", SkillTypes.SkillSource.WORKSPACE, "path", "dir", "content");
        SkillTypes.SkillRequires requires = new SkillTypes.SkillRequires(null, null, Collections.singletonList("NON_EXISTENT_ENV_VAR_12345"), null);
        SkillTypes.SkillMetadata metadata = new SkillTypes.SkillMetadata(null, null, null, null, null, null, requires, null);
        SkillTypes.SkillEntry entry = new SkillTypes.SkillEntry(skill, null, metadata, null);

        assertFalse(SkillConfigResolver.shouldIncludeSkill(entry, null));
    }
}
