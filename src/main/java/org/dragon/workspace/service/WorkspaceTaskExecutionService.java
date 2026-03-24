package org.dragon.workspace.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.workspace.chat.ChatMessage;
import org.dragon.workspace.chat.ChatRoom;
import org.dragon.workspace.chat.ChatSession;
import org.dragon.workspace.task.notify.WorkspaceTaskNotifier;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Workspace 任务执行服务
 * 负责执行子任务，支持按依赖顺序串行执行
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceTaskExecutionService {

    private final TaskStore taskStore;
    private final TaskBridge taskBridge;
    private final WorkspaceTaskNotifier taskNotifier;
    private final ChatRoom chatRoom;

    /**
     * 执行单个子任务
     *
     * @param childTask 子任务
     * @param parentTask 父任务
     */
    public void executeChildTask(Task childTask, Task parentTask) {
        String workspaceId = parentTask.getWorkspaceId();

        log.info("[WorkspaceTaskExecutionService] Executing childTask {} on character {}",
                childTask.getId(), childTask.getCharacterId());

        // 更新状态为 RUNNING
        childTask.setStatus(TaskStatus.RUNNING);
        childTask.setStartedAt(LocalDateTime.now());
        taskStore.update(childTask);

        // 发送开始执行通知
        taskNotifier.notifyStarted(childTask);

        // 发送进度通知
        taskNotifier.notifyProgress(childTask, "任务执行中");

        // 组装协作上下文
        TaskBridgeContext context = buildCollaborationContext(childTask, parentTask);

        // 委托 TaskBridge 执行
        Task result = taskBridge.execute(childTask, context);

        // 如果任务进入等待依赖状态，同步更新 ChatRoom 参与者状态
        if (result.getStatus() == TaskStatus.WAITING_DEPENDENCY) {
            String sessionId = childTask.getCollaborationSessionId();
            if (sessionId != null) {
                String reason = result.getWaitingReason() != null ? result.getWaitingReason() : "Waiting for dependency";
                chatRoom.markParticipantWaiting(sessionId, result.getCharacterId(), reason);
            }
        }

        // 如果任务进入等待用户输入状态，同步更新 ChatRoom 参与者状态并通知用户
        if (result.getStatus() == TaskStatus.WAITING_USER_INPUT) {
            String sessionId = childTask.getCollaborationSessionId();
            if (sessionId != null) {
                String reason = result.getLastQuestion() != null ? result.getLastQuestion() : "Waiting for user input";
                chatRoom.markParticipantWaiting(sessionId, result.getCharacterId(), reason);
            }
            // 通知用户
            if (result.getLastQuestion() != null) {
                taskNotifier.notifyQuestion(result, result.getLastQuestion());
            }
        }

        // 记录执行结果并发送通知
        if (result.getStatus() == TaskStatus.COMPLETED) {
            log.info("[WorkspaceTaskExecutionService] ChildTask {} completed successfully", childTask.getId());
            taskNotifier.notifyCompleted(result);
            // 通知依赖已解决
            if (result.getCollaborationSessionId() != null) {
                chatRoom.markParticipantReady(result.getCollaborationSessionId(), result.getCharacterId());
            }
        } else if (result.getStatus() == TaskStatus.FAILED) {
            log.warn("[WorkspaceTaskExecutionService] ChildTask {} failed: {}", childTask.getId(), result.getErrorMessage());
            taskNotifier.notifyFailed(result, result.getErrorMessage());
        } else if (result.getStatus() == TaskStatus.SUSPENDED) {
            taskNotifier.notifyWaiting(result, result.getErrorMessage());
        }

        // 检查并更新父任务状态
        checkAndCompleteParentTask(parentTask);
    }

    /**
     * 构建协作上下文
     */
    private TaskBridgeContext buildCollaborationContext(Task childTask, Task parentTask) {
        String collaborationSessionId = childTask.getCollaborationSessionId();

        // 获取同级 Character ID 列表（同一父任务下的其他子任务的执行者）
        List<String> peerCharacterIds = new ArrayList<>();
        List<String> childTaskIds = parentTask.getChildTaskIds();
        if (childTaskIds != null) {
            for (String childTaskId : childTaskIds) {
                if (!childTaskId.equals(childTask.getId())) {
                    Optional<Task> siblingTask = taskStore.findById(childTaskId);
                    if (siblingTask.isPresent() && siblingTask.get().getCharacterId() != null) {
                        peerCharacterIds.add(siblingTask.get().getCharacterId());
                    }
                }
            }
        }

        // 协作会话状态信息
        java.util.Map<String, String> participantStates = null;
        List<String> blockedParticipants = null;
        String sessionStatus = null;

        // 获取最新会话消息（格式化）
        List<String> latestSessionMessages = new ArrayList<>();
        if (collaborationSessionId != null) {
            ChatSession session = chatRoom.getSession(collaborationSessionId);
            if (session != null) {
                participantStates = session.getParticipantStates();
                blockedParticipants = session.getBlockedParticipants();
                sessionStatus = session.getStatus() != null ? session.getStatus().name() : null;
            }

            List<ChatMessage> messages = chatRoom.listSessionMessages(collaborationSessionId, 10);
            latestSessionMessages = messages.stream()
                    .map(msg -> String.format("[sender=%s][purpose=%s][subtype=%s] %s",
                            msg.getSenderId(),
                            msg.getTaskPurpose() != null ? msg.getTaskPurpose().name() : "null",
                            msg.getMessageType(),
                            msg.getContent()))
                    .collect(Collectors.toList());
        }

        return TaskBridgeContext.builder()
                .workspaceId(parentTask.getWorkspaceId())
                .parentTaskId(parentTask.getId())
                .collaborationSessionId(collaborationSessionId)
                .peerCharacterIds(peerCharacterIds.isEmpty() ? null : peerCharacterIds)
                .dependencyTaskIds(childTask.getDependencyTaskIds())
                .latestSessionMessages(latestSessionMessages.isEmpty() ? null : latestSessionMessages)
                .participantStates(participantStates)
                .blockedParticipants(blockedParticipants)
                .sessionStatus(sessionStatus)
                .build();
    }

    /**
     * 执行多个子任务（串行执行）
     * 第一版先串行，保留后续并发扩展点
     *
     * @param childTasks 子任务列表
     * @param parentTask 父任务
     */
    public void executeChildTasks(List<Task> childTasks, Task parentTask) {
        for (Task childTask : childTasks) {
            try {
                // 检查依赖是否满足
                if (!areDependenciesMet(childTask)) {
                    // 依赖未满足，跳过执行（后续可改为加入等待队列）
                    log.warn("[WorkspaceTaskExecutionService] Dependencies not met for task {}, marking as WAITING_DEPENDENCY", childTask.getId());
                    childTask.setStatus(TaskStatus.WAITING_DEPENDENCY);
                    taskStore.update(childTask);
                    taskNotifier.notifyWaiting(childTask, "Waiting for dependency");
                    continue;
                }

                executeChildTask(childTask, parentTask);

            } catch (Exception e) {
                log.error("[WorkspaceTaskExecutionService] Error executing childTask {}: {}", childTask.getId(), e.getMessage(), e);
                childTask.setStatus(TaskStatus.FAILED);
                childTask.setErrorMessage(e.getMessage());
                taskStore.update(childTask);
                taskNotifier.notifyFailed(childTask, e.getMessage());
            }
        }
    }

    /**
     * 检查依赖是否满足
     *
     * @param task 任务
     * @return 依赖是否满足
     */
    private boolean areDependenciesMet(Task task) {
        List<String> dependencyTaskIds = task.getDependencyTaskIds();
        if (dependencyTaskIds == null || dependencyTaskIds.isEmpty()) {
            return true;
        }

        // 检查所有依赖任务是否已完成
        for (String depTaskId : dependencyTaskIds) {
            Optional<Task> depTask = taskStore.findById(depTaskId);
            if (depTask.isEmpty() || depTask.get().getStatus() != TaskStatus.COMPLETED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查并更新父任务状态
     */
    private void checkAndCompleteParentTask(Task parentTask) {
        List<String> childTaskIds = parentTask.getChildTaskIds();
        if (childTaskIds == null || childTaskIds.isEmpty()) {
            return;
        }

        List<Task> childTasks = childTaskIds.stream()
                .map(taskStore::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        boolean allCompleted = childTasks.stream()
                .allMatch(st -> st.getStatus() == TaskStatus.COMPLETED);
        boolean anyFailed = childTasks.stream()
                .anyMatch(st -> st.getStatus() == TaskStatus.FAILED);
        boolean anyRunning = childTasks.stream()
                .anyMatch(st -> st.getStatus() == TaskStatus.RUNNING);
        boolean anyWaiting = childTasks.stream()
                .anyMatch(st -> st.getStatus() == TaskStatus.WAITING_DEPENDENCY
                        || st.getStatus() == TaskStatus.WAITING_USER_INPUT
                        || st.getStatus() == TaskStatus.SUSPENDED);

        if (allCompleted) {
            parentTask.setStatus(TaskStatus.COMPLETED);
            parentTask.setCompletedAt(LocalDateTime.now());
            parentTask.setResult("All child tasks completed successfully");
            taskStore.update(parentTask);
            log.info("[WorkspaceTaskExecutionService] Parent task {} completed", parentTask.getId());
        } else if (anyFailed) {
            parentTask.setStatus(TaskStatus.FAILED);
            taskStore.update(parentTask);
            log.warn("[WorkspaceTaskExecutionService] Parent task {} has failed child tasks", parentTask.getId());
        } else if (!anyRunning && anyWaiting) {
            // 没有正在运行的，但有等待中的
            parentTask.setStatus(TaskStatus.RUNNING); // 保持运行状态
            taskStore.update(parentTask);
        }
    }

    /**
     * 暂停任务
     */
    public Task suspendTask(String workspaceId, String taskId, String reason) {
        Task task = taskStore.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        SuspendContext context = SuspendContext.builder()
                .reason(reason)
                .suspendedAt(LocalDateTime.now().toString())
                .build();
        return taskBridge.suspend(task, context);
    }

    /**
     * 恢复任务
     */
    public Task resumeTask(String workspaceId, String taskId, Object newInput) {
        Task task = taskStore.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        ResumeContext context = ResumeContext.builder()
                .newInput(newInput)
                .reason("User resumed")
                .build();
        return taskBridge.resume(task, context);
    }

    /**
     * 标记任务等待用户输入
     */
    public Task markWaitingUserInput(String workspaceId, String taskId, String question) {
        Task task = taskStore.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        task.setStatus(TaskStatus.WAITING_USER_INPUT);
        task.setErrorMessage(question); // 存储最后的问题
        task.setUpdatedAt(LocalDateTime.now());
        taskStore.update(task);

        log.info("[WorkspaceTaskExecutionService] Task {} waiting for user input: {}", taskId, question);
        taskNotifier.notifyQuestion(task, question);
        return task;
    }

    /**
     * 标记任务等待依赖
     */
    public Task markWaitingDependency(String workspaceId, String taskId, String dependencyTaskId) {
        Task task = taskStore.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // 添加依赖
        List<String> deps = task.getDependencyTaskIds();
        if (deps == null) {
            deps = new ArrayList<>();
            task.setDependencyTaskIds(deps);
        }
        if (!deps.contains(dependencyTaskId)) {
            deps.add(dependencyTaskId);
        }

        task.setStatus(TaskStatus.WAITING_DEPENDENCY);
        task.setUpdatedAt(LocalDateTime.now());
        taskStore.update(task);

        log.info("[WorkspaceTaskExecutionService] Task {} waiting for dependency: {}", taskId, dependencyTaskId);
        taskNotifier.notifyWaiting(task, "Waiting for dependency: " + dependencyTaskId);

        // 同步更新 ChatRoom 参与者状态
        String sessionId = task.getCollaborationSessionId();
        if (sessionId != null && task.getCharacterId() != null) {
            chatRoom.markParticipantWaiting(sessionId, task.getCharacterId(), "Waiting for dependency: " + dependencyTaskId);
        }

        return task;
    }

    /**
     * 通知依赖已解决，触发重调度
     *
     * @param dependencyTaskId 已解决的依赖任务 ID
     */
    public void notifyDependencyResolved(String dependencyTaskId) {
        taskBridge.notifyDependencyResolved(null, dependencyTaskId);
    }

    /**
     * 执行父任务下所有可执行的子任务
     *
     * @param parentTaskId 父任务 ID
     * @param parentTask 父任务
     * @return 被调度的子任务列表
     */
    public List<Task> executeRunnableChildTasks(String parentTaskId, Task parentTask) {
        List<Task> runnableTasks = taskStore.findRunnableChildTasks(parentTaskId);
        for (Task task : runnableTasks) {
            try {
                executeChildTask(task, parentTask);
            } catch (Exception e) {
                log.error("[WorkspaceTaskExecutionService] Error executing runnable child task {}: {}",
                        task.getId(), e.getMessage(), e);
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMessage(e.getMessage());
                taskStore.update(task);
                taskNotifier.notifyFailed(task, e.getMessage());
            }
        }
        return runnableTasks;
    }
}
