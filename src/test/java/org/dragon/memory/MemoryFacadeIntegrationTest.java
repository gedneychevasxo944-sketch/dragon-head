package org.dragon.memory;

import org.dragon.memory.constants.MemoryScope;
import org.dragon.memory.constants.MemoryType;
import org.dragon.memory.entity.MemoryEntry;
import org.dragon.memory.entity.MemoryQuery;
import org.dragon.memory.entity.MemorySearchResult;
import org.dragon.memory.entity.SessionSnapshot;
import org.dragon.memory.service.core.MemoryFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

/**
 * MemoryFacade 集成测试
 *
 * <p>使用真实的文件系统存储（指向 @TempDir 临时目录），验证 workspace、character、session
 * 三类记忆的完整生命周期：创建空间 → 保存记忆 → 召回。
 *
 * <p>LLMCallerSelector 通过 @MockBean 替换，令 recall 中的 LLM 筛选返回空内容，
 * 触发降级逻辑（全量返回），使测试不依赖外部 LLM 服务。
 *
 * @author binarytom
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MemoryFacadeIntegrationTest {

    /**
     * 用 noop LLMCallerSelector 替换真实实现，令 recall 中的 LLM 筛选返回空内容，
     * 触发降级逻辑（全量返回），使测试不依赖外部 LLM 服务。
     */
//    @TestConfiguration
//    static class NoopLLMConfig {
//        @Bean
//        @Primary
//        LLMCallerSelector noopLLMCallerSelector() {
//            LLMCaller noopCaller = mock(LLMCaller.class);
//            when(noopCaller.call(any())).thenReturn(LLMResponse.builder().content("").build());
//            LLMCallerSelector selector = mock(LLMCallerSelector.class);
//            when(selector.getDefault()).thenReturn(noopCaller);
//            return selector;
//        }
//    }

    // 每个测试类共享一个临时目录，随 JVM 退出自动清理
    @TempDir
    static Path tempMemoryDir;

    @DynamicPropertySource
    static void configureMemoryDir(DynamicPropertyRegistry registry) {
        registry.add("dragon.memory.rootDir", () -> tempMemoryDir.toAbsolutePath().toString());
    }

    @Autowired
    private MemoryFacade memoryFacade;

    private static final String WORKSPACE_ID = "ws-integration-test";
    private static final String CHARACTER_ID = "char-integration-test";
    private static final String SESSION_ID   = "sess-integration-test";

    // ===== 1. Workspace Memory =====

    @Test
    void workspaceMemory_saveAndList() {
        // 保存两条 workspace 记忆
        MemoryEntry decision = memoryFacade.saveWorkspaceMemory(WORKSPACE_ID, MemoryEntry.builder()
                .title("技术选型决策")
                .description("关于数据库选型的讨论结果")
                .content("经过评估，决定采用 MySQL 作为主存储，Redis 作为缓存层。")
                .type(MemoryType.WORKSPACE_DECISION)
                .scope(MemoryScope.WORKSPACE)
                .fileName("tech-decision.md")
                .build());

        MemoryEntry feedback = memoryFacade.saveWorkspaceMemory(WORKSPACE_ID, MemoryEntry.builder()
                .title("代码审查反馈")
                .description("Sprint 3 代码审查中收集的改进建议")
                .content("需要增加单元测试覆盖率，目前仅 42%，目标 80%。")
                .type(MemoryType.FEEDBACK)
                .scope(MemoryScope.WORKSPACE)
                .fileName("code-review-feedback.md")
                .build());

        // 验证保存后可以列出
        List<MemoryEntry> all = memoryFacade.listWorkspaceMemories(WORKSPACE_ID);
        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(e -> "技术选型决策".equals(e.getTitle())));
        assertTrue(all.stream().anyMatch(e -> "代码审查反馈".equals(e.getTitle())));
    }

    @Test
    void workspaceMemory_recallByQuery() {
        // 准备：保存三条内容各异的记忆
        memoryFacade.saveWorkspaceMemory(WORKSPACE_ID + "-recall", MemoryEntry.builder()
                .title("部署规范")
                .description("生产环境部署流程说明")
                .content("所有部署必须经过 staging 环境验证，使用蓝绿发布策略。")
                .type(MemoryType.REFERENCE)
                .scope(MemoryScope.WORKSPACE)
                .fileName("deploy-spec.md")
                .build());

        memoryFacade.saveWorkspaceMemory(WORKSPACE_ID + "-recall", MemoryEntry.builder()
                .title("安全策略")
                .description("数据安全和访问控制要求")
                .content("所有 API 必须通过 JWT 鉴权，敏感字段需加密存储。")
                .type(MemoryType.REFERENCE)
                .scope(MemoryScope.WORKSPACE)
                .fileName("security-policy.md")
                .build());

        // 召回（LLM 降级为全量返回，limit=5 覆盖全部）
        List<MemorySearchResult> results = memoryFacade.recall(MemoryQuery.builder()
                .workspaceId(WORKSPACE_ID + "-recall")
                .limit(5)
                .build());

        assertEquals(2, results.size());
        results.forEach(r -> assertNotNull(r.getMemory()));
    }

    // ===== 2. Character Memory =====

    @Test
    void characterMemory_saveAndList() {
        MemoryEntry profile = memoryFacade.saveCharacterMemory(CHARACTER_ID, MemoryEntry.builder()
                .title("用户偏好")
                .description("用户交互风格偏好")
                .content("用户偏好简洁的回复，不喜欢冗长的解释，倾向于代码示例而非文字描述。")
                .type(MemoryType.CHARACTER_PROFILE)
                .scope(MemoryScope.CHARACTER)
                .fileName("user-preference.md")
                .build());

        MemoryEntry projectCtx = memoryFacade.saveCharacterMemory(CHARACTER_ID, MemoryEntry.builder()
                .title("当前项目背景")
                .description("正在参与的项目概述")
                .content("参与 dragon-head 项目开发，负责 memory 模块设计与实现，使用 Spring Boot + 文件系统存储。")
                .type(MemoryType.PROJECT)
                .scope(MemoryScope.CHARACTER)
                .fileName("project-context.md")
                .build());

        MemoryEntry note = memoryFacade.saveCharacterMemory(CHARACTER_ID, MemoryEntry.builder()
                .title("重要提醒")
                .description("需要特别关注的注意事项")
                .content("每次修改 MemoryPathResolver 后需同步更新集成测试中的路径断言。")
                .type(MemoryType.FEEDBACK)
                .scope(MemoryScope.CHARACTER)
                .fileName("important-note.md")
                .build());

        List<MemoryEntry> all = memoryFacade.listCharacterMemories(CHARACTER_ID);
        assertEquals(3, all.size());
        assertTrue(all.stream().anyMatch(e -> "用户偏好".equals(e.getTitle())));
        assertTrue(all.stream().anyMatch(e -> "当前项目背景".equals(e.getTitle())));
        assertTrue(all.stream().anyMatch(e -> "重要提醒".equals(e.getTitle())));
    }

    @Test
    void characterMemory_recallByQuery() {
        String charId = CHARACTER_ID + "-recall";

        memoryFacade.saveCharacterMemory(charId, MemoryEntry.builder()
                .title("Java 编码规范")
                .description("团队 Java 代码风格要求")
                .content("使用 Lombok 简化样板代码，所有 public 方法必须有 Javadoc。")
                .type(MemoryType.REFERENCE)
                .scope(MemoryScope.CHARACTER)
                .fileName("java-conventions.md")
                .build());

        memoryFacade.saveCharacterMemory(charId, MemoryEntry.builder()
                .title("常见错误模式")
                .description("过去遇到的典型 bug 及解法")
                .content("Ebean 实体类必须经过字节码增强，否则懒加载不生效，需在 pom.xml 配置 enhance 插件。")
                .type(MemoryType.FEEDBACK)
                .scope(MemoryScope.CHARACTER)
                .fileName("common-pitfalls.md")
                .build());

        List<MemorySearchResult> results = memoryFacade.recall(MemoryQuery.builder()
                .characterId(charId)
                .limit(10)
                .text("错误")
                .build());

        assertEquals(1, results.size());
    }

    // ===== 3. Session Memory =====

    @Test
    void sessionMemory_startAndUpdate() {
        // 创建 session
        SessionSnapshot created = memoryFacade.startSession(SESSION_ID, WORKSPACE_ID, CHARACTER_ID);
        assertNotNull(created);
        assertEquals(SESSION_ID, created.getSessionId());

        // 更新 session：记录本轮会话的目标和决策
        SessionSnapshot updated = memoryFacade.updateSession(SESSION_ID, SessionSnapshot.builder()
                .sessionId(SESSION_ID)
                .workspaceId(WORKSPACE_ID)
                .characterId(CHARACTER_ID)
                .summary("用户正在调试 memory 模块的召回逻辑，已确认文件存储路径配置正确。")
                .currentGoal("完成 MemoryFacade 集成测试")
                .recentDecisions(List.of(
                        "使用 @TempDir 隔离测试文件系统",
                        "通过 @MockBean 替换 LLM 调用"))
                .unresolvedQuestions(List.of("是否需要测试 checkpoint 恢复场景"))
                .build());

        assertNotNull(updated);

        // 读回并验证内容
        SessionSnapshot fetched = memoryFacade.getSession(SESSION_ID);
        assertNotNull(fetched);
        assertEquals("完成 MemoryFacade 集成测试", fetched.getCurrentGoal());
        assertEquals(2, fetched.getRecentDecisions().size());
        assertEquals(1, fetched.getUnresolvedQuestions().size());
    }

    @Test
    void sessionMemory_recallIncludesSessionSummary() {
        String sessionId = SESSION_ID + "-recall";

        // 创建并更新 session，写入摘要和决策
        memoryFacade.startSession(sessionId, WORKSPACE_ID, CHARACTER_ID);
        memoryFacade.updateSession(sessionId, SessionSnapshot.builder()
                .sessionId(sessionId)
                .summary("讨论了记忆召回的降级策略：LLM 不可用时回退到全量返回。")
                .recentDecisions(List.of("降级策略：LLM 失败时全量返回", "limit 默认值设为 5"))
                .unresolvedQuestions(List.of())
                .build());

        // 召回：指定 sessionId，期望 session 摘要和决策都出现在结果中
        List<MemorySearchResult> results = memoryFacade.recall(MemoryQuery.builder()
                .sessionId(sessionId)
                .limit(10)
                .build());

        // session 摘要（1条）+ 决策（2条）= 3条
        assertEquals(3, results.size());
        assertTrue(results.stream()
                .anyMatch(r -> MemoryType.SESSION_SUMMARY == r.getMemory().getType()));
        assertTrue(results.stream()
                .anyMatch(r -> MemoryType.WORKSPACE_DECISION == r.getMemory().getType()));
    }

    // ===== 4. 跨来源复合召回 =====

    @Test
    void compositeRecall_combinesAllSources() {
        String wsId    = "ws-composite";
        String charId  = "char-composite";
        String sessId  = "sess-composite";

        // Workspace：保存一条架构决策
        memoryFacade.saveWorkspaceMemory(wsId, MemoryEntry.builder()
                .title("微服务拆分原则")
                .description("服务边界划分指导方针")
                .content("按业务域拆分服务，单个服务代码量不超过 5000 行。")
                .type(MemoryType.WORKSPACE_DECISION)
                .scope(MemoryScope.WORKSPACE)
                .fileName("microservice-principle.md")
                .build());

        // Character：保存一条个人偏好
        memoryFacade.saveCharacterMemory(charId, MemoryEntry.builder()
                .title("回复风格偏好")
                .description("AI 助手回复的格式要求")
                .content("优先使用 Markdown 列表和代码块，避免大段连续文字。")
                .type(MemoryType.CHARACTER_PROFILE)
                .scope(MemoryScope.CHARACTER)
                .fileName("reply-style.md")
                .build());

        // Session：创建并写入摘要
        memoryFacade.startSession(sessId, wsId, charId);
        memoryFacade.updateSession(sessId, SessionSnapshot.builder()
                .sessionId(sessId)
                .summary("用户询问如何优化 Agent 的 prompt 组装流程。")
                .recentDecisions(List.of())
                .unresolvedQuestions(List.of())
                .build());

        // 复合召回：同时指定三个来源
        List<MemorySearchResult> results = memoryFacade.recall(MemoryQuery.builder()
                .workspaceId(wsId)
                .characterId(charId)
                .sessionId(sessId)
                .limit(10)
                .build());

        // workspace(1) + character(1) + session summary(1) = 3
        assertEquals(3, results.size());

        // 验证三种 scope 都有覆盖
        assertTrue(results.stream().anyMatch(r -> MemoryScope.WORKSPACE == r.getMemory().getScope()));
        assertTrue(results.stream().anyMatch(r -> MemoryScope.CHARACTER == r.getMemory().getScope()));
        assertTrue(results.stream().anyMatch(r -> MemoryScope.SESSION   == r.getMemory().getScope()));
    }
}