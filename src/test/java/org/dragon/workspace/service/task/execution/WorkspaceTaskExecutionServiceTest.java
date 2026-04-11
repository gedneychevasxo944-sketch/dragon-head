package org.dragon.workspace.service.task.execution;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.dragon.store.StoreFactory;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.workspace.cooperation.chat.ChatRoom;
import org.dragon.workspace.cooperation.task.TaskBridge;
import org.dragon.workspace.cooperation.task.TaskBridgeContext;
import org.dragon.workspace.cooperation.task.notify.WorkspaceTaskNotifier;
import org.dragon.workspace.service.material.WorkspaceMaterialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * WorkspaceTaskExecutionService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceTaskExecutionServiceTest {

    @Mock
    private TaskBridge taskBridge;

    @Mock
    private WorkspaceTaskNotifier taskNotifier;

    @Mock
    private ChatRoom chatRoom;

    @Mock
    private WorkspaceMaterialService materialService;

    @Mock
    private StoreFactory storeFactory;

    @Mock
    private TaskStore taskStore;

    private WorkspaceTaskExecutionService executionService;

    private Task parentTask;
    private Task childTask;
    private String workspaceId;
    private String parentTaskId;
    private String childTaskId;

    @BeforeEach
    void setUp() {
        when(storeFactory.get(TaskStore.class)).thenReturn(taskStore);

        executionService = new WorkspaceTaskExecutionService(
                taskBridge,
                taskNotifier,
                chatRoom,
                materialService,
                storeFactory
        );

        workspaceId = "workspace-" + UUID.randomUUID();
        parentTaskId = "parent-" + UUID.randomUUID();
        childTaskId = "child-" + UUID.randomUUID();

        parentTask = Task.builder()
                .id(parentTaskId)
                .workspaceId(workspaceId)
                .name("Parent Task")
                .description("Parent Description")
                .status(TaskStatus.RUNNING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .childTaskIds(List.of(childTaskId))
                .build();

        childTask = Task.builder()
                .id(childTaskId)
                .parentTaskId(parentTaskId)
                .workspaceId(workspaceId)
                .name("Child Task")
                .description("Child Description")
                .characterId("char-1")
                .status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testExecuteChildTask_Success() {
        // given
        Task completedTask = Task.builder()
                .id(childTaskId)
                .status(TaskStatus.COMPLETED)
                .result("Task completed successfully")
                .build();

        when(taskBridge.execute(any(Task.class), any(TaskBridgeContext.class)))
                .thenReturn(completedTask);
        when(taskStore.findById(childTaskId)).thenReturn(Optional.of(childTask));
        when(taskStore.findById(parentTaskId)).thenReturn(Optional.of(parentTask));
        when(taskStore.findRunnableChildTasks(parentTaskId)).thenReturn(List.of());

        // when
        executionService.executeChildTask(childTask, parentTask);

        // then
        verify(taskNotifier).notifyStarted(any(Task.class));
        verify(taskNotifier).notifyProgress(any(Task.class), eq("任务执行中"));
        verify(taskBridge).execute(eq(childTask), any(TaskBridgeContext.class));
        verify(taskNotifier).notifyCompleted(any(Task.class));
        verify(taskStore).update(any(Task.class));
    }

    @Test
    void testExecuteChildTask_Failed() {
        // given
        Task failedTask = Task.builder()
                .id(childTaskId)
                .status(TaskStatus.FAILED)
                .errorMessage("Execution failed")
                .build();

        when(taskBridge.execute(any(Task.class), any(TaskBridgeContext.class)))
                .thenReturn(failedTask);
        when(taskStore.findById(childTaskId)).thenReturn(Optional.of(childTask));
        when(taskStore.findById(parentTaskId)).thenReturn(Optional.of(parentTask));
        when(taskStore.findRunnableChildTasks(parentTaskId)).thenReturn(List.of());

        // when
        executionService.executeChildTask(childTask, parentTask);

        // then
        verify(taskNotifier).notifyFailed(any(Task.class), eq("Execution failed"));
    }

    @Test
    void testExecuteChildTask_WaitingDependency() {
        // given
        childTask.setCollaborationSessionId("session-123");
        childTask.setMaterialIds(null); // avoid NPE in buildMaterialContext

        Task waitingTask = Task.builder()
                .id(childTaskId)
                .status(TaskStatus.WAITING_DEPENDENCY)
                .waitingReason("Waiting for dependency")
                .characterId("char-1")
                .collaborationSessionId("session-123")
                .build();

        when(taskBridge.execute(any(Task.class), any(TaskBridgeContext.class)))
                .thenReturn(waitingTask);
        when(taskStore.findById(childTaskId)).thenReturn(Optional.of(childTask));
        when(taskStore.findById(parentTaskId)).thenReturn(Optional.of(parentTask));
        when(taskStore.findRunnableChildTasks(parentTaskId)).thenReturn(List.of());

        // when
        executionService.executeChildTask(childTask, parentTask);

        // then - WAITING_DEPENDENCY only calls chatRoom.markParticipantWaiting, not taskNotifier.notifyWaiting
        verify(chatRoom).markParticipantWaiting("session-123", "char-1", "Waiting for dependency");
    }

    @Test
    void testExecuteChildTask_WaitingUserInput() {
        // given
        childTask.setCollaborationSessionId("session-123");
        childTask.setMaterialIds(null); // avoid NPE in buildMaterialContext

        Task waitingTask = Task.builder()
                .id(childTaskId)
                .status(TaskStatus.WAITING_USER_INPUT)
                .lastQuestion("What should I do next?")
                .waitingReason("Waiting for user input")
                .characterId("char-1")
                .collaborationSessionId("session-123")
                .build();

        when(taskBridge.execute(any(Task.class), any(TaskBridgeContext.class)))
                .thenReturn(waitingTask);
        when(taskStore.findById(childTaskId)).thenReturn(Optional.of(childTask));
        when(taskStore.findById(parentTaskId)).thenReturn(Optional.of(parentTask));
        when(taskStore.findRunnableChildTasks(parentTaskId)).thenReturn(List.of());

        // when
        executionService.executeChildTask(childTask, parentTask);

        // then
        verify(chatRoom).markParticipantWaiting("session-123", "char-1", "What should I do next?");
        verify(taskNotifier).notifyQuestion(any(Task.class), eq("What should I do next?"));
    }

    @Test
    void testExecuteChildTasks_WithDependencies() {
        // given
        String depTaskId = "dep-" + UUID.randomUUID();
        childTask.setDependencyTaskIds(List.of(depTaskId));

        Task depTask = Task.builder()
                .id(depTaskId)
                .status(TaskStatus.PENDING) // dependency not completed
                .build();

        when(taskStore.findById(depTaskId)).thenReturn(Optional.of(depTask));

        // when
        executionService.executeChildTasks(List.of(childTask), parentTask);

        // then
        verify(taskBridge, never()).execute(any(Task.class), any(TaskBridgeContext.class));
        verify(taskStore, times(1)).update(argThat(task ->
                task.getStatus() == TaskStatus.WAITING_DEPENDENCY));
    }

    @Test
    void testExecuteChildTasks_DependencyMet() {
        // given
        String depTaskId = "dep-" + UUID.randomUUID();
        childTask.setDependencyTaskIds(List.of(depTaskId));

        Task depTask = Task.builder()
                .id(depTaskId)
                .status(TaskStatus.COMPLETED) // dependency completed
                .build();

        Task completedTask = Task.builder()
                .id(childTaskId)
                .status(TaskStatus.COMPLETED)
                .build();

        when(taskStore.findById(depTaskId)).thenReturn(Optional.of(depTask));
        when(taskStore.findById(childTaskId)).thenReturn(Optional.of(childTask));
        when(taskStore.findById(parentTaskId)).thenReturn(Optional.of(parentTask));
        when(taskBridge.execute(any(Task.class), any(TaskBridgeContext.class)))
                .thenReturn(completedTask);
        when(taskStore.findRunnableChildTasks(parentTaskId)).thenReturn(List.of());

        // when
        executionService.executeChildTasks(List.of(childTask), parentTask);

        // then
        verify(taskBridge).execute(any(Task.class), any(TaskBridgeContext.class));
    }

    @Test
    void testCheckAndCompleteParentTask_AllChildrenCompleted() {
        // given
        String child1Id = "child-1-" + UUID.randomUUID();
        String child2Id = "child-2-" + UUID.randomUUID();

        parentTask.setChildTaskIds(List.of(child1Id, child2Id));

        Task child1 = Task.builder()
                .id(child1Id)
                .parentTaskId(parentTaskId)
                .status(TaskStatus.COMPLETED)
                .build();

        Task child2 = Task.builder()
                .id(child2Id)
                .parentTaskId(parentTaskId)
                .status(TaskStatus.COMPLETED)
                .build();

        when(taskStore.findById(child1Id)).thenReturn(Optional.of(child1));
        when(taskStore.findById(child2Id)).thenReturn(Optional.of(child2));
        when(taskStore.findById(childTaskId)).thenReturn(Optional.of(childTask));

        Task completedChild = Task.builder()
                .id(childTaskId)
                .status(TaskStatus.COMPLETED)
                .build();

        when(taskBridge.execute(any(Task.class), any(TaskBridgeContext.class)))
                .thenReturn(completedChild);
        when(taskStore.findRunnableChildTasks(parentTaskId)).thenReturn(List.of());

        // when
        executionService.executeChildTask(childTask, parentTask);

        // then
        verify(taskStore, atLeastOnce()).update(argThat(task -> {
            if (task.getId().equals(parentTaskId)) {
                return task.getStatus() == TaskStatus.COMPLETED;
            }
            return true;
        }));
    }

    @Test
    void testCheckAndCompleteParentTask_OneChildFailed() {
        // given
        String child1Id = "child-1-" + UUID.randomUUID();

        parentTask.setChildTaskIds(List.of(child1Id, childTaskId));

        Task child1 = Task.builder()
                .id(child1Id)
                .parentTaskId(parentTaskId)
                .status(TaskStatus.FAILED)
                .build();

        when(taskStore.findById(child1Id)).thenReturn(Optional.of(child1));
        when(taskStore.findById(childTaskId)).thenReturn(Optional.of(childTask));

        Task completedChild = Task.builder()
                .id(childTaskId)
                .status(TaskStatus.COMPLETED)
                .build();

        when(taskBridge.execute(any(Task.class), any(TaskBridgeContext.class)))
                .thenReturn(completedChild);
        when(taskStore.findRunnableChildTasks(parentTaskId)).thenReturn(List.of());

        // when
        executionService.executeChildTask(childTask, parentTask);

        // then
        verify(taskStore, atLeastOnce()).update(argThat(task -> {
            if (task.getId().equals(parentTaskId)) {
                return task.getStatus() == TaskStatus.FAILED;
            }
            return true;
        }));
    }

    @Test
    void testNotifyDependencyResolved() {
        // given
        String dependencyTaskId = "dep-" + UUID.randomUUID();
        String waitingTaskId = "waiting-" + UUID.randomUUID();

        Task waitingTask = Task.builder()
                .id(waitingTaskId)
                .status(TaskStatus.WAITING_DEPENDENCY)
                .dependencyTaskIds(List.of(dependencyTaskId))
                .build();

        Task anotherDepTask = Task.builder()
                .id("another-dep-" + UUID.randomUUID())
                .status(TaskStatus.PENDING) // not yet completed
                .build();

        Task completedDep = Task.builder()
                .id(dependencyTaskId)
                .status(TaskStatus.COMPLETED)
                .build();

        when(taskStore.findWaitingTasksByDependencyTaskId(dependencyTaskId))
                .thenReturn(List.of(waitingTask));
        when(taskStore.findById(dependencyTaskId)).thenReturn(Optional.of(completedDep));
        when(taskStore.findById(waitingTaskId)).thenReturn(Optional.of(waitingTask));

        // when
        executionService.notifyDependencyResolved(dependencyTaskId);

        // then
        verify(taskBridge).notifyDependencyResolved(null, dependencyTaskId);
    }

    @Test
    void testExecuteChildTasks_ExceptionHandling() {
        // given
        when(taskBridge.execute(any(Task.class), any(TaskBridgeContext.class)))
                .thenThrow(new RuntimeException("Unexpected error"));
        when(taskStore.findById(childTaskId)).thenReturn(Optional.of(childTask));

        // when
        executionService.executeChildTasks(List.of(childTask), parentTask);

        // then
        verify(taskStore, atLeastOnce()).update(argThat(task ->
                task.getStatus() == TaskStatus.FAILED &&
                        "Unexpected error".equals(task.getErrorMessage())));
        verify(taskNotifier).notifyFailed(any(Task.class), eq("Unexpected error"));
    }

    @Test
    void testParentTaskWithNoChildTasks() {
        // given - parent has no child tasks
        parentTask.setChildTaskIds(List.of());

        Task completedTask = Task.builder()
                .id(childTaskId)
                .status(TaskStatus.COMPLETED)
                .build();

        when(taskBridge.execute(any(Task.class), any(TaskBridgeContext.class)))
                .thenReturn(completedTask);
        when(taskStore.findById(childTaskId)).thenReturn(Optional.of(childTask));

        // when - should not throw NPE
        executionService.executeChildTask(childTask, parentTask);

        // then - verify execution completed
        verify(taskBridge).execute(any(Task.class), any(TaskBridgeContext.class));
    }

    @Test
    void testDependencyWithPartialCompletion_StillBlocked() {
        // given - task has 2 dependencies, one completed one not
        String dep1Id = "dep-1-" + UUID.randomUUID();
        String dep2Id = "dep-2-" + UUID.randomUUID();
        childTask.setDependencyTaskIds(List.of(dep1Id, dep2Id));

        Task completedDep = Task.builder()
                .id(dep1Id)
                .status(TaskStatus.COMPLETED)
                .build();

        Task pendingDep = Task.builder()
                .id(dep2Id)
                .status(TaskStatus.PENDING) // not yet completed
                .build();

        when(taskStore.findById(dep1Id)).thenReturn(Optional.of(completedDep));
        when(taskStore.findById(dep2Id)).thenReturn(Optional.of(pendingDep));

        // when
        executionService.executeChildTasks(List.of(childTask), parentTask);

        // then - execution should NOT happen because one dependency is still pending
        verify(taskBridge, never()).execute(any(Task.class), any(TaskBridgeContext.class));
        verify(taskStore, times(1)).update(argThat(task ->
                task.getStatus() == TaskStatus.WAITING_DEPENDENCY));
    }

    @Test
    void testAllDependenciesCompleted_Executes() {
        // given - task has 2 dependencies, both completed
        String dep1Id = "dep-1-" + UUID.randomUUID();
        String dep2Id = "dep-2-" + UUID.randomUUID();
        childTask.setDependencyTaskIds(List.of(dep1Id, dep2Id));

        Task completedDep1 = Task.builder()
                .id(dep1Id)
                .status(TaskStatus.COMPLETED)
                .build();

        Task completedDep2 = Task.builder()
                .id(dep2Id)
                .status(TaskStatus.COMPLETED)
                .build();

        Task resultTask = Task.builder()
                .id(childTaskId)
                .status(TaskStatus.COMPLETED)
                .build();

        when(taskStore.findById(dep1Id)).thenReturn(Optional.of(completedDep1));
        when(taskStore.findById(dep2Id)).thenReturn(Optional.of(completedDep2));
        when(taskBridge.execute(any(Task.class), any(TaskBridgeContext.class)))
                .thenReturn(resultTask);
        when(taskStore.findById(childTaskId)).thenReturn(Optional.of(childTask));
        when(taskStore.findById(parentTaskId)).thenReturn(Optional.of(parentTask));
        when(taskStore.findRunnableChildTasks(parentTaskId)).thenReturn(List.of());

        // when
        executionService.executeChildTasks(List.of(childTask), parentTask);

        // then - execution SHOULD happen because all dependencies are completed
        verify(taskBridge).execute(any(Task.class), any(TaskBridgeContext.class));
    }
}
