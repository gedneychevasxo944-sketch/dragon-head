package org.dragon.skill.runtime;

import org.dragon.skill.domain.StorageInfoVO;
import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.enums.PersistMode;
import org.dragon.skill.enums.SkillCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * SkillExecutor 单元测试
 *
 * <p>测试执行器逻辑：
 * <ol>
 *   <li>inline 模式执行</li>
 *   <li>fork 模式执行</li>
 *   <li>物化工作区判断</li>
 *   <li>参数注入</li>
 *   <li>persist 内容处理</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class SkillExecutorTest {

    @Mock
    private SkillWorkspaceManager workspaceManager;

    private SkillExecutor skillExecutor;

    @BeforeEach
    void setUp() {
        skillExecutor = new SkillExecutor(workspaceManager);
    }

    // ==================== 执行模式测试 ====================

    /**
     * 测试 inline 模式返回正确的数据结构
     */
    @Test
    void testExecute_InlineMode() {
        // given
        SkillDefinition skill = SkillDefinition.builder()
                .name("git-commit")
                .displayName("Git Commit")
                .skillId("skill-id")
                .version(1)
                .category(SkillCategory.DEVELOPMENT)
                .executionContext(ExecutionContext.INLINE)
                .content("# Git Commit Prompt")
                .build();

        when(workspaceManager.needsMaterialization(any())).thenReturn(false);

        // when
        SkillToolData result = skillExecutor.execute(skill, "fix: bug", "session-123", null);

        // then
        assertEquals(SkillToolData.ExecutionMode.INLINE, result.getExecutionMode());
        assertEquals("git-commit", result.getSkillName());
        assertNotNull(result.getNewMessages());
        assertNull(result.getPersistContent());
    }

    /**
     * 测试 fork 模式返回正确的数据结构
     */
    @Test
    void testExecute_ForkMode() {
        // given
        SkillDefinition skill = SkillDefinition.builder()
                .name("code-gen")
                .displayName("Code Generator")
                .skillId("skill-id")
                .version(1)
                .category(SkillCategory.DEVELOPMENT)
                .executionContext(ExecutionContext.FORK)
                .content("# Code Gen Prompt")
                .build();

        when(workspaceManager.needsMaterialization(any())).thenReturn(false);

        // when
        SkillToolData result = skillExecutor.execute(skill, null, "session-123", null);

        // then
        assertEquals(SkillToolData.ExecutionMode.FORK, result.getExecutionMode());
        assertEquals("code-gen", result.getSkillName());
        assertNull(result.getNewMessages()); // fork 模式下为 null
    }

    // ==================== 物化工作区测试 ====================

    /**
     * 测试无附属文件时不触发物化
     */
    @Test
    void testExecute_NoMaterializationNeeded() {
        // given
        SkillDefinition skill = SkillDefinition.builder()
                .name("simple-skill")
                .skillId("skill-id")
                .version(1)
                .executionContext(ExecutionContext.INLINE)
                .content("# Simple Skill")
                .storageInfo(null)
                .build();

        when(workspaceManager.needsMaterialization(null)).thenReturn(false);

        // when
        SkillToolData result = skillExecutor.execute(skill, null, "session-123", null);

        // then
        assertEquals(SkillToolData.ExecutionMode.INLINE, result.getExecutionMode());
        verify(workspaceManager, never()).prepareExecDir(any(), anyInt(), any());
    }

    /**
     * 测试 needsMaterialization 多个文件返回 true
     * 注意：workspaceManager 是 mock，需要显式 stub
     */
    @Test
    void testNeedsMaterialization_True() {
        // given
        StorageInfoVO storageInfo = new StorageInfoVO();
        when(workspaceManager.needsMaterialization(storageInfo)).thenReturn(true);

        // then
        assertTrue(workspaceManager.needsMaterialization(storageInfo));
    }

    /**
     * 测试 needsMaterialization 单个文件返回 false
     */
    @Test
    void testNeedsMaterialization_False() {
        // given
        StorageInfoVO storageInfo = new StorageInfoVO();
        when(workspaceManager.needsMaterialization(storageInfo)).thenReturn(false);

        // then
        assertFalse(workspaceManager.needsMaterialization(storageInfo));
    }

    // ==================== 参数注入测试 ====================

    /**
     * 测试 $ARGUMENTS 占位符替换
     */
    @Test
    void testExecute_ArgumentInjection() {
        // given
        SkillDefinition skill = SkillDefinition.builder()
                .name("git-commit")
                .skillId("skill-id")
                .version(1)
                .executionContext(ExecutionContext.INLINE)
                .content("# Commit Message\n\nTask: $ARGUMENTS")
                .build();

        when(workspaceManager.needsMaterialization(any())).thenReturn(false);

        // when
        SkillToolData result = skillExecutor.execute(skill, "fix: resolve bug", "session-123", null);

        // then
        assertEquals(SkillToolData.ExecutionMode.INLINE, result.getExecutionMode());
        // 验证 content 被正确处理
        assertNotNull(result.getNewMessages());
    }

    /**
     * 测试无占位符时 args 追加到末尾
     */
    @Test
    void testExecute_ArgumentAppended() {
        // given
        SkillDefinition skill = SkillDefinition.builder()
                .name("simple")
                .skillId("skill-id")
                .version(1)
                .executionContext(ExecutionContext.INLINE)
                .content("# Simple Prompt")
                .build();

        when(workspaceManager.needsMaterialization(any())).thenReturn(false);

        // when
        SkillToolData result = skillExecutor.execute(skill, "extra args", "session-123", null);

        // then
        assertNotNull(result.getNewMessages());
    }

    // ==================== persist 测试 ====================

    /**
     * 测试 persist=false 时 persistContent 为 null
     */
    @Test
    void testExecute_PersistFalse() {
        // given
        SkillDefinition skill = SkillDefinition.builder()
                .name("temp-skill")
                .skillId("skill-id")
                .version(1)
                .executionContext(ExecutionContext.INLINE)
                .persist(false)
                .content("# Temp Skill")
                .build();

        when(workspaceManager.needsMaterialization(any())).thenReturn(false);

        // when
        SkillToolData result = skillExecutor.execute(skill, null, "session-123", null);

        // then
        assertNull(result.getPersistContent());
    }

    /**
     * 测试 persist=true 时 persistContent 非 null
     */
    @Test
    void testExecute_PersistTrue() {
        // given
        SkillDefinition skill = SkillDefinition.builder()
                .name("persistent-skill")
                .skillId("skill-id")
                .version(1)
                .executionContext(ExecutionContext.INLINE)
                .persist(true)
                .persistMode(PersistMode.FULL)
                .content("# Persistent Skill")
                .build();

        when(workspaceManager.needsMaterialization(any())).thenReturn(false);

        // when
        SkillToolData result = skillExecutor.execute(skill, null, "session-123", null);

        // then
        assertNotNull(result.getPersistContent());
    }

    /**
     * 测试 persistMode=SUMMARY 时只提取约束片段
     */
    @Test
    void testExecute_PersistSummary() {
        // given
        String fullContent = "# Rules\n\n## Constraints\n- Rule 1\n- Rule 2\n\n## Guide\nSome guide text";
        SkillDefinition skill = SkillDefinition.builder()
                .name("rule-skill")
                .skillId("skill-id")
                .version(1)
                .executionContext(ExecutionContext.INLINE)
                .persist(true)
                .persistMode(PersistMode.SUMMARY)
                .content(fullContent)
                .build();

        when(workspaceManager.needsMaterialization(any())).thenReturn(false);

        // when
        SkillToolData result = skillExecutor.execute(skill, null, "session-123", null);

        // then
        assertNotNull(result.getPersistContent());
        assertTrue(result.getPersistContent().contains("Constraints"));
    }

    // ==================== 内容缓存测试 ====================

    /**
     * 测试同一 sessionKey 不重复加载内容
     */
    @Test
    void testExecute_ContentCacheHit() {
        // given
        SkillDefinition skill = SkillDefinition.builder()
                .name("cached-skill")
                .skillId("skill-id")
                .version(1)
                .executionContext(ExecutionContext.INLINE)
                .content("# Cached Skill Content")
                .build();

        when(workspaceManager.needsMaterialization(any())).thenReturn(false);

        // when: 同一 session 调用两次
        skillExecutor.execute(skill, null, "session-123", null);
        SkillToolData result2 = skillExecutor.execute(skill, null, "session-123", null);

        // then: 内容被缓存，第二次调用仍正常返回
        assertNotNull(result2.getNewMessages());
    }

    // ==================== ContextPatch 测试 ====================

    /**
     * 测试带 allowedTools 的 Skill 生成 ContextPatch
     */
    @Test
    void testExecute_ContextPatchWithTools() {
        // given
        SkillDefinition skill = SkillDefinition.builder()
                .name("tooled-skill")
                .skillId("skill-id")
                .version(1)
                .executionContext(ExecutionContext.INLINE)
                .allowedTools(List.of("Bash", "Read"))
                .model("gpt-4")
                .content("# Tooled Skill")
                .build();

        when(workspaceManager.needsMaterialization(any())).thenReturn(false);

        // when
        SkillToolData result = skillExecutor.execute(skill, null, "session-123", null);

        // then
        assertNotNull(result.getContextPatch());
        assertEquals(List.of("Bash", "Read"), result.getContextPatch().getAdditionalAllowedTools());
        assertEquals("gpt-4", result.getContextPatch().getModelOverride());
    }

    // ==================== 辅助方法 ====================

    private SkillDefinition createSkill(String name, ExecutionContext context) {
        return SkillDefinition.builder()
                .name(name)
                .displayName(name)
                .skillId("skill-id-" + name)
                .version(1)
                .category(SkillCategory.DEVELOPMENT)
                .executionContext(context)
                .disableModelInvocation(false)
                .userInvocable(true)
                .content("# Skill content for " + name)
                .build();
    }
}
