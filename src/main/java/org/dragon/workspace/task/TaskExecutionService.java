package org.dragon.workspace.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.material.Material;
import org.dragon.store.StoreFactory;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.workspace.cooperation.chat.ChatMessage;
import org.dragon.workspace.cooperation.chat.ChatRoom;
import org.dragon.workspace.cooperation.chat.ChatSession;
import org.dragon.workspace.cooperation.task.notify.WorkspaceTaskNotifier;
import org.dragon.workspace.material.WorkspaceMaterialService;
import org.dragon.workspace.task.event.TaskEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 任务执行服务
 * <p>
 * 职责：
 * - 执行子任务
 * - 状态更新
 * - 发送通知
 * <p>
 * 不负责（移至 TaskDependencyService）：
 * - 依赖检查
 * - 依赖解决通知
 * - 父任务状态检查
 * - 重调度
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutionService {

    private final TaskBridge taskBridge;
    private final WorkspaceTaskNotifier taskNotifier;
    private final ChatRoom chatRoom;
    private final WorkspaceMaterialService materialService;
    private final TaskDependencyService taskDependencyService;
    private final TaskEventPublisher taskEventPublisher;
    private final StoreFactory storeFactory;

    private TaskStore getTaskStore() {
        return storeFactory.get(TaskStore.class);
    }

    /**
     * 执行单个子任务
     */
    public void executeChildTask(Task childTask, Task parentTask) {
        String workspaceId = parentTask.getWorkspaceId();

        log.info("[TaskExecutionService] Executing childTask {} on character {}", childTask.getId(), childTask.getCharacterId());

        // 更新状态为 RUNNING
        childTask.setStatus(TaskStatus.RUNNING);
        childTask.setStartedAt(LocalDateTime.now());
        getTaskStore().update(childTask);

        // 发送开始执行通知
        taskNotifier.notifyStarted(childTask);

        // 发送进度通知
        taskNotifier.notifyProgress(childTask, "任务执行中");

        // 组装协作上下文
        TaskBridgeContext context = buildCollaborationContext(childTask, parentTask);

        // 委托 TaskBridge 执行
        Task result = taskBridge.execute(childTask, context);

        // 处理不同执行结果
        handleExecutionResult(result, childTask, parentTask);
    }

    /**
     * 执行 SPECIFIED 模式任务 - 直接执行，跳过子任务封装
     *
     * @param task 任务（其 parentTaskId 作为父任务ID）
     * @param characterId 指定的 Character ID
     */
    public void executeSpecifiedTask(Task task, String characterId) {
        log.info("[TaskExecutionService] Executing specified task {} on character {}", task.getId(), characterId);

        task.setCharacterId(characterId);
        task.setStatus(TaskStatus.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        getTaskStore().update(task);

        taskNotifier.notifyStarted(task);
        taskNotifier.notifyProgress(task, "任务执行中");

        // 最小化上下文，无协作会话
        TaskBridgeContext context = TaskBridgeContext.builder()
                .workspaceId(task.getWorkspaceId())
                .parentTaskId(task.getParentTaskId())
                .build();

        Task result = taskBridge.execute(task, context);

        // 处理执行结果（无递归子任务调度）
        handleSpecifiedExecutionResult(result, task);
    }

    /**
     * 处理 SPECIFIED 模式执行结果
     */
    private void handleSpecifiedExecutionResult(Task result, Task task) {
        if (result.getStatus() == TaskStatus.WAITING_DEPENDENCY) {
            taskNotifier.notifyWaiting(result, result.getWaitingReason());
        } else if (result.getStatus() == TaskStatus.WAITING_USER_INPUT) {
            if (result.getLastQuestion() != null) {
                taskNotifier.notifyQuestion(result, result.getLastQuestion());
            }
        } else if (result.getStatus() == TaskStatus.COMPLETED) {
            log.info("[TaskExecutionService] Specified task {} completed successfully", task.getId());
            taskNotifier.notifyCompleted(result);
            taskDependencyService.notifyDependencyResolved(task.getId());
        } else if (result.getStatus() == TaskStatus.FAILED) {
            log.warn("[TaskExecutionService] Specified task {} failed: {}", task.getId(), result.getErrorMessage());
            taskNotifier.notifyFailed(result, result.getErrorMessage());
        }
    }

    /**
     * 执行多个子任务（串行执行）
     */
    public void executeChildTasks(List<Task> childTasks, Task parentTask) {
        for (Task childTask : childTasks) {
            try {
                // 检查依赖是否满足
                if (!taskDependencyService.areDependenciesMet(childTask)) {
                    log.warn("[TaskExecutionService] Dependencies not met for task {}, marking as WAITING_DEPENDENCY", childTask.getId());
                    childTask.setStatus(TaskStatus.WAITING_DEPENDENCY);
                    getTaskStore().update(childTask);
                    taskNotifier.notifyWaiting(childTask, "Waiting for dependency");
                    continue;
                }

                executeChildTask(childTask, parentTask);

            } catch (Exception e) {
                log.error("[TaskExecutionService] Error executing childTask {}: {}", childTask.getId(), e.getMessage(), e);
                childTask.setStatus(TaskStatus.FAILED);
                childTask.setErrorMessage(e.getMessage());
                getTaskStore().update(childTask);
                taskNotifier.notifyFailed(childTask, e.getMessage());
            }
        }
    }

    /**
     * 处理执行结果
     */
    private void handleExecutionResult(Task result, Task childTask, Task parentTask) {
        // 如果任务进入等待依赖状态，同步更新 ChatRoom 参与者状态
        if (result.getStatus() == TaskStatus.WAITING_DEPENDENCY) {
            String sessionId = childTask.getCollaborationSessionId();
            String reason = result.getWaitingReason() != null ? result.getWaitingReason() : "Waiting for dependency";
            if (sessionId != null) {
                chatRoom.markParticipantWaiting(sessionId, result.getCharacterId(), reason);
            }
            taskNotifier.notifyWaiting(result, reason);
        }

        // 如果任务进入等待用户输入状态，同步更新 ChatRoom 参与者状态并通知用户
        if (result.getStatus() == TaskStatus.WAITING_USER_INPUT) {
            String sessionId = childTask.getCollaborationSessionId();
            if (sessionId != null) {
                String reason = result.getLastQuestion() != null ? result.getLastQuestion() : "Waiting for user input";
                chatRoom.markParticipantWaiting(sessionId, result.getCharacterId(), reason);
            }
            if (result.getLastQuestion() != null) {
                taskNotifier.notifyQuestion(result, result.getLastQuestion());
            }
        }

        // 记录执行结果并发送通知
        if (result.getStatus() == TaskStatus.COMPLETED) {
            log.info("[TaskExecutionService] ChildTask {} completed successfully", childTask.getId());
            taskNotifier.notifyCompleted(result);

            // 通知依赖已解决，触发等待此任务的依赖任务重调度
            taskDependencyService.notifyDependencyResolved(childTask.getId());

            // 发布事件触发可执行子任务调度（避免递归）
            taskEventPublisher.publishChildCompleted(result, parentTask);

            // 标记参与者就绪
            if (result.getCollaborationSessionId() != null) {
                chatRoom.markParticipantReady(result.getCollaborationSessionId(), result.getCharacterId());
            }

            // 检查并更新父任务状态
            taskDependencyService.checkAndCompleteParentTask(parentTask);

        } else if (result.getStatus() == TaskStatus.FAILED) {
            log.warn("[TaskExecutionService] ChildTask {} failed: {}", childTask.getId(), result.getErrorMessage());
            taskNotifier.notifyFailed(result, result.getErrorMessage());
            taskDependencyService.checkAndCompleteParentTask(parentTask);
        } else if (result.getStatus() == TaskStatus.SUSPENDED) {
            taskNotifier.notifyWaiting(result, result.getErrorMessage());
        }
    }

    /**
     * 构建协作上下文
     */
    private TaskBridgeContext buildCollaborationContext(Task childTask, Task parentTask) {
        String collaborationSessionId = childTask.getCollaborationSessionId();

        // 获取同级 Character ID 列表
        List<String> peerCharacterIds = new ArrayList<>();
        List<String> childTaskIds = parentTask.getChildTaskIds();
        if (childTaskIds != null) {
            for (String childTaskId : childTaskIds) {
                if (!childTaskId.equals(childTask.getId())) {
                    Optional<Task> siblingTask = getTaskStore().findById(childTaskId);
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

        // 获取最新会话消息
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
                if (task.getCharacterId() == null || !task.getCharacterId().equals(material.getVisibilityTargetId())) {
                    continue;
                }
            } else if (material.getVisibility() == Material.VisibilityScope.TASK_SCOPED) {
                if (!task.getId().equals(material.getVisibilityTargetId())) {
                    continue;
                }
            }

            // 获取解析内容
            Optional<org.dragon.material.ParsedMaterialContent> contentOpt = materialService.getParsedContent(materialId);
            if (contentOpt.isPresent() && contentOpt.get().getTextContent() != null) {
                context.append(String.format("[%s]: %s\n", material.getName(), contentOpt.get().getTextContent()));
            }
        }

        return context.length() > 0 ? context.toString() : null;
    }
}
