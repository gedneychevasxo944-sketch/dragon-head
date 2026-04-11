package org.dragon.workspace.service.task.integration;

import org.dragon.agent.react.ReActResult;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.workspace.cooperation.task.TaskBridge;
import org.dragon.workspace.cooperation.task.TaskBridgeContext;
import org.dragon.workspace.service.task.execution.WorkspaceTaskExecutionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Task 执行流程集成测试
 *
 * <p>测试执行层的核心逻辑：
 * <ol>
 *   <li>依赖检查：任务是否等待依赖完成</li>
 *   <li>执行成功：任务正常完成</li>
 *   <li>执行失败：任务失败状态传播</li>
 * </ol>
 */
class TaskExecutionIntegrationTest extends TaskIntegrationTestBase {

    @Test
    @DisplayName("依赖检查 - 部分依赖未完成时应阻塞")
    void testDependencyCheck_PartialDependencies() {
        // given
        TaskStore taskStore = storeFactory.get(TaskStore.class);

        Task childTask = Task.builder()
                .id("child-task-partial-" + System.currentTimeMillis())
                .parentTaskId("parent-task-partial")
                .workspaceId(workspaceId)
                .characterId(characterId)
                .status(TaskStatus.PENDING)
                .dependencyTaskIds(List.of("dep-task-pending"))
                .createdAt(LocalDateTime.now())
                .build();

        // Mock 依赖任务未完成
        Task depTask = Task.builder()
                .id("dep-task-pending")
                .status(TaskStatus.PENDING) // 未完成
                .build();

        Task parentTask = Task.builder()
                .id("parent-task-partial")
                .workspaceId(workspaceId)
                .status(TaskStatus.RUNNING)
                .childTaskIds(List.of(childTask.getId()))
                .createdAt(LocalDateTime.now())
                .build();

        taskStore.save(depTask);
        taskStore.save(childTask);
        taskStore.save(parentTask);

        // when - 尝试执行子任务
        executionService.executeChildTasks(List.of(childTask), parentTask);

        // then - 任务应该被标记为等待依赖
        Task updated = taskStore.findById(childTask.getId()).orElseThrow();
        assertEquals(TaskStatus.WAITING_DEPENDENCY, updated.getStatus());
    }

    @Test
    @DisplayName("依赖检查 - 所有依赖完成时应执行")
    void testDependencyCheck_AllDependenciesMet() {
        // given
        TaskStore taskStore = storeFactory.get(TaskStore.class);

        String childTaskId = "child-task-all-deps-" + System.currentTimeMillis();

        Task childTask = Task.builder()
                .id(childTaskId)
                .parentTaskId("parent-task-all-deps")
                .workspaceId(workspaceId)
                .characterId(characterId)
                .status(TaskStatus.PENDING)
                .dependencyTaskIds(List.of("dep-task-completed"))
                .createdAt(LocalDateTime.now())
                .build();

        // Mock 依赖任务已完成
        Task depTask = Task.builder()
                .id("dep-task-completed")
                .status(TaskStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();

        Task parentTask = Task.builder()
                .id("parent-task-all-deps")
                .workspaceId(workspaceId)
                .status(TaskStatus.RUNNING)
                .childTaskIds(List.of(childTaskId))
                .createdAt(LocalDateTime.now())
                .build();

        taskStore.save(depTask);
        taskStore.save(childTask);
        taskStore.save(parentTask);

        // when - 使用 mock taskBridge 避免真正执行
        TaskBridge mockBridge = mock(TaskBridge.class);
        Task completedResult = Task.builder()
                .id(childTaskId)
                .status(TaskStatus.COMPLETED)
                .result("Task completed successfully")
                .completedAt(LocalDateTime.now())
                .build();
        when(mockBridge.execute(any(Task.class), any(TaskBridgeContext.class))).thenReturn(completedResult);

        // 使用测试专用的 executionService，它使用 mockBridge
        WorkspaceTaskExecutionService testExecutionService = new WorkspaceTaskExecutionService(
                mockBridge, taskNotifier, chatRoom, materialService, storeFactory
        );

        testExecutionService.executeChildTasks(List.of(childTask), parentTask);

        // then - 验证 taskBridge 被调用了
        verify(mockBridge, times(1)).execute(any(Task.class), any(TaskBridgeContext.class));
    }

    @Test
    @DisplayName("执行异常处理 - 异常时任务标记为 FAILED")
    void testExecutionExceptionHandling() {
        // given
        TaskStore taskStore = storeFactory.get(TaskStore.class);

        String childTaskId = "child-exception-" + System.currentTimeMillis();

        Task childTask = Task.builder()
                .id(childTaskId)
                .parentTaskId("parent-exception")
                .workspaceId(workspaceId)
                .characterId(characterId)
                .status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Task parentTask = Task.builder()
                .id("parent-exception")
                .workspaceId(workspaceId)
                .status(TaskStatus.RUNNING)
                .childTaskIds(List.of(childTaskId))
                .createdAt(LocalDateTime.now())
                .build();

        taskStore.save(childTask);
        taskStore.save(parentTask);

        // Mock taskBridge 抛出异常
        TaskBridge mockBridge = mock(TaskBridge.class);
        when(mockBridge.execute(any(Task.class), any(TaskBridgeContext.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        WorkspaceTaskExecutionService testExecutionService = new WorkspaceTaskExecutionService(
                mockBridge, taskNotifier, chatRoom, materialService, storeFactory
        );

        // when
        testExecutionService.executeChildTasks(List.of(childTask), parentTask);

        // then
        Task updated = taskStore.findById(childTaskId).orElseThrow();
        assertEquals(TaskStatus.FAILED, updated.getStatus());
        assertEquals("Unexpected error", updated.getErrorMessage());

        // 验证通知被发送
        verify(taskNotifier, times(1)).notifyFailed(any(Task.class), eq("Unexpected error"));
    }

    @Test
    @DisplayName("等待依赖任务 - notifyDependencyResolved 触发重调度")
    void testNotifyDependencyResolved_TriggersRescheduling() {
        // given
        String dependencyTaskId = "resolved-dep-task-" + System.currentTimeMillis();

        Task waitingTask = Task.builder()
                .id("waiting-task-" + System.currentTimeMillis())
                .workspaceId(workspaceId)
                .characterId(characterId)
                .status(TaskStatus.WAITING_DEPENDENCY)
                .dependencyTaskIds(List.of(dependencyTaskId))
                .createdAt(LocalDateTime.now())
                .build();

        Task depTask = Task.builder()
                .id(dependencyTaskId)
                .workspaceId(workspaceId)
                .status(TaskStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();

        TaskStore taskStore = storeFactory.get(TaskStore.class);
        taskStore.save(depTask);
        taskStore.save(waitingTask);

        // Mock taskBridge
        TaskBridge mockBridge = mock(TaskBridge.class);
        WorkspaceTaskExecutionService testExecutionService = new WorkspaceTaskExecutionService(
                mockBridge, taskNotifier, chatRoom, materialService, storeFactory
        );

        // when
        testExecutionService.notifyDependencyResolved(dependencyTaskId);

        // then - TaskBridge 的 notifyDependencyResolved 应该被调用
        verify(mockBridge, times(1)).notifyDependencyResolved(null, dependencyTaskId);
    }

    @Test
    @DisplayName("执行完成 - 状态流转正确")
    void testExecutionComplete_StatusTransition() {
        // given
        TaskStore taskStore = storeFactory.get(TaskStore.class);

        String childTaskId = "child-complete-" + System.currentTimeMillis();

        Task childTask = Task.builder()
                .id(childTaskId)
                .parentTaskId("parent-complete")
                .workspaceId(workspaceId)
                .characterId(characterId)
                .status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Task parentTask = Task.builder()
                .id("parent-complete")
                .workspaceId(workspaceId)
                .status(TaskStatus.RUNNING)
                .childTaskIds(List.of(childTaskId))
                .createdAt(LocalDateTime.now())
                .build();

        taskStore.save(childTask);
        taskStore.save(parentTask);

        // Mock 返回完成状态
        TaskBridge mockBridge = mock(TaskBridge.class);
        Task completedResult = Task.builder()
                .id(childTaskId)
                .status(TaskStatus.COMPLETED)
                .result("Task completed successfully")
                .completedAt(LocalDateTime.now())
                .build();

        when(mockBridge.execute(any(Task.class), any(TaskBridgeContext.class)))
                .thenReturn(completedResult);

        WorkspaceTaskExecutionService testExecutionService = new WorkspaceTaskExecutionService(
                mockBridge, taskNotifier, chatRoom, materialService, storeFactory
        );

        // when
        testExecutionService.executeChildTask(childTask, parentTask);

        // then
        verify(taskNotifier, times(1)).notifyStarted(any(Task.class));
        verify(taskNotifier, times(1)).notifyProgress(any(Task.class), anyString());
        verify(taskNotifier, times(1)).notifyCompleted(any(Task.class));
    }

    @Test
    @DisplayName("执行失败 - 失败状态正确传播")
    void testExecutionFailed_StatusPropagation() {
        // given
        TaskStore taskStore = storeFactory.get(TaskStore.class);

        String childTaskId = "child-fail-" + System.currentTimeMillis();

        Task childTask = Task.builder()
                .id(childTaskId)
                .parentTaskId("parent-fail")
                .workspaceId(workspaceId)
                .characterId(characterId)
                .status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Task parentTask = Task.builder()
                .id("parent-fail")
                .workspaceId(workspaceId)
                .status(TaskStatus.RUNNING)
                .childTaskIds(List.of(childTaskId))
                .createdAt(LocalDateTime.now())
                .build();

        taskStore.save(childTask);
        taskStore.save(parentTask);

        // Mock 返回失败状态
        TaskBridge mockBridge = mock(TaskBridge.class);
        Task failedResult = Task.builder()
                .id(childTaskId)
                .status(TaskStatus.FAILED)
                .errorMessage("Execution failed")
                .build();

        when(mockBridge.execute(any(Task.class), any(TaskBridgeContext.class)))
                .thenReturn(failedResult);

        WorkspaceTaskExecutionService testExecutionService = new WorkspaceTaskExecutionService(
                mockBridge, taskNotifier, chatRoom, materialService, storeFactory
        );

        // when
        testExecutionService.executeChildTask(childTask, parentTask);

        // then
        verify(taskNotifier, times(1)).notifyFailed(any(Task.class), eq("Execution failed"));
    }
}
