package org.dragon.skill;

<<<<<<< HEAD
import org.dragon.skill.SkillTypes.Skill;
import org.dragon.skill.SkillTypes.SkillEntry;
import org.dragon.skill.SkillTypes.SkillMetadata;
import org.dragon.skill.SkillTypes.SkillRequires;
import org.dragon.skill.SkillTypes.SkillSource;
=======
>>>>>>> origin/main
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillConfigResolverTest {

    @Test
    void testShouldIncludeSkill_AlwaysTrue() {
<<<<<<< HEAD
        Skill skill = new Skill("test", "desc", SkillSource.WORKSPACE, "path", "dir", "content");
        SkillMetadata metadata = new SkillMetadata(true, null, null, null, null, null, null, null);
        SkillEntry entry = new SkillEntry(skill, null, metadata, null);
=======
        SkillTypes.Skill skill = new SkillTypes.Skill("test", "desc", SkillTypes.SkillSource.WORKSPACE, "path", "dir", "content");
        SkillTypes.SkillMetadata metadata = new SkillTypes.SkillMetadata(true, null, null, null, null, null, null, null);
        SkillTypes.SkillEntry entry = new SkillTypes.SkillEntry(skill, null, metadata, null);
>>>>>>> origin/main

        assertTrue(SkillConfigResolver.shouldIncludeSkill(entry, null));
    }

    @Test
    void testShouldIncludeSkill_OsMismatch() {
<<<<<<< HEAD
        Skill skill = new Skill("test", "desc", SkillSource.WORKSPACE, "path", "dir", "content");
        // Assuming tests run on something other than TempleOS
        SkillMetadata metadata = new SkillMetadata(null, null, null, null, null, Collections.singletonList("templeos"), null, null);
        SkillEntry entry = new SkillEntry(skill, null, metadata, null);
=======
        SkillTypes.Skill skill = new SkillTypes.Skill("test", "desc", SkillTypes.SkillSource.WORKSPACE, "path", "dir", "content");
        // Assuming tests run on something other than TempleOS
        SkillTypes.SkillMetadata metadata = new SkillTypes.SkillMetadata(null, null, null, null, null, Collections.singletonList("templeos"), null, null);
        SkillTypes.SkillEntry entry = new SkillTypes.SkillEntry(skill, null, metadata, null);
>>>>>>> origin/main

        assertFalse(SkillConfigResolver.shouldIncludeSkill(entry, null));
    }

    @Test
    void testShouldIncludeSkill_MissingBinary() {
<<<<<<< HEAD
        Skill skill = new Skill("test", "desc", SkillSource.WORKSPACE, "path", "dir", "content");
        SkillRequires requires = new SkillRequires(Collections.singletonList("non_existent_binary_12345"), null, null, null);
        SkillMetadata metadata = new SkillMetadata(null, null, null, null, null, null, requires, null);
        SkillEntry entry = new SkillEntry(skill, null, metadata, null);
=======
        SkillTypes.Skill skill = new SkillTypes.Skill("test", "desc", SkillTypes.SkillSource.WORKSPACE, "path", "dir", "content");
        SkillTypes.SkillRequires requires = new SkillTypes.SkillRequires(Collections.singletonList("non_existent_binary_12345"), null, null, null);
        SkillTypes.SkillMetadata metadata = new SkillTypes.SkillMetadata(null, null, null, null, null, null, requires, null);
        SkillTypes.SkillEntry entry = new SkillTypes.SkillEntry(skill, null, metadata, null);
>>>>>>> origin/main

        assertFalse(SkillConfigResolver.shouldIncludeSkill(entry, null));
    }

    @Test
    void testShouldIncludeSkill_MissingEnv() {
<<<<<<< HEAD
        Skill skill = new Skill("test", "desc", SkillSource.WORKSPACE, "path", "dir", "content");
        SkillRequires requires = new SkillRequires(null, null, Collections.singletonList("NON_EXISTENT_ENV_VAR_12345"), null);
        SkillMetadata metadata = new SkillMetadata(null, null, null, null, null, null, requires, null);
        SkillEntry entry = new SkillEntry(skill, null, metadata, null);
=======
        SkillTypes.Skill skill = new SkillTypes.Skill("test", "desc", SkillTypes.SkillSource.WORKSPACE, "path", "dir", "content");
        SkillTypes.SkillRequires requires = new SkillTypes.SkillRequires(null, null, Collections.singletonList("NON_EXISTENT_ENV_VAR_12345"), null);
        SkillTypes.SkillMetadata metadata = new SkillTypes.SkillMetadata(null, null, null, null, null, null, requires, null);
        SkillTypes.SkillEntry entry = new SkillTypes.SkillEntry(skill, null, metadata, null);
>>>>>>> origin/main

        assertFalse(SkillConfigResolver.shouldIncludeSkill(entry, null));
    }
}
