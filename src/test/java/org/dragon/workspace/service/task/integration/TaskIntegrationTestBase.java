package org.dragon.workspace.service.task.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dragon.agent.llm.LLMRequest;
import org.dragon.agent.llm.LLMResponse;
import org.dragon.agent.model.ModelRegistry;
import org.dragon.character.Character;
import org.dragon.character.profile.CharacterProfile;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.CharacterRuntimeBinder;
import org.dragon.config.service.ConfigApplication;
import org.dragon.store.StoreFactory;
import org.dragon.store.StoreType;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.tools.AgentTool;
import org.dragon.tools.ToolRegistry;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.chat.ChatRoom;
import org.dragon.workspace.chat.ChatSession;
import org.dragon.workspace.chat.ChatSessionStore;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.service.member.WorkspaceMemberManagementService;
import org.dragon.workspace.service.task.arrangement.TaskAssignmentResolver;
import org.dragon.workspace.service.task.arrangement.TaskDecomposer;
import org.dragon.workspace.service.task.arrangement.WorkspaceTaskArrangementService;
import org.dragon.workspace.service.task.arrangement.dto.TaskCreationCommand;
import org.dragon.workspace.service.task.execution.CollaborationSessionCoordinator;
import org.dragon.workspace.service.task.execution.TaskBridge;
import org.dragon.workspace.service.task.execution.TaskBridgeContext;
import org.dragon.workspace.service.task.execution.WorkspaceTaskExecutionService;
import org.dragon.workspace.task.notify.WorkspaceTaskNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Task 模块集成测试基类
 *
 * <p>提供完整的测试环境搭建，包括：
 * <ul>
 *   <li>MEMORY 存储配置（不依赖 MySQL）</li>
 *   <li>Mock LLM 调用（不调用外部 API）</li>
 *   <li>真实 Workspace/Character 注册</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class TaskIntegrationTestBase {

    protected ObjectMapper objectMapper = new ObjectMapper();

    // ==================== Mock Components ====================

    @Mock
    protected org.dragon.agent.llm.caller.LLMCaller llmCaller;

    @Mock
    protected WorkspaceTaskNotifier taskNotifier;

    @Mock
    protected ChatRoom chatRoom;

    @Mock
    protected org.dragon.workspace.service.material.WorkspaceMaterialService materialService;

    // ==================== Real Components ====================

    protected StoreFactory storeFactory;
    protected ToolRegistry toolRegistry;
    protected WorkspaceRegistry workspaceRegistry;
    protected CharacterRegistry characterRegistry;
    protected ModelRegistry modelRegistry;
    protected ConfigApplication configApplication;
    protected WorkspaceMemberManagementService memberService;
    protected TaskDecomposer taskDecomposer;
    protected TaskAssignmentResolver taskAssignmentResolver;
    protected WorkspaceTaskArrangementService arrangementService;
    protected WorkspaceTaskExecutionService executionService;
    protected TaskBridge taskBridge;

    // Real CollaborationSessionCoordinator
    protected CollaborationSessionCoordinator sessionCoordinator;

    // ==================== Test Data ====================

    protected String workspaceId;
    protected String characterId;
    protected String modelId;

    @BeforeEach
    void setUpIntegrationTest() {
        // 初始化 StoreFactory（使用 MEMORY 类型）
        storeFactory = mock(StoreFactory.class);

        // 创建 Mock Store 实例
        setupMockStores();

        // 初始化 ToolRegistry
        toolRegistry = new ToolRegistry(storeFactory);

        // 初始化 WorkspaceRegistry
        workspaceRegistry = new WorkspaceRegistry(storeFactory);

        // 初始化 CharacterRegistry
        characterRegistry = new CharacterRegistry(storeFactory);

        // 初始化 ModelRegistry
        modelRegistry = new ModelRegistry(storeFactory, createMockConfigApplication());

        // 初始化配置
        configApplication = createMockConfigApplication();

        // 初始化 MemberService
        memberService = createMockMemberService();

        // 注册测试数据（在创建需要 characterId 的 mock 之前）
        registerTestData();

        // 初始化 TaskDecomposer（Mock LLM）
        taskDecomposer = createMockTaskDecomposer();

        // 初始化 TaskAssignmentResolver
        taskAssignmentResolver = createMockTaskAssignmentResolver();

        // 初始化 CollaborationSessionCoordinator（真实实例）
        sessionCoordinator = new CollaborationSessionCoordinator(chatRoom, storeFactory);

        // 初始化 TaskBridge（Mock，用于集成测试）
        taskBridge = createMockTaskBridge();

        // 初始化 WorkspaceTaskExecutionService
        executionService = new WorkspaceTaskExecutionService(
                taskBridge,
                taskNotifier,
                chatRoom,
                materialService,
                storeFactory
        );

        // 初始化 WorkspaceTaskArrangementService
        arrangementService = new WorkspaceTaskArrangementService(
                workspaceRegistry,
                memberService,
                chatRoom,
                executionService,
                taskDecomposer,
                taskAssignmentResolver,
                new org.dragon.workspace.service.task.arrangement.ChildTaskFactory(),
                sessionCoordinator,
                storeFactory
        );

        // 初始化 Mock LLM 调用
        setupMockLLMCalls();
    }

    /**
     * 设置 Mock Store 实例
     */
    private void setupMockStores() {
        TaskStore taskStore = new org.dragon.task.MemoryTaskStore();
        when(storeFactory.get(TaskStore.class)).thenReturn(taskStore);
        when(storeFactory.get(TaskStore.class, StoreType.MEMORY)).thenReturn(taskStore);

        // 使用真实的 Memory Store（支持持久化）
        org.dragon.workspace.store.WorkspaceStore workspaceStore = new org.dragon.workspace.store.MemoryWorkspaceStore();
        when(storeFactory.get(org.dragon.workspace.store.WorkspaceStore.class)).thenReturn(workspaceStore);
        when(storeFactory.get(org.dragon.workspace.store.WorkspaceStore.class, StoreType.MEMORY)).thenReturn(workspaceStore);

        org.dragon.character.store.CharacterStore characterStore = new org.dragon.character.store.MemoryCharacterStore();
        when(storeFactory.get(org.dragon.character.store.CharacterStore.class)).thenReturn(characterStore);
        when(storeFactory.get(org.dragon.character.store.CharacterStore.class, StoreType.MEMORY)).thenReturn(characterStore);

        // ChatSessionStore 可以用 mock
        ChatSessionStore chatSessionStore = createMockChatSessionStore();
        when(storeFactory.get(ChatSessionStore.class)).thenReturn(chatSessionStore);

        // ConfigStore 可以用 mock
        org.dragon.config.store.ConfigStore configStore = createMockConfigStore();
        when(storeFactory.get(org.dragon.config.store.ConfigStore.class)).thenReturn(configStore);

        // ToolStore 可以用 mock
        org.dragon.tools.store.ToolStore toolStore = createMockToolStore();
        when(storeFactory.get(org.dragon.tools.store.ToolStore.class)).thenReturn(toolStore);
    }

    protected void registerTestData() {
        // 创建 Workspace
        workspaceId = "test-workspace-" + System.currentTimeMillis();
        Workspace workspace = Workspace.builder()
                .id(workspaceId)
                .name("Test Workspace")
                .owner("test-owner")
                .status(Workspace.Status.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        workspaceRegistry.register(workspace);

        // 创建 Character
        characterId = "test-character-" + System.currentTimeMillis();
        Character character = createTestCharacter(characterId, "Test Character");
        characterRegistry.register(character);

        // 注册成员
        WorkspaceMember member = WorkspaceMember.builder()
                .id("member-1")
                .workspaceId(workspaceId)
                .characterId(characterId)
                .role("Developer")
                .build();
        when(memberService.listMembers(workspaceId)).thenReturn(List.of(member));
    }

    protected Character createTestCharacter(String id, String name) {
        Character character = new Character();
        character.setId(id);
        character.setName(name);
        character.setDescription("Test character for integration tests");
        character.setSource("test");
        character.setVersion(1);
        character.setStatus(CharacterProfile.Status.LOADED);
        character.setCreatedAt(LocalDateTime.now());
        character.setUpdatedAt(LocalDateTime.now());
        character.setAllowedTools(new HashSet<>());
        return character;
    }

    protected void registerTestTool(String toolName, String description) {
        AgentTool tool = new AgentTool() {
            @Override
            public String getName() {
                return toolName;
            }

            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public JsonNode getParameterSchema() {
                return objectMapper.createObjectNode();
            }

            @Override
            public CompletableFuture<ToolResult> execute(ToolContext context) {
                return CompletableFuture.completedFuture(ToolResult.ok("Tool executed: " + toolName));
            }
        };
        toolRegistry.register(tool);
    }

    // ==================== Mock Factory Methods ====================

    protected ConfigApplication createMockConfigApplication() {
        ConfigApplication mock = mock(ConfigApplication.class);
        when(mock.getGlobalPrompt(anyString(), anyString())).thenReturn("Test prompt");
        when(mock.getWorkspacePrompt(anyString(), anyString(), anyString())).thenReturn("Test workspace prompt");
        return mock;
    }

    protected WorkspaceMemberManagementService createMockMemberService() {
        WorkspaceMemberManagementService mock = mock(WorkspaceMemberManagementService.class);
        WorkspaceMember member = WorkspaceMember.builder()
                .id("member-1")
                .workspaceId("test-workspace")
                .characterId("test-character")
                .role("Developer")
                .build();
        when(mock.listMembers(anyString())).thenReturn(List.of(member));
        return mock;
    }

    protected TaskDecomposer createMockTaskDecomposer() {
        TaskDecomposer mock = mock(TaskDecomposer.class);
        org.dragon.workspace.service.task.arrangement.dto.TaskDecompositionResult result =
                org.dragon.workspace.service.task.arrangement.dto.TaskDecompositionResult.builder()
                        .summary("Test decomposition")
                        .childTasks(List.of(
                                org.dragon.workspace.service.task.arrangement.dto.ChildTaskPlan.builder()
                                        .planTaskId("plan-1")
                                        .name("Sub Task 1")
                                        .description("First sub task")
                                        .characterId(characterId)
                                        .build()
                        ))
                        .build();
        when(mock.decompose(any(Task.class), any(Workspace.class), anyList())).thenReturn(result);
        return mock;
    }

    protected TaskAssignmentResolver createMockTaskAssignmentResolver() {
        TaskAssignmentResolver mock = mock(TaskAssignmentResolver.class);
        org.dragon.workspace.service.task.arrangement.dto.TaskDecompositionResult result =
                org.dragon.workspace.service.task.arrangement.dto.TaskDecompositionResult.builder()
                        .summary("Test decomposition")
                        .childTasks(List.of(
                                org.dragon.workspace.service.task.arrangement.dto.ChildTaskPlan.builder()
                                        .planTaskId("plan-1")
                                        .name("Sub Task 1")
                                        .description("First sub task")
                                        .characterId(characterId)
                                        .build()
                        ))
                        .build();
        when(mock.resolveAssignments(any(), any(Workspace.class), anyList())).thenReturn(result);
        return mock;
    }

    protected CharacterRuntimeBinder createMockRuntimeBinder() {
        CharacterRuntimeBinder mock = mock(CharacterRuntimeBinder.class);
        return mock;
    }

    /**
     * 创建 Mock TaskBridge
     * 返回已完成的任务，避免真实执行 Character.runReAct()
     */
    protected TaskBridge createMockTaskBridge() {
        TaskBridge mock = mock(TaskBridge.class);
        // 当 execute 被调用时，返回一个已完成的任务
        when(mock.execute(any(Task.class), any(TaskBridgeContext.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            task.setResult("Task completed successfully");
            return task;
        });
        return mock;
    }

    protected void setupMockLLMCalls() {
        // Mock LLM 返回成功响应
        LLMResponse successResponse = LLMResponse.builder()
                .content("Task completed successfully")
                .build();
        try {
            when(llmCaller.call(any(LLMRequest.class))).thenReturn(successResponse);
        } catch (Exception e) {
            // ignore
        }

        // Mock ChatSession 返回有效会话
        ChatSession mockSession = ChatSession.builder()
                .id("test-session-" + System.currentTimeMillis())
                .workspaceId(workspaceId)
                .status(ChatSession.Status.ACTIVE)
                .build();
        when(chatRoom.createSession(anyString(), anyList(), anyString())).thenReturn(mockSession);
    }

    // ==================== Mock Store Factory Methods ====================

    protected org.dragon.workspace.store.WorkspaceStore createMockWorkspaceStore() {
        org.dragon.workspace.store.WorkspaceStore mock = mock(org.dragon.workspace.store.WorkspaceStore.class);
        return mock;
    }

    protected org.dragon.character.store.CharacterStore createMockCharacterStore() {
        org.dragon.character.store.CharacterStore mock = mock(org.dragon.character.store.CharacterStore.class);
        return mock;
    }

    protected ChatSessionStore createMockChatSessionStore() {
        ChatSessionStore mock = mock(ChatSessionStore.class);
        return mock;
    }

    protected org.dragon.config.store.ConfigStore createMockConfigStore() {
        org.dragon.config.store.ConfigStore mock = mock(org.dragon.config.store.ConfigStore.class);
        return mock;
    }

    protected org.dragon.tools.store.ToolStore createMockToolStore() {
        org.dragon.tools.store.ToolStore mock = mock(org.dragon.tools.store.ToolStore.class);
        return mock;
    }

    // ==================== Helper Methods ====================

    protected TaskCreationCommand createTaskCommand(String name, String description) {
        return TaskCreationCommand.builder()
                .taskName(name)
                .taskDescription(description)
                .creatorId("test-creator")
                .input("Test input data")
                .sourceChannel("test")
                .build();
    }

    protected void verifyTaskStatus(String taskId, org.dragon.task.TaskStatus expectedStatus) {
        TaskStore taskStore = storeFactory.get(TaskStore.class);
        Task task = taskStore.findById(taskId).orElse(null);
        assertNotNull(task, "Task should exist: " + taskId);
        assertEquals(expectedStatus, task.getStatus());
    }

    protected void assertTaskCreated(String taskId) {
        TaskStore taskStore = storeFactory.get(TaskStore.class);
        assertTrue(taskStore.exists(taskId), "Task should be created: " + taskId);
    }
}
