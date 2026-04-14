package org.dragon.skill.runtime;

import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.util.SkillDirectoryBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SkillDirectoryBuilder 单元测试
 *
 * <p>测试静态工具类：
 * <ol>
 *   <li>buildDirectoryPrompt</li>
 *   <li>buildPersistedSkillPrompt</li>
 *   <li>extractSummary</li>
 * </ol>
 */
class SkillDirectoryBuilderTest {

    // ==================== buildDirectoryPrompt 测试 ====================

    /**
     * 测试构建目录 prompt
     */
    @Test
    void testBuildDirectoryPrompt() {
        List<SkillDefinition> skills = List.of(
                createSkill("deploy-check", "部署前检查", "在部署前检查代码质量"),
                createSkill("git-commit", "生成 commit", null)
        );

        String result = SkillDirectoryBuilder.buildDirectoryPrompt(skills);

        assertNotNull(result);
        assertTrue(result.contains("## Available Skills"));
        assertTrue(result.contains("deploy-check"));
        assertTrue(result.contains("部署前检查"));
        assertTrue(result.contains("部署前检查代码质量"));
        assertTrue(result.contains("git-commit"));
    }

    /**
     * 测试空列表返回空字符串
     */
    @Test
    void testBuildDirectoryPrompt_Empty() {
        String result = SkillDirectoryBuilder.buildDirectoryPrompt(List.of());
        assertEquals("", result);
    }

    /**
     * 测试 null 列表返回空字符串
     */
    @Test
    void testBuildDirectoryPrompt_Null() {
        String result = SkillDirectoryBuilder.buildDirectoryPrompt(null);
        assertEquals("", result);
    }

    /**
     * 测试带参数提示的目录构建
     */
    @Test
    void testBuildDirectoryPrompt_WithArgumentHint() {
        SkillDefinition skill = SkillDefinition.builder()
                .name("git-commit")
                .argumentHint("<scope> <message>")
                .description("Generate commit message")
                .whenToUse("When you need to commit changes")
                .build();

        String result = SkillDirectoryBuilder.buildDirectoryPrompt(List.of(skill));

        assertTrue(result.contains("git-commit"));
        assertTrue(result.contains("<scope> <message>"));
        assertTrue(result.contains("Generate commit message"));
    }

    /**
     * 测试带别名的目录构建
     */
    @Test
    void testBuildDirectoryPrompt_WithAliases() {
        SkillDefinition skill = SkillDefinition.builder()
                .name("git-commit")
                .aliases(List.of("gc", "commit"))
                .description("Generate commit message")
                .build();

        String result = SkillDirectoryBuilder.buildDirectoryPrompt(List.of(skill));

        assertTrue(result.contains("git-commit"));
        // 别名在目录中不显示，只在运行时匹配
    }

    // ==================== buildPersistedSkillPrompt 测试 ====================

    /**
     * 测试构建持续留存 prompt
     */
    @Test
    void testBuildPersistedSkillPrompt() {
        Map<String, String> persisted = Map.of(
                "code-review", "## Constraints\n- Check security\n- Check performance",
                "deploy-check", "## Rules\n- Run tests first"
        );

        String result = SkillDirectoryBuilder.buildPersistedSkillPrompt(persisted);

        assertNotNull(result);
        assertTrue(result.contains("## Active Skill Constraints"));
        assertTrue(result.contains("code-review"));
        assertTrue(result.contains("deploy-check"));
        assertTrue(result.contains("<skill-context"));
        assertTrue(result.contains("</skill-context>"));
    }

    /**
     * 测试空 map 返回空字符串
     */
    @Test
    void testBuildPersistedSkillPrompt_Empty() {
        String result = SkillDirectoryBuilder.buildPersistedSkillPrompt(Map.of());
        assertEquals("", result);
    }

    /**
     * 测试 null map 返回空字符串
     */
    @Test
    void testBuildPersistedSkillPrompt_Null() {
        String result = SkillDirectoryBuilder.buildPersistedSkillPrompt(null);
        assertEquals("", result);
    }

    // ==================== extractSummary 测试 ====================

    /**
     * 测试提取 ## Constraints 部分
     */
    @Test
    void testExtractSummary_Constraints() {
        String content = "# Code Review\n\n## Constraints\n- Check security\n- Check performance\n\n## Guide\nSome guide text";

        String result = SkillDirectoryBuilder.extractSummary(content);

        assertNotNull(result);
        assertTrue(result.contains("Constraints"));
        assertTrue(result.contains("Check security"));
        assertTrue(result.contains("Check performance"));
        assertFalse(result.contains("Guide")); // Guide 在 Constraints 之后，不应包含
    }

    /**
     * 测试提取 ## Rules 部分
     */
    @Test
    void testExtractSummary_Rules() {
        String content = "# Deployment\n\n## Rules\n- Run tests first\n- Check environment\n\n## Notes\nSome notes";

        String result = SkillDirectoryBuilder.extractSummary(content);

        assertNotNull(result);
        assertTrue(result.contains("Rules"));
        assertTrue(result.contains("Run tests first"));
    }

    /**
     * 测试中文约束标题
     */
    @Test
    void testExtractSummary_ChineseHeaders() {
        String content = "# Coding Standard\n\n## 约束\n- 使用中文注释\n- 遵循命名规范\n\n## 规则\nSome rules";

        String result = SkillDirectoryBuilder.extractSummary(content);

        assertNotNull(result);
        assertTrue(result.contains("约束"));
        assertTrue(result.contains("使用中文注释"));
    }

    /**
     * 测试找不到约束标题时返回原文前500字符
     */
    @Test
    void testExtractSummary_NoHeader() {
        String content = "This is just plain content without any headers. ".repeat(50);

        String result = SkillDirectoryBuilder.extractSummary(content);

        assertNotNull(result);
        // 原文超过500字符时会截断并添加 "..."
        assertTrue(result.length() > 400); // 截断后的长度
    }

    /**
     * 测试空内容返回空字符串
     */
    @Test
    void testExtractSummary_Empty() {
        assertEquals("", SkillDirectoryBuilder.extractSummary(""));
        assertEquals("", SkillDirectoryBuilder.extractSummary(null));
    }

    /**
     * 测试只有 ## 标题的内容
     */
    @Test
    void testExtractSummary_OnlyHeader() {
        String content = "## Constraints\n- Rule 1\n- Rule 2";

        String result = SkillDirectoryBuilder.extractSummary(content);

        assertNotNull(result);
        assertTrue(result.contains("Constraints"));
        assertTrue(result.contains("Rule 1"));
    }

    // ==================== 辅助方法 ====================

    private SkillDefinition createSkill(String name, String displayName, String whenToUse) {
        return SkillDefinition.builder()
                .name(name)
                .skillId("skill-id-" + name)
                .version(1)
                .category(SkillCategory.CODER)
                .executionContext(ExecutionContext.INLINE)
                .whenToUse(whenToUse)
                .description("Description for " + name)
                .build();
    }
}
