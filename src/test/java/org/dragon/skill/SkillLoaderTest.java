package org.dragon.skill;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillLoaderTest {

    @Test
    void testLoadSkillsFromDir() {
        Path testDir = Paths.get("src/test/resources/skills");
        List<SkillTypes.Skill> skills = SkillLoader.loadSkillsFromDir(testDir, SkillTypes.SkillSource.WORKSPACE);

        assertNotNull(skills);
        assertEquals(2, skills.size());

        SkillTypes.Skill skill1 = skills.stream().filter(s -> s.getName().equals("test-skill-1")).findFirst().orElse(null);
        assertNotNull(skill1);
        assertEquals("A basic test skill", skill1.getDescription());
        assertEquals(SkillTypes.SkillSource.WORKSPACE, skill1.getSource());
        assertTrue(skill1.getContent().contains("This is the body of test skill 1."));

        SkillTypes.Skill skill2 = skills.stream().filter(s -> s.getName().equals("test-skill-2")).findFirst().orElse(null);
        assertNotNull(skill2);
        assertEquals("A skill with requirements", skill2.getDescription());
    }

    @Test
    void testBuildSkillsPrompt() {
        Path testDir = Paths.get("src/test/resources/skills");
        List<SkillTypes.Skill> skills = SkillLoader.loadSkillsFromDir(testDir, SkillTypes.SkillSource.WORKSPACE);

        // Mock SkillEntry creation for testing prompt builder
        List<SkillTypes.SkillEntry> entries = skills.stream().map(s -> new SkillTypes.SkillEntry(s, null, null, null)).collect(Collectors.toList());

        String prompt = SkillLoader.buildSkillsPrompt(entries);

        assertNotNull(prompt);
        assertTrue(prompt.contains("<skill name=\"test-skill-1\">"));
        assertTrue(prompt.contains("This is the body of test skill 1."));
        assertTrue(prompt.contains("</skill>"));
        assertTrue(prompt.contains("<skill name=\"test-skill-2\">"));
    }
}
