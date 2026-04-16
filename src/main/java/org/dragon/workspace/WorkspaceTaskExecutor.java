package org.dragon.workspace;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.store.StoreFactory;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.workspace.context.StepResult;
import org.dragon.workspace.context.TaskContext;
import org.dragon.workspace.cooperation.chat.ChatRoom;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.member.WorkspaceMemberService;
import org.dragon.workspace.plugin.WorkspacePluginRegistry;
import org.dragon.workspace.step.AssignStep;
import org.dragon.workspace.step.CompleteStep;
import org.dragon.workspace.step.DecomposeStep;
import org.dragon.workspace.step.DependencyStep;
import org.dragon.workspace.step.ExecuteStep;
import org.dragon.workspace.step.NotifyStep;
import org.dragon.workspace.step.ObserveStep;
import org.dragon.workspace.step.ResumeStep;
import org.dragon.workspace.step.Step;
import org.dragon.workspace.step.StepRegistry;
import org.dragon.workspace.task.dto.TaskCreationCommand;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceTaskExecutor - WorkspaceTask 执行器
 *
 * <p>负责构建 WorkspaceTask 实例并执行。
 *
 * @author yijunw
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceTaskExecutor {

    private final WorkspaceRegistry workspaceRegistry;
    private final WorkspaceMemberService memberService;
    private final ChatRoom chatRoom;
    private final StoreFactory storeFactory;

    // Step implementations
    private final ResumeStep resumeStep;
    private final DecomposeStep decomposeStep;
    private final AssignStep assignStep;
    private final ExecuteStep executeStep;
    private final DependencyStep dependencyStep;
    private final CompleteStep completeStep;
    private final NotifyStep notifyStep;
    private final ObserveStep observeStep;

    // Plugin registry
    private final WorkspacePluginRegistry pluginRegistry;

    /**
     * 提交新任务并执行
     */
    public Task submitAndExecute(String workspaceId, TaskCreationCommand command) {
        Workspace workspace = workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        // 创建任务
        Task task = Task.builder()
                .id(UUID.randomUUID().toString())
                .workspaceId(workspaceId)
                .name(command.getTaskName())
                .description(command.getTaskDescription())
                .input(command.getInput())
                .creatorId(command.getCreatorId())
                .status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .metadata(command.getMetadata())
                .sourceChatId(command.getSourceChatId())
                .sourceMessageId(command.getSourceMessageId())
                .sourceChannel(command.getSourceChannel())
                .build();

        storeFactory.get(TaskStore.class).save(task);
        log.info("[WorkspaceTaskExecutor] Submitted task {} to workspace {}", task.getId(), workspaceId);

        // 构建上下文并执行
        TaskContext ctx = buildContext(task, workspaceId);
        executeTask(ctx);

        return task;
    }

    /**
     * 继续执行已有任务
     */
    public Task resumeAndExecute(String workspaceId, Task task, NormalizedMessage message) {
        // 追加用户输入
        Object currentInput = task.getInput();
        if (currentInput != null) {
            task.setInput(currentInput.toString() + "\n" + message.getTextContent());
        } else {
            task.setInput(message.getTextContent());
        }
        task.setStatus(TaskStatus.RUNNING);
        task.setUpdatedAt(LocalDateTime.now());
        storeFactory.get(TaskStore.class).update(task);

        Workspace workspace = workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        TaskContext ctx = buildContext(task, workspaceId);
        ctx.setConfigValue("resume", true);

        executeTask(ctx);

        return task;
    }

    /**
     * 执行任务（Step DAG 驱动）
     */
    private void executeTask(TaskContext ctx) {
        // 构建 WorkspaceTask（Step DAG）
        org.dragon.workspace.WorkspaceTask workspaceTask = buildWorkspaceTask(ctx.getWorkspaceId());

        // 执行
        workspaceTask.execute(ctx);

        // 处理完成后的通知
        handleCompletion(ctx);
    }

    /**
     * 构建 WorkspaceTask 实例
     */
    private org.dragon.workspace.WorkspaceTask buildWorkspaceTask(String workspaceId) {
        org.dragon.workspace.WorkspaceTask task = org.dragon.workspace.WorkspaceTask.builder()
                .id("workspace-task-" + workspaceId)
                .workspaceId(workspaceId)
                .stepRegistry(buildStepRegistry())
                .pluginRegistry(pluginRegistry)
                .build();

        // 设置 DAG 结构
        // ResumeStep -> (DecisionStep) -> DecomposeStep -> AssignStep -> ExecuteStep -> DependencyStep
        //                                                              ↓
        //                                                         NotifyStep -> ObserveStep
        //                                                              ↓
        //                                                         CompleteStep

        task.addStep(resumeStep, Set.of());  // ResumeStep 无依赖
        task.addStep(decomposeStep, Set.of("resume"));  // 依赖 resume
        task.addStep(assignStep, Set.of("decompose"));  // 依赖 decompose
        task.addStep(executeStep, Set.of("assign"));  // 依赖 assign
        task.addStep(dependencyStep, Set.of("execute"));  // 依赖 execute
        task.addStep(notifyStep, Set.of("execute"));  // 依赖 execute（与 dependency 并行）
        task.addStep(observeStep, Set.of("notify"));  // 依赖 notify
        task.addStep(completeStep, Set.of("dependency", "observe"));  // 依赖 dependency 和 observe

        // 设置循环中止条件
        task.setTerminationCondition((completed, results, ctx) -> {
            // 所有核心步骤完成
            return completed.contains("complete");
        });

        return task;
    }

    /**
     * 构建 Step 注册表
     */
    private StepRegistry buildStepRegistry() {
        StepRegistry registry = new StepRegistry();
        registry.register("resume", resumeStep);
        registry.register("decompose", decomposeStep);
        registry.register("assign", assignStep);
        registry.register("execute", executeStep);
        registry.register("dependency", dependencyStep);
        registry.register("notify", notifyStep);
        registry.register("observe", observeStep);
        registry.register("complete", completeStep);
        return registry;
    }

    /**
     * 构建执行上下文
     */
    private TaskContext buildContext(Task task, String workspaceId) {
        List<WorkspaceMember> members = memberService.listMembers(workspaceId);

        return TaskContext.builder()
                .workspaceId(workspaceId)
                .task(task)
                .members(members)
                .chatRoom(chatRoom)
                .build();
    }

    /**
     * 处理任务完成后的逻辑
     */
    private void handleCompletion(TaskContext ctx) {
        Task task = ctx.getTask();
        Map<String, StepResult> stepResults = ctx.getStepResultsForCurrentTask();

        // 检查是否有失败的 Step
        boolean hasFailure = stepResults.values().stream()
                .anyMatch(r -> !r.isSuccess());

        if (hasFailure) {
            log.warn("[WorkspaceTaskExecutor] Task {} completed with failures", task.getId());
        } else {
            log.info("[WorkspaceTaskExecutor] Task {} completed successfully", task.getId());
        }
    }

    private Map<String, StepResult> getStepResultsForCurrentTask() {
        return java.util.Map.of();
    }
}
