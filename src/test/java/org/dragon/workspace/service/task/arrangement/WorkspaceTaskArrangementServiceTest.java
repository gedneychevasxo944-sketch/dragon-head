package org.dragon.workspace.service.task.arrangement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.dragon.store.StoreFactory;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.cooperation.chat.ChatRoom;
import org.dragon.workspace.cooperation.task.CollaborationSessionCoordinator;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.service.member.WorkspaceMemberManagementService;
import org.dragon.workspace.service.task.arrangement.dto.ChildTaskPlan;
import org.dragon.workspace.service.task.arrangement.dto.TaskCreationCommand;
import org.dragon.workspace.service.task.arrangement.dto.TaskDecompositionResult;
import org.dragon.workspace.service.task.execution.WorkspaceTaskExecutionService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * WorkspaceTaskArrangementService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceTaskArrangementServiceTest {

    @Mock
    private WorkspaceRegistry workspaceRegistry;

    @Mock
    private WorkspaceMemberManagementService memberService;

    @Mock
    private ChatRoom chatRoom;

    @Mock
    private WorkspaceTaskExecutionService taskExecutionService;

    @Mock
    private TaskDecomposer taskDecomposer;

    @Mock
    private TaskAssignmentResolver taskAssignmentResolver;

    @Mock
    private CollaborationSessionCoordinator sessionCoordinator;

    @Mock
    private StoreFactory storeFactory;

    @Mock
    private TaskStore taskStore;

    private WorkspaceTaskArrangementService arrangementService;

    private String workspaceId;
    private Workspace workspace;
    private List<WorkspaceMember> members;

    @BeforeEach
    void setUp() {
        when(storeFactory.get(TaskStore.class)).thenReturn(taskStore);

        arrangementService = new WorkspaceTaskArrangementService(
                workspaceRegistry,
                memberService,
                chatRoom,
                taskExecutionService,
                taskDecomposer,
                taskAssignmentResolver,
                new ChildTaskFactory(),
                sessionCoordinator,
                storeFactory
        );

        workspaceId = "workspace-" + UUID.randomUUID();
        workspace = Workspace.builder()
                .id(workspaceId)
                .name("Test Workspace")
                .createdAt(LocalDateTime.now())
                .build();

        WorkspaceMember member1 = WorkspaceMember.builder()
                .id("member-1")
                .workspaceId(workspaceId)
                .characterId("char-1")
                .role("Character 1")
                .build();

        WorkspaceMember member2 = WorkspaceMember.builder()
                .id("member-2")
                .workspaceId(workspaceId)
                .characterId("char-2")
                .role("Character 2")
                .build();

        members = List.of(member1, member2);
    }

    @Test
    void testSubmitTask_SpecifiedMode() {
        // given
        when(workspaceRegistry.get(workspaceId)).thenReturn(Optional.of(workspace));
        when(memberService.listMembers(workspaceId)).thenReturn(members);

        TaskCreationCommand command = TaskCreationCommand.builder()
                .taskName("Test Task")
                .taskDescription("Test Description")
                .creatorId("user-1")
                .input("Task input")
                .sourceChannel("feishu")
                .build();

        // when
        Task result = arrangementService.submitTask(
                workspaceId,
                command,
                WorkspaceTaskArrangementService.TaskExecutionMode.SPECIFIED,
                List.of("char-1")
        );

        // then
        assertNotNull(result);
        assertEquals("Test Task", result.getName());
        assertEquals("Test Description", result.getDescription());
        // SPECIFIED mode sets parent status to RUNNING after createAndExecuteSingleChildTask
        assertEquals(TaskStatus.RUNNING, result.getStatus());
        assertEquals(workspaceId, result.getWorkspaceId());

        // save is called twice: once for parent task, once for child task
        verify(taskStore, atLeast(1)).save(any(Task.class));
    }

    @Test
    void testSubmitTask_DefaultMode() {
        // given
        when(workspaceRegistry.get(workspaceId)).thenReturn(Optional.of(workspace));
        when(memberService.listMembers(workspaceId)).thenReturn(members);

        TaskCreationCommand command = TaskCreationCommand.builder()
                .taskName("Default Mode Task")
                .taskDescription("Description")
                .creatorId("user-1")
                .build();

        // when
        Task result = arrangementService.submitTask(
                workspaceId,
                command,
                WorkspaceTaskArrangementService.TaskExecutionMode.DEFAULT,
                null
        );

        // then
        assertNotNull(result);
        // save is called twice: once for parent task, once for child task
        verify(taskStore, atLeast(1)).save(any(Task.class));
    }

    @Test
    void testSubmitTask_AutoMode() {
        // given
        when(workspaceRegistry.get(workspaceId)).thenReturn(Optional.of(workspace));
        when(memberService.listMembers(workspaceId)).thenReturn(members);

        TaskDecompositionResult decompositionResult = TaskDecompositionResult.builder()
                .summary("Test decomposition")
                .childTasks(List.of(
                        ChildTaskPlan.builder()
                                .planTaskId("plan-1")
                                .name("Sub Task 1")
                                .description("Sub task description")
                                .characterId("char-1")
                                .build()
                ))
                .build();

        when(taskDecomposer.decompose(any(Task.class), any(Workspace.class), anyList()))
                .thenReturn(decompositionResult);
        when(taskAssignmentResolver.resolveAssignments(any(), any(), anyList()))
                .thenReturn(decompositionResult);

        TaskCreationCommand command = TaskCreationCommand.builder()
                .taskName("Auto Mode Task")
                .taskDescription("Description")
                .creatorId("user-1")
                .input("input data")
                .build();

        // when
        Task result = arrangementService.submitTask(
                workspaceId,
                command,
                WorkspaceTaskArrangementService.TaskExecutionMode.AUTO,
                null
        );

        // then
        assertNotNull(result);
        verify(taskDecomposer).decompose(any(Task.class), eq(workspace), eq(members));
        verify(taskAssignmentResolver).resolveAssignments(any(), eq(workspace), eq(members));
    }

    @Test
    void testSubmitTask_WorkspaceNotFound() {
        // given
        when(workspaceRegistry.get(workspaceId)).thenReturn(Optional.empty());

        TaskCreationCommand command = TaskCreationCommand.builder()
                .taskName("Test Task")
                .taskDescription("Description")
                .creatorId("user-1")
                .build();

        // when & then
        assertThrows(IllegalArgumentException.class, () ->
                arrangementService.submitTask(
                        workspaceId,
                        command,
                        WorkspaceTaskArrangementService.TaskExecutionMode.SPECIFIED,
                        null
                )
        );
    }

    @Test
    void testSubmitTask_NoMembers() {
        // given
        when(workspaceRegistry.get(workspaceId)).thenReturn(Optional.of(workspace));
        when(memberService.listMembers(workspaceId)).thenReturn(List.of());

        TaskCreationCommand command = TaskCreationCommand.builder()
                .taskName("Test Task")
                .taskDescription("Description")
                .creatorId("user-1")
                .build();

        // when
        Task result = arrangementService.submitTask(
                workspaceId,
                command,
                WorkspaceTaskArrangementService.TaskExecutionMode.DEFAULT,
                null
        );

        // then
        assertEquals(TaskStatus.FAILED, result.getStatus());
        assertEquals("No members available", result.getErrorMessage());
    }

    @Test
    void testSubmitTask_AutoMode_DecompositionFailed() {
        // given
        when(workspaceRegistry.get(workspaceId)).thenReturn(Optional.of(workspace));
        when(memberService.listMembers(workspaceId)).thenReturn(members);
        when(taskDecomposer.decompose(any(Task.class), any(Workspace.class), anyList()))
                .thenReturn(null);

        TaskCreationCommand command = TaskCreationCommand.builder()
                .taskName("Task")
                .creatorId("user-1")
                .build();

        // when
        Task result = arrangementService.submitTask(
                workspaceId,
                command,
                WorkspaceTaskArrangementService.TaskExecutionMode.AUTO,
                null
        );

        // then
        assertEquals(TaskStatus.FAILED, result.getStatus());
        assertEquals("Task decomposition returned no child tasks", result.getErrorMessage());
    }

    @Test
    void testSubmitTask_AutoMode_EmptyChildTasks() {
        // given
        when(workspaceRegistry.get(workspaceId)).thenReturn(Optional.of(workspace));
        when(memberService.listMembers(workspaceId)).thenReturn(members);

        TaskDecompositionResult emptyResult = TaskDecompositionResult.builder()
                .childTasks(List.of())
                .build();

        when(taskDecomposer.decompose(any(Task.class), any(Workspace.class), anyList()))
                .thenReturn(emptyResult);

        TaskCreationCommand command = TaskCreationCommand.builder()
                .taskName("Task")
                .creatorId("user-1")
                .build();

        // when
        Task result = arrangementService.submitTask(
                workspaceId,
                command,
                WorkspaceTaskArrangementService.TaskExecutionMode.AUTO,
                null
        );

        // then
        assertEquals(TaskStatus.FAILED, result.getStatus());
        assertEquals("Task decomposition returned no child tasks", result.getErrorMessage());
    }

    @Test
    void testRebalance() {
        // given
        String taskId = "task-" + UUID.randomUUID();

        // when
        arrangementService.rebalance(taskId, new WorkspaceTaskArrangementService.ExecutionFeedback(
                "child-1", true, null, 1000L
        ));

        // then - rebalance is a no-op for now, just verify no exception
    }

    @Test
    void testSubmitTask_SpecifiedMode_NoValidCharacters() {
        // given
        when(workspaceRegistry.get(workspaceId)).thenReturn(Optional.of(workspace));
        when(memberService.listMembers(workspaceId)).thenReturn(members);

        TaskCreationCommand command = TaskCreationCommand.builder()
                .taskName("Test Task")
                .creatorId("user-1")
                .build();

        // when - specify non-existent characters
        Task result = arrangementService.submitTask(
                workspaceId,
                command,
                WorkspaceTaskArrangementService.TaskExecutionMode.SPECIFIED,
                List.of("non-existent-char")
        );

        // then
        assertEquals(TaskStatus.FAILED, result.getStatus());
        assertEquals("No valid specified characters", result.getErrorMessage());
    }

    @Test
    void testSubmitTask_SpecifiedMode_WithValidCharacter() {
        // given
        when(workspaceRegistry.get(workspaceId)).thenReturn(Optional.of(workspace));
        when(memberService.listMembers(workspaceId)).thenReturn(members);

        TaskCreationCommand command = TaskCreationCommand.builder()
                .taskName("Test Task")
                .creatorId("user-1")
                .build();

        // when - specify existing character
        Task result = arrangementService.submitTask(
                workspaceId,
                command,
                WorkspaceTaskArrangementService.TaskExecutionMode.SPECIFIED,
                List.of("char-1")
        );

        // then
        assertNotNull(result);
        // save is called twice: once for parent task, once for child task
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskStore, atLeast(1)).save(taskCaptor.capture());

        // The child task should have char-1 as characterId
        boolean foundChildWithCharId = taskCaptor.getAllValues().stream()
                .anyMatch(task -> "char-1".equals(task.getCharacterId()));
        assertTrue(foundChildWithCharId);
    }
}
