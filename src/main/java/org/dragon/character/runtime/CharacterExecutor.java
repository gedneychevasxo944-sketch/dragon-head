package org.dragon.character.runtime;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dragon.agent.orchestration.OrchestrationService;
import org.dragon.agent.react.ReActContext;
import org.dragon.agent.react.ReActResult;
import org.dragon.agent.react.context.PromptMaterialContext;
import org.dragon.agent.react.context.PromptMaterialConfig;
import org.dragon.agent.react.context.PromptMaterialContextBuilder;
import org.dragon.agent.workflow.Workflow;
import org.dragon.agent.workflow.WorkflowResult;
import org.dragon.character.config.CharacterExecutorConfig;
import org.dragon.character.mind.DefaultMind;
import org.dragon.character.mind.Mind;
import org.dragon.character.mind.PersonalityDescriptor;
import org.dragon.character.profile.CharacterProfile;
import org.dragon.config.PromptKeys;
import org.dragon.config.config.PromptMaterialConfigProperties;
import org.dragon.skill.runtime.SkillDefinition;
import org.dragon.skill.runtime.SkillDirectoryBuilder;
import org.dragon.skill.runtime.SkillRegistry;
import org.dragon.task.Task;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.util.SpringUtils;

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
     * Prompt 物料上下文构建器（可选）
     */
    @Builder.Default
    private final PromptMaterialContextBuilder promptMaterialContextBuilder = SpringUtils.getBean(PromptMaterialContextBuilder.class);

    /**
     * Prompt 物料配置属性（可选）
     */
    @Builder.Default
    private final PromptMaterialConfigProperties promptMaterialConfigProperties = null;

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
                                org.dragon.workspace.task.TaskBridgeContext bridgeContext) {
        if (runtime.getReActExecutor() == null) {
            throw new IllegalStateException("ReActExecutor not initialized");
        }

        String defaultModelId = resolveDefaultModelId();
        int maxIterations = resolveMaxIterations();
        String workspace = resolveWorkspace();

        // 构建 PromptMaterialContext
        PromptMaterialContext promptMaterialContext = buildPromptMaterialContext(workspace, task);

        // 使用 PromptMaterialContext 构建 system prompt
        String systemPrompt = resolveSystemPrompt(promptMaterialContext);

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
                .allowedTools(profile.getAllowedTools())
                .activeSkills(resolveActiveSkills(workspace))
                .promptMaterialContext(promptMaterialContext);

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
                if (runtime.getConfigApplication() != null) {
                    collaborationPrompt = runtime.getConfigApplication().getPrompt(workspace, profile.getId(), PromptKeys.CHARACTER_COLLABORATION_DECISION);
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

    /**
     * 构建 PromptMaterialContext
     */
    private PromptMaterialContext buildPromptMaterialContext(String workspace, Task task) {
        if (promptMaterialContextBuilder == null) {
            return null;
        }
        PromptMaterialConfig materialConfig = resolvePromptMaterialConfig();
        return promptMaterialContextBuilder.buildForReAct(
                workspace,
                profile.getId(),
                task,
                getMind(),
                resolveActiveSkills(workspace),
                config != null ? config.getReActConfig() : null,
                materialConfig
        );
    }

    /**
     * 获取 PromptMaterialConfig
     */
    private PromptMaterialConfig resolvePromptMaterialConfig() {
        if (promptMaterialConfigProperties != null) {
            return promptMaterialConfigProperties.toConfig();
        }
        return PromptMaterialConfig.defaultReAct();
    }

    /**
     * 解析 System Prompt
     */
    private String resolveSystemPrompt(PromptMaterialContext promptMaterialContext) {
        String workspace = resolveWorkspace();

        // 1. 优先从 ConfigApplication 获取 prompt（4级层次）
        String prompt = "";
        if (runtime.getConfigApplication() != null) {
            prompt = runtime.getConfigApplication().getPrompt(workspace, profile.getId(), PromptKeys.CHARACTER_SYSTEM);
        }

        // 2. 如果未获取到，使用 Mind.personality.toPrompt()
        if (StringUtils.isBlank(prompt)) {
            Mind currentMind = getMind();
            if (currentMind != null && currentMind.getPersonality() != null) {
                prompt = currentMind.getPersonality().toPrompt();
            }
        }

        // 3. 追加 Character 基本信息（profile.description）
        prompt = appendCharacterBasicInfo(prompt);

        // 4. 追加 Character 特征（traits）
        prompt = appendCharacterTraitsInfo(prompt);

        // 5. 增加 skill 的 prompt
        prompt += SkillDirectoryBuilder.buildDirectoryPrompt(runtime.getSkillRegistry().getSkills(profile.getId(), workspace));

        // 6. 增加 Workspace Personality（如果有）
        if (promptMaterialContext != null && promptMaterialContext.getWorkspacePersonality() != null) {
            prompt = appendWorkspacePersonalityPrompt(prompt, promptMaterialContext.getWorkspacePersonality());
        }

        return prompt != null ? prompt : "";
    }

    /**
     * 追加 Character 基本信息到 System Prompt
     * 包含 profile.description，确保 LLM 能获取到角色描述
     */
    private String appendCharacterBasicInfo(String currentPrompt) {
        StringBuilder sb = new StringBuilder();
        if (currentPrompt != null && !currentPrompt.isEmpty()) {
            sb.append(currentPrompt).append("\n\n");
        }

        boolean hasInfo = false;

        // 追加 Character 描述
        if (profile.getDescription() != null && !profile.getDescription().isBlank()) {
            sb.append("## Character 描述\n");
            sb.append(profile.getDescription()).append("\n");
            hasInfo = true;
        }

        // 追加 Character 名称（如果与描述不同）
        if (profile.getName() != null && !profile.getName().isBlank()) {
            // 只有当描述中没有包含名称时才追加
            if (currentPrompt == null || !currentPrompt.contains(profile.getName())) {
                sb.append("## Character 名称\n");
                sb.append(profile.getName()).append("\n");
                hasInfo = true;
            }
        }

        return hasInfo ? sb.toString() : (currentPrompt != null ? currentPrompt : "");
    }

    /**
     * 追加 Character 特征（Traits）到 System Prompt
     * 通过 AssetAssociationService 获取关联的 traitIds，再通过 TraitResolutionService 解析具体内容
     */
    private String appendCharacterTraitsInfo(String currentPrompt) {
        if (runtime.getAssetAssociationService() == null || runtime.getTraitResolutionService() == null) {
            return currentPrompt;
        }

        List<String> traitIds = runtime.getAssetAssociationService().getTraitsForCharacter(profile.getId());
        if (traitIds == null || traitIds.isEmpty()) {
            return currentPrompt;
        }

        List<PersonalityDescriptor.TraitContent> traits = runtime.getTraitResolutionService().resolveTraits(traitIds);
        if (traits == null || traits.isEmpty()) {
            return currentPrompt;
        }

        StringBuilder sb = new StringBuilder();
        if (currentPrompt != null && !currentPrompt.isEmpty()) {
            sb.append(currentPrompt).append("\n\n");
        }

        sb.append("## Character 品质特征\n");
        for (PersonalityDescriptor.TraitContent trait : traits) {
            if (trait.getName() != null && !trait.getName().isBlank()) {
                sb.append("- ").append(trait.getName());
                if (trait.getCategory() != null && !trait.getCategory().isBlank()) {
                    sb.append(" (").append(trait.getCategory()).append(")");
                }
                sb.append(": ").append(trait.getContent() != null ? trait.getContent() : "").append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 追加 Workspace Personality 到 System Prompt
     */
    private String appendWorkspacePersonalityPrompt(String currentPrompt, org.dragon.workspace.WorkspacePersonality personality) {
        if (personality == null) {
            return currentPrompt;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(currentPrompt).append("\n\n");
        sb.append("## Workspace 工作空间人格\n");
        if (personality.getWorkingStyle() != null) {
            sb.append("- 工作风格: ").append(personality.getWorkingStyle()).append("\n");
        }
        if (personality.getDecisionPattern() != null) {
            sb.append("- 决策模式: ").append(personality.getDecisionPattern()).append("\n");
        }
        if (personality.getRiskTolerance() != null) {
            sb.append("- 风险容忍度: ").append(personality.getRiskTolerance()).append("\n");
        }
        if (personality.getCoreValues() != null && !personality.getCoreValues().isBlank()) {
            sb.append("- 核心价值观: ").append(personality.getCoreValues()).append("\n");
        }
        if (personality.getBehaviorGuidelines() != null && !personality.getBehaviorGuidelines().isBlank()) {
            sb.append("- 行为准则: ").append(personality.getBehaviorGuidelines()).append("\n");
        }
        if (personality.getCollaborationPreference() != null && !personality.getCollaborationPreference().isBlank()) {
            sb.append("- 协作偏好: ").append(personality.getCollaborationPreference()).append("\n");
        }
        if (personality.getPersonalityDescription() != null && !personality.getPersonalityDescription().isBlank()) {
            sb.append("- 组织描述:\n").append(personality.getPersonalityDescription()).append("\n");
        }
        return sb.toString();
    }

    private String resolveWorkspace() {
        // 从运行时获取 workspaceId（由 CharacterRuntimeBinder.bind 注入）
        if (runtime != null && runtime.getWorkspaceId() != null) {
            return String.valueOf(runtime.getWorkspaceId());
        }
        return null;
    }

    /**
     * 解析当前激活的 Skills 列表。
     *
     * @param workspaceId 工作空间 ID（可能为 null）
     * @return 激活的 SkillRuntimeEntry 列表
     */
    private List<SkillDefinition> resolveActiveSkills(String workspaceId) {
        if (workspaceId == null || runtime.getSkillRegistry() == null) {
            return Collections.emptyList();
        }
        SkillRegistry registry = runtime.getSkillRegistry();
        return new java.util.ArrayList<>(registry.getSkills(profile.getId(), workspaceId));
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
        DefaultMind defaultMind = new DefaultMind(
                profile.getId(), null, null, runtime.getTraitResolutionService());
         String personalityPath = profile.getMindConfig().getPersonalityDescriptorPath();
        if (personalityPath != null && !personalityPath.isEmpty()) {
            defaultMind.loadPersonality(personalityPath);
        }
        // 如果有 trait IDs，从数据库加载 Trait 内容
        if (runtime.getAssetAssociationService() != null) {
            List<String> traitIds = runtime.getAssetAssociationService().getTraitsForCharacter(profile.getId());
            if (traitIds != null && !traitIds.isEmpty()) {
                defaultMind.loadPersonalityFromTraitIds(traitIds);
            }
        }
        return defaultMind;
    }
}
