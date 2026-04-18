package org.dragon.task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.dragon.asset.service.AssetAssociationService;
import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.runtime.CharacterRuntime;
import org.dragon.character.mind.TraitResolutionService;
import org.dragon.config.service.ConfigApplication;
import org.dragon.agent.model.ModelRegistry;
import org.dragon.skill.runtime.SkillRegistry;
import org.dragon.store.StoreFactory;
import org.dragon.step.StepResult;
import org.dragon.step.workspace.AssignStep;
import org.dragon.step.workspace.ClaimStep;
import org.dragon.step.workspace.CompleteStep;
import org.dragon.step.workspace.DecomposeStep;
import org.dragon.step.workspace.ExecuteStep;
import org.dragon.step.workspace.NotifyStep;
import org.dragon.step.workspace.ObserverEvalStep;
import org.dragon.step.workspace.ResumeStep;
import org.dragon.step.workspace.ResultStep;
import org.dragon.step.workspace.StepRegistry;
import org.dragon.workspace.WorkspaceFacadeService;
import org.dragon.workspace.WorkspaceTask;
import org.dragon.workspace.context.TaskContext;
import org.dragon.workspace.context.TaskContextConfig;
import org.dragon.workspace.cooperation.chat.ChatRoom;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.member.WorkspaceMemberService;
import org.dragon.workspace.plugin.WorkspacePluginRegistry;
import org.dragon.workspace.task.dto.TaskCreationCommand;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Task 执行服务 - 统一的 Character/Task 执行入口
 *
 * <p>所有任务执行都通过这里管理：
 * <ul>
 *   <li>Ad-hoc 执行（无 Workspace）- 直接调用 character.run()</li>
 *   <li>Workspace Task 执行 - Step DAG 驱动</li>
 * </ul>
 *
 * @author yijunw
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutionService {

    // Ad-hoc execution dependencies
    private final CharacterRegistry characterRegistry;
    private final ConfigApplication configApplication;
    private final ModelRegistry modelRegistry;
    private final SkillRegistry skillRegistry;
    private final TraitResolutionService traitResolutionService;
    private final AssetAssociationService assetAssociationService;
    private final StoreFactory storeFactory;

    // Workspace Task execution dependencies
    private final WorkspaceFacadeService workspaceFacadeService;
    private final WorkspaceMemberService memberService;
    private final WorkspacePluginRegistry pluginRegistry;

    // Step implementations
    private final ResumeStep resumeStep;
    private final DecomposeStep decomposeStep;
    private final AssignStep assignStep;
    private final ExecuteStep executeStep;
    private final ClaimStep claimStep;
    private final ResultStep resultStep;
    private final CompleteStep completeStep;
    private final NotifyStep notifyStep;
    private final ObserverEvalStep observerEvalStep;

    // ==================== Ad-hoc 执行（无 Workspace） ====================

    /**
     * 统一执行入口 - 所有 Character.run() 都走这里
     */
    public String execute(String characterId, String userInput) {
        return execute(characterId, userInput, null);
    }

    /**
     * 统一执行入口（带 Task 上下文）
     */
    public String execute(String characterId, String userInput, Task existingTask) {
        Character character = getCharacter(characterId);

        // 创建/复用 Task
        Task task = existingTask != null ? existingTask : createAdhocTask(characterId, userInput);

        // 绑定运行时
        bindRuntime(character, task.getWorkspaceId());

        // 执行
        return character.run(userInput, task);
    }

    private Character getCharacter(String characterId) {
        return characterRegistry.get(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));
    }

    /**
     * Ad-hoc Task 创建（无 Workspace 场景）
     */
    private Task createAdhocTask(String characterId, String userInput) {
        Task task = Task.builder()
                .id(UUID.randomUUID().toString())
                .workspaceId(null)  // Ad-hoc，无 Workspace
                .characterId(characterId)
                .name("ad-hoc-task")
                .input(userInput)
                .status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        storeFactory.get(TaskStore.class).save(task);
        log.info("[TaskExecutionService] Created ad-hoc task {} for character {}", task.getId(), characterId);
        return task;
    }

    private void bindRuntime(Character character, String workspaceId) {
        Long wsId = workspaceId != null ? Long.valueOf(workspaceId) : null;
        CharacterRuntime runtime = CharacterRuntime.builder()
                .configApplication(configApplication)
                .modelRegistry(modelRegistry)
                .skillRegistry(skillRegistry)
                .traitResolutionService(traitResolutionService)
                .assetAssociationService(assetAssociationService)
                .workspaceId(wsId)
                .build();
        character.setRuntime(runtime);
        log.info("[TaskExecutionService] Bound runtime for character {} in workspace {}",
                character.getId(), workspaceId);
    }

    // ==================== Workspace Task 执行（Step DAG） ====================

    /**
     * 提交新任务并执行
     */
    public Task submitAndExecute(String workspaceId, TaskCreationCommand command) {
        workspaceFacadeService.getWorkspace(workspaceId)
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
        log.info("[TaskExecutionService] Submitted task {} to workspace {}", task.getId(), workspaceId);

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

        workspaceFacadeService.getWorkspace(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        TaskContext ctx = buildContext(task, workspaceId);
        ctx.setConfigValue(TaskContextConfig.RESUME, true);

        executeTask(ctx);

        return task;
    }

    /**
     * 执行任务（Step DAG 驱动）
     */
    private void executeTask(TaskContext ctx) {
        // 构建 WorkspaceTask（Step DAG）
        WorkspaceTask workspaceTask = buildWorkspaceTask(ctx.getWorkspaceId());

        // 执行
        workspaceTask.execute(ctx);

        // 处理完成后的通知
        handleCompletion(ctx);
    }

    /**
     * 构建 WorkspaceTask 实例
     */
    private WorkspaceTask buildWorkspaceTask(String workspaceId) {
        WorkspaceTask task = WorkspaceTask.builder()
                .id("workspace-task-" + workspaceId)
                .workspaceId(workspaceId)
                .stepRegistry(buildStepRegistry())
                .pluginRegistry(pluginRegistry)
                .build();

        // 设置 DAG 结构
        // ResumeStep -> DecomposeStep -> AssignStep -> ExecuteStep -> ClaimStep -> ResultStep
        //                                                              ↓
        //                                                         NotifyStep -> ObserveStep
        //                                                              ↓
        //                                                         CompleteStep

        task.addStep(resumeStep, Set.of());  // ResumeStep 无依赖
        task.addStep(decomposeStep, Set.of("resume"));  // 依赖 resume
        task.addStep(assignStep, Set.of("decompose"));  // 依赖 decompose
        task.addStep(executeStep, Set.of("assign"));  // 依赖 assign
        task.addStep(claimStep, Set.of("execute"));  // 依赖 execute
        task.addStep(resultStep, Set.of("claim"));  // 依赖 claim
        task.addStep(notifyStep, Set.of("result"));  // 依赖 result
        task.addStep(observerEvalStep, Set.of("notify"));  // 依赖 notify
        task.addStep(completeStep, Set.of("observe"));  // 依赖 observe

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
        registry.register("claim", claimStep);
        registry.register("result", resultStep);
        registry.register("notify", notifyStep);
        registry.register("observe", observerEvalStep);
        registry.register("complete", completeStep);
        return registry;
    }

    /**
     * 构建执行上下文
     */
    private TaskContext buildContext(Task task, String workspaceId) {
        List<WorkspaceMember> members = memberService.listMembers(workspaceId);

        // 创建 ChatRoom 实例（按 workspace，不同于全局单例）
        // 使用 Supplier 延迟获取 this 来避免循环依赖
        ChatRoom chatRoom = new ChatRoom(workspaceFacadeService, storeFactory, () -> this);

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
            log.warn("[TaskExecutionService] Task {} completed with failures", task.getId());
        } else {
            log.info("[TaskExecutionService] Task {} completed successfully", task.getId());
        }
    }
}