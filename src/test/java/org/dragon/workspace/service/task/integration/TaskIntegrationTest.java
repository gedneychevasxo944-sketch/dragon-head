package org.dragon.workspace.service.task.integration;

import org.dragon.agent.react.ReActResult;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.workspace.service.task.arrangement.WorkspaceTaskArrangementService;
import org.dragon.workspace.service.task.arrangement.dto.TaskCreationCommand;
import org.dragon.workspace.service.task.arrangement.dto.TaskDecompositionResult;
import org.dragon.workspace.service.task.arrangement.dto.ChildTaskPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Task 模块集成测试 - 完整流程验证
 *
 * <p>测试目标：
 * <ol>
 *   <li>AUTO 模式：任务提交 → 分解 → 分配 → 执行 → 完成</li>
 *   <li>SPECIFIED 模式：指定 Character 执行任务</li>
 *   <li>DEFAULT 模式：使用默认 Character 执行</li>
 *   <li>任务依赖：子任务等待依赖完成</li>
 *   <li>协作会话：多 Character 共享会话</li>
 * </ol>
 */
class TaskIntegrationTest extends TaskIntegrationTestBase {

    @Test
    @DisplayName("AUTO 模式 - 任务完整执行流程")
    void testAutoMode_FullFlow() {
        // given
        TaskCreationCommand command = createTaskCommand(
                "测试任务",
                "这是一个测试任务，用于验证完整流程"
        );

        // when - 提交任务
        Task result = arrangementService.submitTask(
                workspaceId,
                command,
                WorkspaceTaskArrangementService.TaskExecutionMode.AUTO,
                null
        );

        // then - 验证任务已创建
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("测试任务", result.getName());
        assertEquals(TaskStatus.COMPLETED, result.getStatus());

        // 验证任务已保存
        assertTaskCreated(result.getId());

        // 验证分解器被调用
        verify(taskDecomposer, times(1)).decompose(any(Task.class), any(), anyList());

        // 验证任务分配器被调用
        verify(taskAssignmentResolver, times(1)).resolveAssignments(any(), any(), anyList());
    }

    @Test
    @DisplayName("SPECIFIED 模式 - 指定 Character 执行")
    void testSpecifiedMode() {
        // given
        TaskCreationCommand command = createTaskCommand(
                "指定模式任务",
                "使用指定的 Character 执行"
        );

        // when
        Task result = arrangementService.submitTask(
                workspaceId,
                command,
                WorkspaceTaskArrangementService.TaskExecutionMode.SPECIFIED,
                List.of(characterId)
        );

        // then
        assertNotNull(result);
        assertEquals(TaskStatus.COMPLETED, result.getStatus());
        assertTaskCreated(result.getId());

        // SPECIFIED 模式不调用分解器
        verify(taskDecomposer, never()).decompose(any(), any(), any());
    }

    @Test
    @DisplayName("DEFAULT 模式 - 使用默认 Character")
    void testDefaultMode() {
        // given
        TaskCreationCommand command = createTaskCommand(
                "默认模式任务",
                "使用默认 Character 执行"
        );

        // when
        Task result = arrangementService.submitTask(
                workspaceId,
                command,
                WorkspaceTaskArrangementService.TaskExecutionMode.DEFAULT,
                null
        );

        // then
        assertNotNull(result);
        assertEquals(TaskStatus.COMPLETED, result.getStatus());
        assertTaskCreated(result.getId());
    }

    @Test
    @DisplayName("AUTO 模式 - 分解失败处理")
    void testAutoMode_DecompositionFailed() {
        // given - 设置分解器返回空结果
        when(taskDecomposer.decompose(any(Task.class), any(), anyList()))
                .thenReturn(TaskDecompositionResult.builder().childTasks(List.of()).build());

        TaskCreationCommand command = createTaskCommand(
                "分解失败任务",
                "这个任务的分解会失败"
        );

        // when
        Task result = arrangementService.submitTask(
                workspaceId,
                command,
                WorkspaceTaskArrangementService.TaskExecutionMode.AUTO,
                null
        );

        // then - 任务应该标记为失败
        assertEquals(TaskStatus.FAILED, result.getStatus());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("AUTO 模式 - 无可用成员处理")
    void testAutoMode_NoMembers() {
        // given - 设置无可用成员
        when(memberService.listMembers(workspaceId)).thenReturn(List.of());

        TaskCreationCommand command = createTaskCommand(
                "无成员任务",
                "工作空间中没有可用成员"
        );

        // when
        Task result = arrangementService.submitTask(
                workspaceId,
                command,
                WorkspaceTaskArrangementService.TaskExecutionMode.AUTO,
                null
        );

        // then
        assertEquals(TaskStatus.FAILED, result.getStatus());
        assertEquals("No members available", result.getErrorMessage());
    }

    @Test
    @DisplayName("AUTO 模式 - 工作空间不存在")
    void testAutoMode_WorkspaceNotFound() {
        // given
        TaskCreationCommand command = createTaskCommand(
                "不存在工作空间任务",
                "这个任务会提交到不存在的工作空间"
        );

        // when & then
        assertThrows(IllegalArgumentException.class, () ->
                arrangementService.submitTask(
                        "non-existent-workspace",
                        command,
                        WorkspaceTaskArrangementService.TaskExecutionMode.AUTO,
                        null
                )
        );
    }

    @Test
    @DisplayName("任务重平衡 - rebalance 方法")
    void testRebalance() {
        // given
        String taskId = "test-task-" + System.currentTimeMillis();

        // when
        arrangementService.rebalance(taskId, new WorkspaceTaskArrangementService.ExecutionFeedback(
                "child-1", true, null, 1000L
        ));

        // then - rebalance 目前是 no-op，不抛异常即可
    }

    @Test
    @DisplayName("多子任务分解 - 验证依赖关系解析")
    void testMultiTaskDecomposition() {
        // given - 设置分解器返回多个有依赖关系的子任务
        TaskDecompositionResult multiResult = TaskDecompositionResult.builder()
                .summary("多任务分解")
                .collaborationMode("SEQUENTIAL")
                .childTasks(List.of(
                        ChildTaskPlan.builder()
                                .planTaskId("plan-1")
                                .name("Task A")
                                .description("First task")
                                .characterId(characterId)
                                .dependencyPlanTaskIds(List.of())
                                .build(),
                        ChildTaskPlan.builder()
                                .planTaskId("plan-2")
                                .name("Task B")
                                .description("Second task")
                                .characterId(characterId)
                                .dependencyPlanTaskIds(List.of("plan-1"))
                                .build(),
                        ChildTaskPlan.builder()
                                .planTaskId("plan-3")
                                .name("Task C")
                                .description("Third task")
                                .characterId(characterId)
                                .dependencyPlanTaskIds(List.of("plan-1", "plan-2"))
                                .build()
                ))
                .build();

        when(taskDecomposer.decompose(any(Task.class), any(), anyList()))
                .thenReturn(multiResult);
        when(taskAssignmentResolver.resolveAssignments(any(), any(), anyList()))
                .thenReturn(multiResult);

        TaskCreationCommand command = createTaskCommand(
                "多子任务",
                "分解为多个有依赖关系的子任务"
        );

        // when
        Task result = arrangementService.submitTask(
                workspaceId,
                command,
                WorkspaceTaskArrangementService.TaskExecutionMode.AUTO,
                null
        );

        // then - 任务创建成功
        assertNotNull(result);
        assertEquals(TaskStatus.COMPLETED, result.getStatus());

        // 验证有子任务
        assertNotNull(result.getChildTaskIds());
        assertEquals(3, result.getChildTaskIds().size());
    }
}
