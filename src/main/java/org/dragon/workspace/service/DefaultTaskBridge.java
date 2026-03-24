package org.dragon.workspace.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.dragon.agent.react.Action;
import org.dragon.agent.react.ReActResult;
import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.CharacterRuntimeBinder;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认任务执行桥接器实现
 * 通过 Character#run() 真正执行任务
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultTaskBridge implements TaskBridge {

    private final CharacterRegistry characterRegistry;
    private final TaskStore taskStore;
    private final CharacterRuntimeBinder characterRuntimeBinder;

    @Override
    public Task execute(Task task, TaskBridgeContext context) {
        String workspaceId = context.getWorkspaceId();
        String characterId = task.getCharacterId();
        if (characterId == null || characterId.isEmpty()) {
            log.error("[DefaultTaskBridge] No characterId assigned to task {}", task.getId());
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("No character assigned");
            taskStore.update(task);
            return task;
        }

        // 获取 Character
        Character character = characterRegistry.get(characterId)
                .orElseThrow(() -> new IllegalStateException("Character not found: " + characterId));

        // 绑定运行时依赖（ReActExecutor, PromptManager 等）
        characterRuntimeBinder.bind(character, workspaceId);

        // 更新任务状态为 RUNNING
        task.setStatus(TaskStatus.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        taskStore.update(task);

        try {
            // 将任务输入转为字符串
            Object input = task.getInput();
            String userInput;
            if (input instanceof NormalizedMessage) {
                userInput = ((NormalizedMessage) input).getTextContent();
            } else {
                userInput = input != null ? input.toString() : "";
            }

            // 调用 Character 执行（透传 Task 和 bridgeContext 以便获取协作上下文）
            ReActResult result = character.runReAct(userInput, false, task, context);

            // 检查是否有 STATUS_CHANGE
            Action.StatusChange statusChange = result.getStatusChange();
            if (statusChange != null) {
                return applyStatusChange(task, statusChange, context);
            }

            // 执行成功
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            task.setResult(result.getResponse());
            taskStore.update(task);

            log.info("[DefaultTaskBridge] Task {} executed successfully on character {}", task.getId(), characterId);
            return task;

        } catch (Exception e) {
            log.error("[DefaultTaskBridge] Task {} execution failed: {}", task.getId(), e.getMessage(), e);
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            taskStore.update(task);
            return task;
        }
    }

    /**
     * 应用状态变更
     *
     * @param task 任务
     * @param statusChange 状态变更信息
     * @param context 桥接上下文
     * @return 更新后的任务
     */
    private Task applyStatusChange(Task task, Action.StatusChange statusChange, TaskBridgeContext context) {
        String targetStatus = statusChange.getTargetStatus();

        if ("WAITING_DEPENDENCY".equals(targetStatus)) {
            task.setStatus(TaskStatus.WAITING_DEPENDENCY);
            task.setWaitingReason(statusChange.getReason());
            if (statusChange.getDependencyTaskId() != null && task.getDependencyTaskIds() != null) {
                task.getDependencyTaskIds().add(statusChange.getDependencyTaskId());
            }
            log.info("[DefaultTaskBridge] Task {} changed to WAITING_DEPENDENCY: {}", task.getId(), statusChange.getReason());

        } else if ("WAITING_USER_INPUT".equals(targetStatus)) {
            task.setStatus(TaskStatus.WAITING_USER_INPUT);
            task.setLastQuestion(statusChange.getQuestion());
            task.setWaitingReason(statusChange.getReason());
            log.info("[DefaultTaskBridge] Task {} changed to WAITING_USER_INPUT: {}", task.getId(), statusChange.getQuestion());

        } else if ("SUSPENDED".equals(targetStatus)) {
            task.setStatus(TaskStatus.SUSPENDED);
            task.setErrorMessage(statusChange.getReason());
            log.info("[DefaultTaskBridge] Task {} changed to SUSPENDED: {}", task.getId(), statusChange.getReason());

        } else {
            log.warn("[DefaultTaskBridge] Unknown targetStatus: {}, treating as COMPLETED", targetStatus);
            task.setStatus(TaskStatus.COMPLETED);
        }

        task.setUpdatedAt(LocalDateTime.now());
        taskStore.update(task);

        return task;
    }

    @Override
    public Task suspend(Task task, SuspendContext context) {
        task.setStatus(TaskStatus.SUSPENDED);
        task.setErrorMessage(context.getReason());
        task.setUpdatedAt(LocalDateTime.now());
        taskStore.update(task);

        log.info("[DefaultTaskBridge] Task {} suspended: {}", task.getId(), context.getReason());
        return task;
    }

    @Override
    public Task resume(Task task, ResumeContext context) {
        // 更新输入（追加用户回复）
        if (context.getNewInput() != null) {
            Object currentInput = task.getInput();
            task.setInput(currentInput != null ? currentInput.toString() + "\n" + context.getNewInput().toString() : context.getNewInput().toString());
        }

        // 恢复为 RUNNING
        task.setStatus(TaskStatus.RUNNING);
        task.setUpdatedAt(LocalDateTime.now());
        taskStore.update(task);

        log.info("[DefaultTaskBridge] Task {} resumed", task.getId());

        // 继续执行
        TaskBridgeContext bridgeContext = TaskBridgeContext.builder()
                .workspaceId(task.getWorkspaceId())
                .build();
        return execute(task, bridgeContext);
    }

    @Override
    public void notifyDependencyResolved(String taskId, String dependencyTaskId) {
        // 查找所有等待此依赖的任务
        List<Task> waitingTasks = taskStore.findWaitingTasksByDependencyTaskId(dependencyTaskId);
        for (Task task : waitingTasks) {
            // 检查所有依赖是否都已完成
            if (areAllDependenciesMet(task)) {
                log.info("[DefaultTaskBridge] All dependencies resolved for task {}, re-scheduling", task.getId());
                task.setStatus(TaskStatus.PENDING);
                task.setUpdatedAt(LocalDateTime.now());
                taskStore.update(task);
            }
        }
        log.info("[DefaultTaskBridge] Dependency {} resolved, checked {} waiting tasks", dependencyTaskId, waitingTasks.size());
    }

    /**
     * 检查任务的所有依赖是否都已完成
     */
    private boolean areAllDependenciesMet(Task task) {
        List<String> depIds = task.getDependencyTaskIds();
        if (depIds == null || depIds.isEmpty()) {
            return true;
        }
        for (String depId : depIds) {
            Optional<Task> depTask = taskStore.findById(depId);
            if (depTask.isEmpty() || depTask.get().getStatus() != TaskStatus.COMPLETED) {
                return false;
            }
        }
        return true;
    }
}
