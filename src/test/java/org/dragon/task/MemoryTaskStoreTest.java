package org.dragon.task;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemoryTaskStore 单元测试
 */
class MemoryTaskStoreTest {

    private MemoryTaskStore taskStore;

    private String testWorkspaceId;
    private String testCharacterId;
    private String testCreatorId;

    @BeforeEach
    void setUp() {
        taskStore = new MemoryTaskStore();
        testWorkspaceId = "workspace-" + UUID.randomUUID();
        testCharacterId = "character-" + UUID.randomUUID();
        testCreatorId = "creator-" + UUID.randomUUID();
    }

    @Test
    void testSaveAndFindById() {
        Task task = createTestTask();
        taskStore.save(task);

        Optional<Task> found = taskStore.findById(task.getId());
        assertTrue(found.isPresent());
        assertEquals(task.getId(), found.get().getId());
        assertEquals(task.getName(), found.get().getName());
    }

    @Test
    void testFindByIdNotFound() {
        Optional<Task> found = taskStore.findById("non-existent-id");
        assertFalse(found.isPresent());
    }

    @Test
    void testUpdate() {
        Task task = createTestTask();
        taskStore.save(task);

        task.setStatus(TaskStatus.RUNNING);
        task.setResult("Updated result");
        taskStore.update(task);

        Task found = taskStore.findById(task.getId()).orElseThrow();
        assertEquals(TaskStatus.RUNNING, found.getStatus());
        assertEquals("Updated result", found.getResult());
    }

    @Test
    void testDelete() {
        Task task = createTestTask();
        taskStore.save(task);

        assertTrue(taskStore.findById(task.getId()).isPresent());

        taskStore.delete(task.getId());

        assertFalse(taskStore.findById(task.getId()).isPresent());
    }

    @Test
    void testExists() {
        Task task = createTestTask();
        assertFalse(taskStore.exists(task.getId()));

        taskStore.save(task);
        assertTrue(taskStore.exists(task.getId()));
    }

    @Test
    void testFindByWorkspaceId() {
        Task task1 = createTestTask();
        task1.setWorkspaceId("other-workspace-1");
        Task task2 = createTestTask();
        task2.setWorkspaceId(testWorkspaceId);
        Task task3 = createTestTask();
        task3.setWorkspaceId("other-workspace-2");

        taskStore.save(task1);
        taskStore.save(task2);
        taskStore.save(task3);

        List<Task> found = taskStore.findByWorkspaceId(testWorkspaceId);
        assertEquals(1, found.size());
        assertEquals(testWorkspaceId, found.get(0).getWorkspaceId());
    }

    @Test
    void testFindByParentTaskId() {
        String parentTaskId = "parent-" + UUID.randomUUID();

        Task parent = createTestTask();
        parent.setId(parentTaskId);
        taskStore.save(parent);

        Task child1 = createTestTask();
        child1.setParentTaskId(parentTaskId);
        taskStore.save(child1);

        Task child2 = createTestTask();
        child2.setParentTaskId(parentTaskId);
        taskStore.save(child2);

        Task orphan = createTestTask();
        orphan.setParentTaskId("other-parent");
        taskStore.save(orphan);

        List<Task> children = taskStore.findByParentTaskId(parentTaskId);
        assertEquals(2, children.size());
        assertTrue(children.stream().allMatch(t -> parentTaskId.equals(t.getParentTaskId())));
    }

    @Test
    void testFindByStatus() {
        Task pending = createTestTask();
        pending.setStatus(TaskStatus.PENDING);
        taskStore.save(pending);

        Task running = createTestTask();
        running.setStatus(TaskStatus.RUNNING);
        taskStore.save(running);

        Task completed = createTestTask();
        completed.setStatus(TaskStatus.COMPLETED);
        taskStore.save(completed);

        List<Task> pendingTasks = taskStore.findByStatus(TaskStatus.PENDING);
        assertEquals(1, pendingTasks.size());
        assertEquals(TaskStatus.PENDING, pendingTasks.get(0).getStatus());

        List<Task> runningTasks = taskStore.findByStatus(TaskStatus.RUNNING);
        assertEquals(1, runningTasks.size());
    }

    @Test
    void testFindByCharacterId() {
        Task task1 = createTestTask();
        task1.setCharacterId(testCharacterId);
        taskStore.save(task1);

        Task task2 = createTestTask();
        task2.setCharacterId("other-character");
        taskStore.save(task2);

        List<Task> found = taskStore.findByCharacterId(testCharacterId);
        assertEquals(1, found.size());
        assertEquals(testCharacterId, found.get(0).getCharacterId());
    }

    @Test
    void testFindByCreatorId() {
        Task task1 = createTestTask();
        task1.setCreatorId(testCreatorId);
        taskStore.save(task1);

        Task task2 = createTestTask();
        task2.setCreatorId("other-creator");
        taskStore.save(task2);

        List<Task> found = taskStore.findByCreatorId(testCreatorId);
        assertEquals(1, found.size());
        assertEquals(testCreatorId, found.get(0).getCreatorId());
    }

    @Test
    void testFindByCollaborationSessionId() {
        String sessionId = "session-" + UUID.randomUUID();

        Task task1 = createTestTask();
        task1.setCollaborationSessionId(sessionId);
        taskStore.save(task1);

        Task task2 = createTestTask();
        task2.setCollaborationSessionId("other-session");
        taskStore.save(task2);

        List<Task> found = taskStore.findByCollaborationSessionId(sessionId);
        assertEquals(1, found.size());
        assertEquals(sessionId, found.get(0).getCollaborationSessionId());
    }

    @Test
    void testFindWaitingTasksByWorkspaceId() {
        Task suspended = createTestTask();
        suspended.setStatus(TaskStatus.SUSPENDED);
        suspended.setWorkspaceId(testWorkspaceId);
        taskStore.save(suspended);

        Task waitingInput = createTestTask();
        waitingInput.setStatus(TaskStatus.WAITING_USER_INPUT);
        waitingInput.setWorkspaceId(testWorkspaceId);
        taskStore.save(waitingInput);

        Task waitingDep = createTestTask();
        waitingDep.setStatus(TaskStatus.WAITING_DEPENDENCY);
        waitingDep.setWorkspaceId(testWorkspaceId);
        taskStore.save(waitingDep);

        Task completed = createTestTask();
        completed.setStatus(TaskStatus.COMPLETED);
        completed.setWorkspaceId(testWorkspaceId);
        taskStore.save(completed);

        List<Task> waitingTasks = taskStore.findWaitingTasksByWorkspaceId(testWorkspaceId);
        assertEquals(3, waitingTasks.size());
        assertTrue(waitingTasks.stream().allMatch(t ->
                t.getStatus() == TaskStatus.SUSPENDED ||
                t.getStatus() == TaskStatus.WAITING_USER_INPUT ||
                t.getStatus() == TaskStatus.WAITING_DEPENDENCY));
    }

    @Test
    void testFindRunnableChildTasks() {
        String parentTaskId = "parent-" + UUID.randomUUID();

        Task parent = createTestTask();
        parent.setId(parentTaskId);
        taskStore.save(parent);

        Task runnable1 = createTestTask();
        runnable1.setParentTaskId(parentTaskId);
        runnable1.setStatus(TaskStatus.PENDING);
        taskStore.save(runnable1);

        Task runnable2 = createTestTask();
        runnable2.setParentTaskId(parentTaskId);
        runnable2.setStatus(TaskStatus.PENDING);
        taskStore.save(runnable2);

        Task running = createTestTask();
        running.setParentTaskId(parentTaskId);
        running.setStatus(TaskStatus.RUNNING);
        taskStore.save(running);

        Task completed = createTestTask();
        completed.setParentTaskId(parentTaskId);
        completed.setStatus(TaskStatus.COMPLETED);
        taskStore.save(completed);

        List<Task> runnable = taskStore.findRunnableChildTasks(parentTaskId);
        assertEquals(2, runnable.size());
        assertTrue(runnable.stream().allMatch(t -> t.getStatus() == TaskStatus.PENDING));
    }

    @Test
    void testFindWaitingTasksByDependencyTaskId() {
        String depTaskId = "dep-task-" + UUID.randomUUID();

        Task waiting1 = createTestTask();
        waiting1.setStatus(TaskStatus.WAITING_DEPENDENCY);
        waiting1.setDependencyTaskIds(List.of(depTaskId));
        taskStore.save(waiting1);

        Task waiting2 = createTestTask();
        waiting2.setStatus(TaskStatus.WAITING_DEPENDENCY);
        waiting2.setDependencyTaskIds(List.of("other-dep"));
        taskStore.save(waiting2);

        Task pending = createTestTask();
        pending.setStatus(TaskStatus.PENDING);
        pending.setDependencyTaskIds(List.of(depTaskId));
        taskStore.save(pending);

        List<Task> waiting = taskStore.findWaitingTasksByDependencyTaskId(depTaskId);
        assertEquals(1, waiting.size());
        assertEquals(waiting1.getId(), waiting.get(0).getId());
    }

    @Test
    void testTaskWithDependencies() {
        Task task = createTestTask();
        task.setDependencyTaskIds(List.of("dep1", "dep2"));
        taskStore.save(task);

        Task found = taskStore.findById(task.getId()).orElseThrow();
        assertEquals(2, found.getDependencyTaskIds().size());
        assertTrue(found.getDependencyTaskIds().contains("dep1"));
        assertTrue(found.getDependencyTaskIds().contains("dep2"));
    }

    @Test
    void testTaskWithChildTaskIds() {
        Task parent = createTestTask();
        parent.setChildTaskIds(List.of("child1", "child2", "child3"));
        taskStore.save(parent);

        Task found = taskStore.findById(parent.getId()).orElseThrow();
        assertEquals(3, found.getChildTaskIds().size());
    }

    private Task createTestTask() {
        return Task.builder()
                .id(UUID.randomUUID().toString())
                .workspaceId(testWorkspaceId)
                .name("Test Task")
                .description("Test Description")
                .creatorId(testCreatorId)
                .characterId(testCharacterId)
                .status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}