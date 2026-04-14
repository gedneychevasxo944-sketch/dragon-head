package org.dragon.skill.runtime;

import org.dragon.config.service.ConfigApplication;
import org.dragon.skill.config.SkillPermissionConfig;
import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.enums.SkillCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * SkillPermissionChecker 单元测试
 *
 * <p>测试四步权限检查逻辑：
 * <ol>
 *   <li>deny 规则（黑名单）</li>
 *   <li>allow 规则（白名单）</li>
 *   <li>安全属性白名单</li>
 *   <li>ASK 策略</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class SkillPermissionCheckerTest {

    @Mock
    private ConfigApplication configApplication;

    @Mock
    private SkillPermissionConfig permissionConfig;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SkillPermissionChecker checker;
    private AgentContext agentContext;

    @BeforeEach
    void setUp() {
        checker = new SkillPermissionChecker(configApplication, permissionConfig, eventPublisher);
        agentContext = AgentContext.builder()
                .characterId("char-1")
                .workspaceId("ws-1")
                .agentId("agent-1")
                .build();
    }

    // ==================== deny 规则测试 ====================

    /**
     * 测试 deny 规则 - 精确匹配
     */
    @Test
    void testCheck_DenyRuleExactMatch() {
        when(permissionConfig.getDenyRules()).thenReturn(List.of("dangerous-op", "deploy-prod"));
        when(permissionConfig.getAllowRules()).thenReturn(List.of());

        SkillDefinition skill = createSkill("dangerous-op");
        SkillPermissionResult result = checker.check(skill, agentContext);

        assertTrue(result.isDeny());
        assertEquals("dangerous-op", result.getMatchedRule());
    }

    /**
     * 测试 deny 规则 - 前缀通配
     */
    @Test
    void testCheck_DenyRuleWildcardMatch() {
        when(permissionConfig.getDenyRules()).thenReturn(List.of("deploy:*"));
        when(permissionConfig.getAllowRules()).thenReturn(List.of());

        SkillDefinition skill = createSkill("deploy-check");
        SkillPermissionResult result = checker.check(skill, agentContext);

        assertTrue(result.isDeny());
        assertEquals("deploy:*", result.getMatchedRule());
    }

    /**
     * 测试 deny 规则不匹配
     */
    @Test
    void testCheck_DenyRuleNotMatch() {
        when(permissionConfig.getDenyRules()).thenReturn(List.of("dangerous-op"));
        when(permissionConfig.getAllowRules()).thenReturn(List.of());

        SkillDefinition skill = createSkill("safe-skill");
        SkillPermissionResult result = checker.check(skill, agentContext);

        assertFalse(result.isDeny());
    }

    // ==================== allow 规则测试 ====================

    /**
     * 测试 allow 规则 - 精确匹配
     */
    @Test
    void testCheck_AllowRuleExactMatch() {
        when(permissionConfig.getDenyRules()).thenReturn(List.of());
        when(permissionConfig.getAllowRules()).thenReturn(List.of("git-commit", "code-review"));

        SkillDefinition skill = createSkill("git-commit");
        SkillPermissionResult result = checker.check(skill, agentContext);

        assertTrue(result.isAllow());
        assertEquals("git-commit", result.getMatchedRule());
    }

    /**
     * 测试 allow 规则 - 前缀通配
     */
    @Test
    void testCheck_AllowRuleWildcardMatch() {
        when(permissionConfig.getDenyRules()).thenReturn(List.of());
        when(permissionConfig.getAllowRules()).thenReturn(List.of("review:*"));

        SkillDefinition skill = createSkill("review-code");
        SkillPermissionResult result = checker.check(skill, agentContext);

        assertTrue(result.isAllow());
        assertEquals("review:*", result.getMatchedRule());
    }

    // ==================== 安全属性白名单测试 ====================

    /**
     * 测试只含安全属性的 Skill 自动放行
     */
    @Test
    void testCheck_SafeProperties_Allow() {
        when(permissionConfig.getDenyRules()).thenReturn(List.of());
        when(permissionConfig.getAllowRules()).thenReturn(List.of());

        // 只含安全属性
        SkillDefinition skill = SkillDefinition.builder()
                .name("git-commit")
                .description("Generate commit message")
                .category(SkillCategory.CODER)
                .executionContext(ExecutionContext.INLINE)
                .build();

        SkillPermissionResult result = checker.check(skill, agentContext);

        assertTrue(result.isAllow());
        assertNull(result.getMatchedRule()); // 白名单自动放行，matchedRule 为 null
    }

    /**
     * 测试含 allowedTools 非空的 Skill 需要权限
     */
    @Test
    void testCheck_UnsafeProperties_AllowedTools() {
        when(permissionConfig.getDenyRules()).thenReturn(List.of());
        when(permissionConfig.getAllowRules()).thenReturn(List.of());
        when(permissionConfig.getAskStrategy()).thenReturn("auto-deny");

        SkillDefinition skill = SkillDefinition.builder()
                .name("deploy-check")
                .allowedTools(List.of("Bash", "Read"))
                .executionContext(ExecutionContext.INLINE)
                .build();

        SkillPermissionResult result = checker.check(skill, agentContext);

        assertTrue(result.isDeny()); // auto-deny 策略
    }

    /**
     * 测试 executionContext=fork 需要权限
     */
    @Test
    void testCheck_UnsafeProperties_ForkContext() {
        when(permissionConfig.getDenyRules()).thenReturn(List.of());
        when(permissionConfig.getAllowRules()).thenReturn(List.of());
        when(permissionConfig.getAskStrategy()).thenReturn("auto-deny");

        SkillDefinition skill = SkillDefinition.builder()
                .name("code-gen")
                .executionContext(ExecutionContext.FORK)
                .build();

        SkillPermissionResult result = checker.check(skill, agentContext);

        assertTrue(result.isDeny());
    }

    // ==================== ASK 策略测试 ====================

    /**
     * 测试 auto-deny 策略
     */
    @Test
    void testCheck_AutoDenyStrategy() {
        when(permissionConfig.getDenyRules()).thenReturn(List.of());
        when(permissionConfig.getAllowRules()).thenReturn(List.of());
        when(permissionConfig.getAskStrategy()).thenReturn("auto-deny");

        SkillDefinition skill = SkillDefinition.builder()
                .name("deploy-check")
                .allowedTools(List.of("Bash"))
                .executionContext(ExecutionContext.INLINE)
                .build();

        SkillPermissionResult result = checker.check(skill, agentContext);

        assertTrue(result.isDeny());
        assertFalse(result.isAsk());
    }

    // ==================== 规则匹配边界测试 ====================

    /**
     * 测试规则前导斜杠被忽略
     */
    @Test
    void testCheck_RuleWithLeadingSlash() {
        when(permissionConfig.getDenyRules()).thenReturn(List.of("/dangerous-op"));
        when(permissionConfig.getAllowRules()).thenReturn(List.of());

        SkillDefinition skill = createSkill("dangerous-op");
        SkillPermissionResult result = checker.check(skill, agentContext);

        assertTrue(result.isDeny());
    }

    /**
     * 测试空规则列表
     */
    @Test
    void testCheck_EmptyRules() {
        when(permissionConfig.getDenyRules()).thenReturn(List.of());
        when(permissionConfig.getAllowRules()).thenReturn(List.of());

        SkillDefinition skill = createSkill("any-skill");
        SkillPermissionResult result = checker.check(skill, agentContext);

        // 无规则命中，走白名单或 ask
        assertNotNull(result);
    }

    // ==================== 辅助方法 ====================

    private SkillDefinition createSkill(String name) {
        return SkillDefinition.builder()
                .name(name)
                .skillId("skill-id-" + name)
                .version(1)
                .category(SkillCategory.CODER)
                .executionContext(ExecutionContext.INLINE)
                .build();
    }
}
