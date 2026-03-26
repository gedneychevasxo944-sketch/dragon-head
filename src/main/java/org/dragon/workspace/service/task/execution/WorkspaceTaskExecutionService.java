package org.dragon.workspace.service.task.execution;

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
import org.dragon.workspace.material.Material;
import org.dragon.workspace.service.material.WorkspaceMaterialService;
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
    private final WorkspaceMaterialService materialService;

    /**
     * 执行单个子任务
     *
     * @param childTask 子任务
     * @param parentTask 父任务
     */
    public void executeChildTask(Task childTask, Task parentTask) {
        String workspaceId = parentTask.getWorkspaceId();

        log.info("[TaskExecutionService] Executing childTask {} on character {}",
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
            log.info("[TaskExecutionService] ChildTask {} completed successfully", childTask.getId());
            taskNotifier.notifyCompleted(result);
            // 通知依赖已解决，触发等待此任务的依赖任务重调度
            notifyDependencyResolved(childTask.getId());
            // 同步拉起同一父任务下其他可执行的子任务
            executeRunnableChildTasks(parentTask.getId(), parentTask);
            // 通知依赖已解决
            if (result.getCollaborationSessionId() != null) {
                chatRoom.markParticipantReady(result.getCollaborationSessionId(), result.getCharacterId());
            }
        } else if (result.getStatus() == TaskStatus.FAILED) {
            log.warn("[TaskExecutionService] ChildTask {} failed: {}", childTask.getId(), result.getErrorMessage());
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
                .materialContext(buildMaterialContext(childTask, parentTask.getWorkspaceId()))
                .build();
    }

    /**
     * 根据任务和可见性构建物料上下文
     */
    private String buildMaterialContext(Task task, String workspaceId) {
        if (task.getMaterialIds() == null || task.getMaterialIds().isEmpty()) {
            return null;
        }

        StringBuilder context = new StringBuilder();
        for (String materialId : task.getMaterialIds()) {
            Optional<Material> materialOpt = materialService.get(materialId);
            if (materialOpt.isEmpty()) {
                continue;
            }
            Material material = materialOpt.get();

            // 检查可见性
            if (material.getVisibility() == Material.VisibilityScope.CHARACTER_SCOPED) {
                // 需要匹配当前 character
                if (task.getCharacterId() == null
                        || !task.getCharacterId().equals(material.getVisibilityTargetId())) {
                    continue;
                }
            } else if (material.getVisibility() == Material.VisibilityScope.TASK_SCOPED) {
                // 需要匹配当前任务
                if (!task.getId().equals(material.getVisibilityTargetId())) {
                    continue;
                }
            }
            // WORKSPACE_SCOPED 默认可访问

            // 获取解析内容
            Optional<org.dragon.workspace.material.ParsedMaterialContent> contentOpt =
                    materialService.getParsedContent(materialId);
            if (contentOpt.isPresent() && contentOpt.get().getTextContent() != null) {
                context.append(String.format("[%s]: %s\n",
                        material.getName(), contentOpt.get().getTextContent()));
            }
        }

        return context.length() > 0 ? context.toString() : null;
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
                    log.warn("[TaskExecutionService] Dependencies not met for task {}, marking as WAITING_DEPENDENCY", childTask.getId());
                    childTask.setStatus(TaskStatus.WAITING_DEPENDENCY);
                    taskStore.update(childTask);
                    taskNotifier.notifyWaiting(childTask, "Waiting for dependency");
                    continue;
                }

                executeChildTask(childTask, parentTask);

            } catch (Exception e) {
                log.error("[TaskExecutionService] Error executing childTask {}: {}", childTask.getId(), e.getMessage(), e);
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
            log.info("[TaskExecutionService] Parent task {} completed", parentTask.getId());
        } else if (anyFailed) {
            parentTask.setStatus(TaskStatus.FAILED);
            taskStore.update(parentTask);
            log.warn("[TaskExecutionService] Parent task {} has failed child tasks", parentTask.getId());
        } else if (!anyRunning && anyWaiting) {
            // 没有正在运行的，但有等待中的
            parentTask.setStatus(TaskStatus.RUNNING); // 保持运行状态
            taskStore.update(parentTask);
        }
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
                log.error("[TaskExecutionService] Error executing runnable child task {}: {}",
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
