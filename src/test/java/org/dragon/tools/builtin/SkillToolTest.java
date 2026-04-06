package org.dragon.tools.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.runtime.*;
import org.dragon.skill.service.SkillUsageService;
import org.dragon.tools.AgentTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * SkillTool 单元测试
 *
 * <p>测试 AgentTool 接口实现：
 * <ol>
 *   <li>getName / getDescription / getParameterSchema</li>
 *   <li>execute 完整执行链路</li>
 *   <li>参数解析（skill 名称、args）</li>
 *   <li>错误处理（skill 不存在、权限拒绝等）</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class SkillToolTest {

    @Mock
    private SkillRegistry skillRegistry;

    @Mock
    private SkillFilter skillFilter;

    @Mock
    private SkillExecutor skillExecutor;

    @Mock
    private SkillPermissionChecker permissionChecker;

    @Mock
    private SkillUsageService usageService;

    private SkillTool skillTool;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        skillTool = new SkillTool();
        // 通过反射注入依赖
        try {
            var registryField = SkillTool.class.getDeclaredField("skillRegistry");
            registryField.setAccessible(true);
            registryField.set(skillTool, skillRegistry);

            var filterField = SkillTool.class.getDeclaredField("skillFilter");
            filterField.setAccessible(true);
            filterField.set(skillTool, skillFilter);

            var executorField = SkillTool.class.getDeclaredField("skillExecutor");
            executorField.setAccessible(true);
            executorField.set(skillTool, skillExecutor);

            var checkerField = SkillTool.class.getDeclaredField("permissionChecker");
            checkerField.setAccessible(true);
            checkerField.set(skillTool, permissionChecker);

            var usageField = SkillTool.class.getDeclaredField("usageService");
            usageField.setAccessible(true);
            usageField.set(skillTool, usageService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        objectMapper = new ObjectMapper();
    }

    // ==================== 接口方法测试 ====================

    @Test
    void testGetName() {
        assertEquals("Skill", skillTool.getName());
    }

    @Test
    void testGetDescription() {
        assertNotNull(skillTool.getDescription());
        assertTrue(skillTool.getDescription().contains("skill"));
    }

    @Test
    void testGetParameterSchema() {
        JsonNode schema = skillTool.getParameterSchema();

        assertNotNull(schema);
        assertEquals("object", schema.get("type").asText());
        assertNotNull(schema.get("properties").get("skill"));
        assertNotNull(schema.get("properties").get("args"));
        assertTrue(schema.get("required").iterator().hasNext());
    }

    // ==================== execute 成功流程测试 ====================

    /**
     * 测试 inline 模式成功执行
     */
    @Test
    void testExecute_InlineSuccess() throws Exception {
        // given
        String skillName = "git-commit";
        String args = "fix: resolve bug";
        String characterId = "char-1";
        String workspaceId = "ws-1";
        String sessionKey = "session-123";

        SkillRuntime skill = createSkillRuntime(skillName);

        AgentTool.ToolContext context = createToolContext(skillName, args, characterId, workspaceId, sessionKey);

        when(skillRegistry.getSkills(characterId, workspaceId)).thenReturn(List.of(skill));
        when(skillFilter.filter(any())).thenReturn(List.of(skill));
        when(permissionChecker.check(any(), any())).thenReturn(SkillPermissionResult.allow(null));

        SkillToolData toolData = SkillToolData.builder()
                .skillName(skillName)
                .executionMode(SkillToolData.ExecutionMode.INLINE)
                .newMessages(List.of())
                .build();
        when(skillExecutor.execute(any(), anyString(), anyString(), any())).thenReturn(toolData);

        // when
        CompletableFuture<AgentTool.ToolResult> future = skillTool.execute(context);
        AgentTool.ToolResult result = future.get();

        // then
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertInstanceOf(SkillToolData.class, result.getData());
        verify(usageService, times(1)).recordSuccess(any(), any(), anyString(), anyString(), anyLong());
    }

    /**
     * 测试 fork 模式成功执行
     */
    @Test
    void testExecute_ForkSuccess() throws Exception {
        // given
        String skillName = "code-gen";
        String characterId = "char-1";
        String workspaceId = "ws-1";

        SkillRuntime skill = SkillRuntime.builder()
                .name(skillName)
                .skillId("skill-id")
                .version(1)
                .category(SkillCategory.CODER)
                .executionContext(ExecutionContext.FORK)
                .build();

        AgentTool.ToolContext context = createToolContext(skillName, null, characterId, workspaceId, "session-123");

        when(skillRegistry.getSkills(characterId, workspaceId)).thenReturn(List.of(skill));
        when(skillFilter.filter(any())).thenReturn(List.of(skill));
        when(permissionChecker.check(any(), any())).thenReturn(SkillPermissionResult.allow(null));

        SkillToolData toolData = SkillToolData.builder()
                .skillName(skillName)
                .executionMode(SkillToolData.ExecutionMode.FORK)
                .build();
        when(skillExecutor.execute(any(), any(), anyString(), any())).thenReturn(toolData);

        // when
        CompletableFuture<AgentTool.ToolResult> future = skillTool.execute(context);
        AgentTool.ToolResult result = future.get();

        // then
        assertTrue(result.isSuccess());
    }

    // ==================== 错误处理测试 ====================

    /**
     * 测试 skill 名称为空时返回失败
     */
    @Test
    void testExecute_SkillNameEmpty() throws Exception {
        AgentTool.ToolContext context = createToolContext("", null, "char-1", "ws-1", "session");

        CompletableFuture<AgentTool.ToolResult> future = skillTool.execute(context);
        AgentTool.ToolResult result = future.get();

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("不能为空"));
    }

    /**
     * 测试 skill 不存在时返回失败
     */
    @Test
    void testExecute_SkillNotFound() throws Exception {
        String skillName = "non-existent";
        AgentTool.ToolContext context = createToolContext(skillName, null, "char-1", "ws-1", "session");

        when(skillRegistry.getSkills(anyString(), anyString())).thenReturn(List.of());
        when(skillFilter.filter(any())).thenReturn(List.of());

        CompletableFuture<AgentTool.ToolResult> future = skillTool.execute(context);
        AgentTool.ToolResult result = future.get();

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("未找到"));
    }

    /**
     * 测试 disableModelInvocation 的 Skill 被拒绝
     */
    @Test
    void testExecute_DisableModelInvocation() throws Exception {
        String skillName = "manual-only";
        SkillRuntime skill = SkillRuntime.builder()
                .name(skillName)
                .skillId("skill-id")
                .version(1)
                .disableModelInvocation(true)
                .build();

        AgentTool.ToolContext context = createToolContext(skillName, null, "char-1", "ws-1", "session");

        when(skillRegistry.getSkills(anyString(), anyString())).thenReturn(List.of(skill));
        when(skillFilter.filter(any())).thenReturn(List.of(skill));

        CompletableFuture<AgentTool.ToolResult> future = skillTool.execute(context);
        AgentTool.ToolResult result = future.get();

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("禁用模型自动调用"));
    }

    /**
     * 测试权限拒绝
     */
    @Test
    void testExecute_PermissionDenied() throws Exception {
        String skillName = "dangerous-op";
        SkillRuntime skill = createSkillRuntime(skillName);
        AgentTool.ToolContext context = createToolContext(skillName, null, "char-1", "ws-1", "session");

        when(skillRegistry.getSkills(anyString(), anyString())).thenReturn(List.of(skill));
        when(skillFilter.filter(any())).thenReturn(List.of(skill));
        when(permissionChecker.check(any(), any())).thenReturn(SkillPermissionResult.deny("dangerous-op"));

        CompletableFuture<AgentTool.ToolResult> future = skillTool.execute(context);
        AgentTool.ToolResult result = future.get();

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("拒绝"));
    }

    /**
     * 测试执行异常时记录失败并返回错误
     */
    @Test
    void testExecute_ExecutionException() throws Exception {
        String skillName = "faulty-skill";
        SkillRuntime skill = createSkillRuntime(skillName);
        AgentTool.ToolContext context = createToolContext(skillName, null, "char-1", "ws-1", "session");

        when(skillRegistry.getSkills(anyString(), anyString())).thenReturn(List.of(skill));
        when(skillFilter.filter(any())).thenReturn(List.of(skill));
        when(permissionChecker.check(any(), any())).thenReturn(SkillPermissionResult.allow(null));
        when(skillExecutor.execute(any(), any(), anyString(), any()))
                .thenThrow(new RuntimeException("Test exception"));

        CompletableFuture<AgentTool.ToolResult> future = skillTool.execute(context);
        AgentTool.ToolResult result = future.get();

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("执行失败"));
        verify(usageService, times(1)).recordFailure(any(), any(), anyString(), any(), anyString());
    }

    // ==================== 参数解析测试 ====================

    /**
     * 测试 skill 名称前导斜杠被去除
     */
    @Test
    void testExecute_SkillNameWithLeadingSlash() throws Exception {
        String skillName = "/git-commit";
        SkillRuntime skill = createSkillRuntime("git-commit");
        AgentTool.ToolContext context = createToolContext(skillName, null, "char-1", "ws-1", "session");

        when(skillRegistry.getSkills(anyString(), anyString())).thenReturn(List.of(skill));
        when(skillFilter.filter(any())).thenReturn(List.of(skill));
        when(permissionChecker.check(any(), any())).thenReturn(SkillPermissionResult.allow(null));
        when(skillExecutor.execute(any(), any(), anyString(), any()))
                .thenReturn(SkillToolData.builder().skillName("git-commit").build());

        CompletableFuture<AgentTool.ToolResult> future = skillTool.execute(context);
        AgentTool.ToolResult result = future.get();

        assertTrue(result.isSuccess());
    }

    /**
     * 测试空 args 被视为 null
     */
    @Test
    void testExecute_EmptyArgsTreatedAsNull() throws Exception {
        String skillName = "git-commit";
        SkillRuntime skill = createSkillRuntime(skillName);
        AgentTool.ToolContext context = createToolContext(skillName, "", "char-1", "ws-1", "session");

        when(skillRegistry.getSkills(anyString(), anyString())).thenReturn(List.of(skill));
        when(skillFilter.filter(any())).thenReturn(List.of(skill));
        when(permissionChecker.check(any(), any())).thenReturn(SkillPermissionResult.allow(null));
        when(skillExecutor.execute(any(), isNull(), anyString(), any()))
                .thenReturn(SkillToolData.builder().skillName(skillName).build());

        CompletableFuture<AgentTool.ToolResult> future = skillTool.execute(context);
        AgentTool.ToolResult result = future.get();

        assertTrue(result.isSuccess());
    }

    // ==================== 辅助方法 ====================

    private SkillRuntime createSkillRuntime(String name) {
        return SkillRuntime.builder()
                .name(name)
                .skillId("skill-id-" + name)
                .version(1)
                .category(SkillCategory.CODER)
                .executionContext(ExecutionContext.INLINE)
                .disableModelInvocation(false)
                .userInvocable(true)
                .content("# Skill content")
                .build();
    }

    private AgentTool.ToolContext createToolContext(String skillName, String args,
                                                    String characterId, String workspaceId, String sessionKey) {
        ObjectMapper mapper = new ObjectMapper();
        var node = mapper.createObjectNode();
        node.put("skill", skillName);
        if (args != null) {
            node.put("args", args);
        }

        return AgentTool.ToolContext.builder()
                .parameters(node)
                .sessionKey(sessionKey)
                .cwd("/workspace")
                .characterId(characterId)
                .workspaceId(workspaceId)
                .build();
    }
}
