package org.dragon.workspace.service.task.arrangement;

import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.workspace.service.task.arrangement.dto.ChildTaskPlan;
import org.dragon.workspace.service.task.arrangement.dto.TaskDecompositionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChildTaskFactory 单元测试
 */
class ChildTaskFactoryTest {

    private ChildTaskFactory childTaskFactory;

    private Task parentTask;
    private String parentTaskId;
    private String workspaceId;

    @BeforeEach
    void setUp() {
        childTaskFactory = new ChildTaskFactory();
        parentTaskId = "parent-" + UUID.randomUUID();
        workspaceId = "workspace-" + UUID.randomUUID();

        parentTask = Task.builder()
                .id(parentTaskId)
                .workspaceId(workspaceId)
                .name("Parent Task")
                .description("Parent Task Description")
                .creatorId("creator-1")
                .input("Parent input data")
                .status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .metadata(Map.of("key", "value"))
                .sourceChatId("chat-123")
                .sourceMessageId("msg-456")
                .sourceChannel("feishu")
                .build();
    }

    @Test
    void testCreateChildTasks_SingleTask() {
        TaskDecompositionResult result = TaskDecompositionResult.builder()
                .summary("Decomposition summary")
                .collaborationMode("AUTO")
                .childTasks(List.of(
                        ChildTaskPlan.builder()
                                .planTaskId("plan-1")
                                .name("Child Task 1")
                                .description("Child Description 1")
                                .characterId("char-1")
                                .build()
                ))
                .build();

        List<Task> childTasks = childTaskFactory.createChildTasks(result, parentTask);

        assertEquals(1, childTasks.size());
        Task child = childTasks.get(0);
        assertNotNull(child.getId());
        assertEquals(parentTaskId, child.getParentTaskId());
        assertEquals(workspaceId, child.getWorkspaceId());
        assertEquals("Child Task 1", child.getName());
        assertEquals("Child Description 1", child.getDescription());
        assertEquals("char-1", child.getCharacterId());
        assertEquals(TaskStatus.PENDING, child.getStatus());
        assertEquals("Parent input data", child.getInput());
    }

    @Test
    void testCreateChildTasks_MultipleTasksWithDependencies() {
        TaskDecompositionResult result = TaskDecompositionResult.builder()
                .summary("Decomposition summary")
                .collaborationMode("SEQUENTIAL")
                .childTasks(List.of(
                        ChildTaskPlan.builder()
                                .planTaskId("plan-1")
                                .name("Task A")
                                .description("Task A Description")
                                .characterId("char-a")
                                .dependencyPlanTaskIds(List.of())
                                .build(),
                        ChildTaskPlan.builder()
                                .planTaskId("plan-2")
                                .name("Task B")
                                .description("Task B Description")
                                .characterId("char-b")
                                .dependencyPlanTaskIds(List.of("plan-1"))
                                .build(),
                        ChildTaskPlan.builder()
                                .planTaskId("plan-3")
                                .name("Task C")
                                .description("Task C Description")
                                .characterId("char-c")
                                .dependencyPlanTaskIds(List.of("plan-1", "plan-2"))
                                .build()
                ))
                .build();

        List<Task> childTasks = childTaskFactory.createChildTasks(result, parentTask);

        assertEquals(3, childTasks.size());

        // Task A - no dependencies
        Task taskA = childTasks.stream().filter(t -> t.getName().equals("Task A")).findFirst().orElseThrow();
        assertTrue(taskA.getDependencyTaskIds() == null || taskA.getDependencyTaskIds().isEmpty());

        // Task B - depends on Task A
        Task taskB = childTasks.stream().filter(t -> t.getName().equals("Task B")).findFirst().orElseThrow();
        assertEquals(1, taskB.getDependencyTaskIds().size());
        assertEquals(taskA.getId(), taskB.getDependencyTaskIds().get(0));

        // Task C - depends on Task A and Task B
        Task taskC = childTasks.stream().filter(t -> t.getName().equals("Task C")).findFirst().orElseThrow();
        assertEquals(2, taskC.getDependencyTaskIds().size());
        assertTrue(taskC.getDependencyTaskIds().contains(taskA.getId()));
        assertTrue(taskC.getDependencyTaskIds().contains(taskB.getId()));
    }

    @Test
    void testCreateChildTasks_InheritsParentMetadata() {
        TaskDecompositionResult result = TaskDecompositionResult.builder()
                .childTasks(List.of(
                        ChildTaskPlan.builder()
                                .planTaskId("plan-1")
                                .name("Child Task")
                                .description("Description")
                                .characterId("char-1")
                                .build()
                ))
                .build();

        List<Task> childTasks = childTaskFactory.createChildTasks(result, parentTask);

        Task child = childTasks.get(0);
        assertEquals("chat-123", child.getSourceChatId());
        assertEquals("msg-456", child.getSourceMessageId());
        assertEquals("feishu", child.getSourceChannel());
        assertEquals("value", child.getMetadata().get("key"));
    }

    @Test
    void testCreateChildTasks_EmptyResult() {
        TaskDecompositionResult emptyResult = TaskDecompositionResult.builder()
                .childTasks(List.of())
                .build();

        List<Task> childTasks = childTaskFactory.createChildTasks(emptyResult, parentTask);

        assertTrue(childTasks.isEmpty());
    }

    @Test
    void testCreateChildTasks_NullResult() {
        List<Task> childTasks = childTaskFactory.createChildTasks(null, parentTask);

        assertTrue(childTasks.isEmpty());
    }

    @Test
    void testCreateChildTasks_NullChildTasks() {
        TaskDecompositionResult result = TaskDecompositionResult.builder()
                .childTasks(null)
                .build();

        List<Task> childTasks = childTaskFactory.createChildTasks(result, parentTask);

        assertTrue(childTasks.isEmpty());
    }

    @Test
    void testCreateChildTasks_InvalidDependencyPlanTaskId() {
        TaskDecompositionResult result = TaskDecompositionResult.builder()
                .childTasks(List.of(
                        ChildTaskPlan.builder()
                                .planTaskId("plan-1")
                                .name("Task A")
                                .description("Description")
                                .characterId("char-1")
                                .dependencyPlanTaskIds(List.of())
                                .build(),
                        ChildTaskPlan.builder()
                                .planTaskId("plan-2")
                                .name("Task B")
                                .description("Description")
                                .characterId("char-2")
                                .dependencyPlanTaskIds(List.of("non-existent-plan-id"))
                                .build()
                ))
                .build();

        List<Task> childTasks = childTaskFactory.createChildTasks(result, parentTask);

        Task taskB = childTasks.stream().filter(t -> t.getName().equals("Task B")).findFirst().orElseThrow();
        // Invalid dependency should be ignored, resulting in empty list
        assertTrue(taskB.getDependencyTaskIds().isEmpty());
    }

    @Test
    void testCreateChildTasks_UniqueIds() {
        TaskDecompositionResult result = TaskDecompositionResult.builder()
                .childTasks(List.of(
                        ChildTaskPlan.builder()
                                .planTaskId("plan-1")
                                .name("Task 1")
                                .description("Description")
                                .characterId("char-1")
                                .build(),
                        ChildTaskPlan.builder()
                                .planTaskId("plan-2")
                                .name("Task 2")
                                .description("Description")
                                .characterId("char-2")
                                .build(),
                        ChildTaskPlan.builder()
                                .planTaskId("plan-3")
                                .name("Task 3")
                                .description("Description")
                                .characterId("char-3")
                                .build()
                ))
                .build();

        List<Task> childTasks = childTaskFactory.createChildTasks(result, parentTask);

        // All IDs should be unique
        long uniqueIds = childTasks.stream().map(Task::getId).distinct().count();
        assertEquals(3, uniqueIds);
    }

    @Test
    void testCreateChildTasks_AllPropertiesCorrectlyMapped() {
        // given
        TaskDecompositionResult result = TaskDecompositionResult.builder()
                .summary("Test decomposition")
                .collaborationMode("SEQUENTIAL")
                .childTasks(List.of(
                        ChildTaskPlan.builder()
                                .planTaskId("plan-1")
                                .name("Child Task Name")
                                .description("Child Task Description")
                                .characterId("char-123")
                                .characterRole("developer")
                                .expectedOutput("Code output")
                                .needsUserInput(false)
                                .build()
                ))
                .build();

        // when
        List<Task> childTasks = childTaskFactory.createChildTasks(result, parentTask);

        // then
        assertEquals(1, childTasks.size());
        Task child = childTasks.get(0);

        // Verify parent relationship
        assertEquals(parentTaskId, child.getParentTaskId());
        assertEquals(workspaceId, child.getWorkspaceId());

        // Verify task properties from plan
        assertEquals("Child Task Name", child.getName());
        assertEquals("Child Task Description", child.getDescription());
        assertEquals("char-123", child.getCharacterId());

        // Verify status is PENDING
        assertEquals(TaskStatus.PENDING, child.getStatus());

        // Verify timestamps are set
        assertNotNull(child.getCreatedAt());
        assertNotNull(child.getUpdatedAt());

        // Verify input is inherited from parent
        assertEquals("Parent input data", child.getInput());
    }

    @Test
    void testCreateChildTasks_CharacterIdCanBeNull() {
        // given - characterId is null (unassigned)
        TaskDecompositionResult result = TaskDecompositionResult.builder()
                .childTasks(List.of(
                        ChildTaskPlan.builder()
                                .planTaskId("plan-1")
                                .name("Unassigned Task")
                                .description("Description")
                                .characterId(null) // explicitly null
                                .build()
                ))
                .build();

        // when
        List<Task> childTasks = childTaskFactory.createChildTasks(result, parentTask);

        // then - should create task with null characterId without error
        assertEquals(1, childTasks.size());
        assertNull(childTasks.get(0).getCharacterId());
    }

    @Test
    void testCreateChildTasks_BuilderDefaultValues() {
        // given
        TaskDecompositionResult result = TaskDecompositionResult.builder()
                .childTasks(List.of(
                        ChildTaskPlan.builder()
                                .planTaskId("plan-1")
                                .name("Task")
                                .description("Desc")
                                .build()
                ))
                .build();

        // when
        List<Task> childTasks = childTaskFactory.createChildTasks(result, parentTask);

        // then
        Task child = childTasks.get(0);
        // Verify lists are initialized (not null)
        assertNotNull(child.getExecutionSteps());
        assertNotNull(child.getExecutionMessages());
        assertNotNull(child.getAssignedMemberIds());
        assertNotNull(child.getDependencyTaskIds());
    }
}
