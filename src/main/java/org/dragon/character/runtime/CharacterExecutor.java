package org.dragon.character.runtime;

import java.util.UUID;

import org.dragon.agent.orchestration.OrchestrationService;
import org.dragon.agent.react.ReActContext;
import org.dragon.agent.react.ReActResult;
import org.dragon.agent.workflow.Workflow;
import org.dragon.agent.workflow.WorkflowResult;
import org.dragon.character.config.CharacterExecutorConfig;
import org.dragon.character.mind.DefaultMind;
import org.dragon.character.mind.Mind;
import org.dragon.character.profile.CharacterProfile;
import org.dragon.config.PromptKeys;
import org.dragon.skill.SkillAccess;
import org.dragon.skill.SkillAccessImpl;
import org.dragon.task.Task;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Character 执行器
 * 负责 Character 的实际执行逻辑
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Builder
@RequiredArgsConstructor
public class CharacterExecutor {

    private final CharacterProfile profile;
    private final CharacterRuntime runtime;
    private final CharacterExecutorConfig config;

    /**
     * 执行主入口
     */
    public String run(String userInput) {
        if (runtime.getOrchestrationService() == null) {
            throw new IllegalStateException("OrchestrationService not initialized");
        }

        OrchestrationService.OrchestrationRequest request = new OrchestrationService.OrchestrationRequest(
                profile.getId(), userInput, null, null);
        OrchestrationService.OrchestrationResult orchestrationResult = runtime.getOrchestrationService().orchestrate(request);

        if (!orchestrationResult.isSuccess()) {
            return "Orchestration failed";
        }

        OrchestrationService.Mode mode = orchestrationResult.getMode();
        if (mode == OrchestrationService.Mode.WORKFLOW) {
            String workflowId = orchestrationResult.getWorkflowId();
            WorkflowResult result = runWorkflow(workflowId);
            return result.getErrorMessage() != null ? result.getErrorMessage() : "Workflow completed";
        } else {
            ReActResult result = runReAct(userInput, false, null, null);
            return result.getResponse();
        }
    }

    /**
     * 执行主入口（带 Task 透传）
     */
    public String run(String userInput, Task task) {
        if (runtime.getOrchestrationService() == null) {
            throw new IllegalStateException("OrchestrationService not initialized");
        }

        OrchestrationService.OrchestrationRequest request = new OrchestrationService.OrchestrationRequest(
                profile.getId(), userInput, null, null);
        OrchestrationService.OrchestrationResult orchestrationResult = runtime.getOrchestrationService().orchestrate(request);

        if (!orchestrationResult.isSuccess()) {
            return "Orchestration failed";
        }

        OrchestrationService.Mode mode = orchestrationResult.getMode();
        if (mode == OrchestrationService.Mode.WORKFLOW) {
            String workflowId = orchestrationResult.getWorkflowId();
            WorkflowResult result = runWorkflow(workflowId);
            return result.getErrorMessage() != null ? result.getErrorMessage() : "Workflow completed";
        } else {
            ReActResult result = runReAct(userInput, false, task, null);
            return result.getResponse();
        }
    }

    /**
     * 使用 ReAct 模式执行
     */
    public ReActResult runReAct(String userInput) {
        return runReAct(userInput, false, null, null);
    }

    /**
     * 执行 ReAct（带流式、Task 和桥接上下文支持）
     */
    public ReActResult runReAct(String userInput, boolean streaming, Task task,
                                org.dragon.workspace.service.task.execution.TaskBridgeContext bridgeContext) {
        if (runtime.getReActExecutor() == null) {
            throw new IllegalStateException("ReActExecutor not initialized");
        }

        String defaultModelId = resolveDefaultModelId();
        int maxIterations = resolveMaxIterations();

        String systemPrompt = resolveSystemPrompt();

        ReActContext.ReActContextBuilder contextBuilder = ReActContext.builder()
                .executionId(UUID.randomUUID().toString())
                .characterId(profile.getId())
                .defaultModelId(defaultModelId)
                .currentModelId(defaultModelId)
                .userInput(userInput)
                .systemPrompt(systemPrompt)
                .maxIterations(maxIterations)
                .streamingEnabled(streaming)
                .task(task)
                .allowedTools(profile.getAllowedTools());

        if (bridgeContext != null) {
            boolean collaborationEnabled = bridgeContext.isCollaborationJudgementEnabled();
            if (task != null && task.getMetadata() != null) {
                Object allowJudgement = task.getMetadata().get("allowCollaborationJudgement");
                if (allowJudgement != null) {
                    collaborationEnabled = Boolean.TRUE.equals(allowJudgement);
                }
            }

            if (collaborationEnabled && bridgeContext.getCollaborationSessionId() != null) {
                String collaborationPrompt = "";
                if (runtime.getPromptManager() != null) {
                    String workspace = resolveWorkspace();
                    collaborationPrompt = runtime.getPromptManager().getPrompt(workspace, profile.getId(), PromptKeys.CHARACTER_COLLABORATION_DECISION);
                }
                contextBuilder
                        .collaborationJudgementEnabled(true)
                        .collaborationDecisionPrompt(collaborationPrompt)
                        .collaborationSessionId(bridgeContext.getCollaborationSessionId())
                        .participantStates(bridgeContext.getParticipantStates())
                        .blockedParticipants(bridgeContext.getBlockedParticipants())
                        .sessionStatus(bridgeContext.getSessionStatus())
                        .latestSessionMessages(bridgeContext.getLatestSessionMessages())
                        .peerCharacterIds(bridgeContext.getPeerCharacterIds())
                        .dependencyTaskIds(bridgeContext.getDependencyTaskIds());
            }
        }

        if (bridgeContext != null && bridgeContext.getMaterialContext() != null) {
            contextBuilder.materialContext(bridgeContext.getMaterialContext());
        }

        ReActContext context = contextBuilder.build();
        return runtime.getReActExecutor().execute(context);
    }

    /**
     * 使用 Workflow 模式执行
     */
    public WorkflowResult runWorkflow(String workflowId) {
        if (runtime.getWorkflowExecutor() == null) {
            throw new IllegalStateException("WorkflowExecutor not initialized");
        }
        if (runtime.getWorkflowStore() == null) {
            throw new IllegalStateException("WorkflowStore not initialized");
        }

        Workflow workflow = runtime.getWorkflowStore().findById(workflowId).orElse(null);
        if (workflow == null) {
            return WorkflowResult.builder()
                    .workflowId(workflowId)
                    .status(org.dragon.agent.workflow.WorkflowState.State.FAILED)
                    .errorMessage("Workflow not found: " + workflowId)
                    .build();
        }

        java.util.Map<String, Object> input = new java.util.HashMap<>();
        input.put("characterId", profile.getId());
        String workspace = resolveWorkspace();
        if (workspace != null) {
            input.put("workspaceId", workspace);
        }

        return runtime.getWorkflowExecutor().execute(workflow, input);
    }

    private String resolveDefaultModelId() {
        String modelId = config != null && config.getDefaultModelId() != null
                ? config.getDefaultModelId()
                : null;
        if (modelId == null && runtime.getModelRegistry() != null) {
            modelId = runtime.getModelRegistry().getDefault()
                    .map(m -> m.getId())
                    .orElse(null);
        }
        return modelId;
    }

    private int resolveMaxIterations() {
        if (config != null && config.getReActConfig() != null) {
            return config.getReActConfig().getMaxIterations();
        }
        return 10;
    }

    private String resolveSystemPrompt() {
        String prompt = "";
        String workspace = resolveWorkspace();
        if (runtime.getPromptManager() != null) {
            prompt = runtime.getPromptManager().getPrompt(workspace, profile.getId(), PromptKeys.CHARACTER_SYSTEM);
        }
        if (prompt == null || prompt.isEmpty()) {
            Mind currentMind = getMind();
            if (currentMind != null && currentMind.getPersonality() != null) {
                prompt = currentMind.getPersonality().toPrompt();
            }
        }
        // 增加skill的prompt
        prompt += runtime.getSkillRegistry().buildSystemPromptFragment(profile.getId(), Long.parseLong(workspace));
        return prompt != null ? prompt : "";
    }

    private String resolveWorkspace() {
        if (profile.getWorkspaceIds() != null && !profile.getWorkspaceIds().isEmpty()) {
            return profile.getWorkspaceIds().get(0);
        }
        return null;
    }

    private Mind getMind() {
        if (runtime.getMind() != null) {
            return runtime.getMind();
        }
        if (profile.getMind() != null) {
            return profile.getMind();
        }
        if (profile.getMindConfig() != null) {
            return initMind();
        }
        return null;
    }

    private Mind initMind() {
        if (profile.getMindConfig() == null) {
            return null;
        }
        // 创建 SkillAccess（如果 skillRegistry 可用）
        SkillAccess skillAccess = null;
        if (runtime.getSkillRegistry() != null) {
            skillAccess = new SkillAccessImpl(
                    profile.getId(),
                    runtime.getWorkspaceId(),
                    runtime.getSkillRegistry());
        }
        DefaultMind defaultMind = new DefaultMind(
                profile.getId(), null, null, skillAccess);
        String personalityPath = profile.getMindConfig().getPersonalityDescriptorPath();
        if (personalityPath != null && !personalityPath.isEmpty()) {
            defaultMind.loadPersonality(personalityPath);
        }
        return defaultMind;
    }
}
