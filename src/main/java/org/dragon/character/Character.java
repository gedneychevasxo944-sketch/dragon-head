package org.dragon.character;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dragon.agent.model.ModelRegistry;
import org.dragon.agent.react.ReActContext;
import org.dragon.agent.react.ReActResult;
import org.dragon.agent.react.ThoughtPromptAssembler;
import org.dragon.agent.react.context.PromptMaterialConfig;
import org.dragon.agent.react.context.PromptMaterialContext;
import org.dragon.agent.react.context.PromptMaterialContextBuilder;
import org.dragon.character.config.CharacterExecutorConfig;
import org.dragon.character.mind.DefaultMind;
import org.dragon.character.mind.Mind;
import org.dragon.character.profile.CharacterProfile;
import org.dragon.character.runtime.CharacterRuntime;
import org.dragon.config.PromptKeys;
import org.dragon.config.config.PromptMaterialConfigProperties;
import org.dragon.config.service.ConfigApplication;
import org.dragon.skill.runtime.SkillDefinition;
import org.dragon.skill.runtime.SkillDirectoryBuilder;
import org.dragon.skill.runtime.SkillRegistry;
import org.dragon.step.character.*;
import org.dragon.task.Task;
import org.dragon.tools.ToolRegistry;
import org.dragon.util.SpringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Character 聚合根
 * AI 数字员工实体，作为对外Facade接口
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Data
public class Character {

    /**
     * 数据体
     */
    private CharacterProfile profile;

    /**
     * 执行引擎配置
     */
    private CharacterExecutorConfig executorConfig;

    /**
     * 运行时依赖
     */
    private CharacterRuntime runtime;

    /**
     * Prompt 物料上下文构建器（惰性）
     */
    @JsonIgnore
    private transient PromptMaterialContextBuilder promptMaterialContextBuilder;

    /**
     * Prompt 物料配置属性（可选）
     */
    @JsonIgnore
    private transient PromptMaterialConfigProperties promptMaterialConfigProperties;

    public Character() {
        this.profile = new CharacterProfile();
        this.profile.setExtensions(new java.util.HashMap<>());
        this.profile.setAllowedTools(new HashSet<>());
        this.profile.setStatus(CharacterProfile.Status.UNLOADED);
    }

    // ==================== 便捷属性代理 ====================

    public String getId() {
        return profile.getId();
    }

    public void setId(String id) {
        profile.setId(id);
    }

    public String getName() {
        return profile.getName();
    }

    public void setName(String name) {
        profile.setName(name);
    }

    public String getDescription() {
        return profile.getDescription();
    }

    public void setDescription(String description) {
        profile.setDescription(description);
    }

    public String getAvatar() {
        return profile.getAvatar();
    }

    public void setAvatar(String avatar) {
        profile.setAvatar(avatar);
    }

    public String getSource() {
        return profile.getSource();
    }

    public void setSource(String source) {
        profile.setSource(source);
    }

    public CharacterProfile.Status getStatus() {
        return profile.getStatus();
    }

    public void setStatus(CharacterProfile.Status status) {
        profile.setStatus(status);
    }

    public Set<String> getAllowedTools() {
        return profile.getAllowedTools();
    }

    public void setAllowedTools(Set<String> allowedTools) {
        profile.setAllowedTools(allowedTools);
    }

    public Map<String, Object> getExtensions() {
        return profile.getExtensions();
    }

    public void setExtensions(Map<String, Object> extensions) {
        profile.setExtensions(extensions);
    }

    public String getPromptTemplate() {
        return profile.getPromptTemplate();
    }

    public void setPromptTemplate(String promptTemplate) {
        profile.setPromptTemplate(promptTemplate);
    }

    public List<String> getDefaultTools() {
        return profile.getDefaultTools();
    }

    public void setDefaultTools(List<String> defaultTools) {
        profile.setDefaultTools(defaultTools);
    }

    public Boolean getIsRunning() {
        return profile.getIsRunning();
    }

    public void setIsRunning(Boolean isRunning) {
        profile.setIsRunning(isRunning);
    }

    public Integer getDeployedCount() {
        return profile.getDeployedCount();
    }

    public void setDeployedCount(Integer deployedCount) {
        profile.setDeployedCount(deployedCount);
    }

    public LocalDateTime getCreatedAt() {
        return profile.getCreatedAt();
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        profile.setCreatedAt(createdAt);
    }

    public LocalDateTime getUpdatedAt() {
        return profile.getUpdatedAt();
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        profile.setUpdatedAt(updatedAt);
    }

    public CharacterProfile.MindConfig getMindConfig() {
        return profile.getMindConfig();
    }

    public void setMindConfig(CharacterProfile.MindConfig mindConfig) {
        profile.setMindConfig(mindConfig);
    }

    public String getMbti() {
        return profile.getMbti();
    }

    public void setMbti(String mbti) {
        profile.setMbti(mbti);
    }

    @JsonIgnore
    public Mind getMind() {
        if (runtime != null && runtime.getMind() != null) {
            return runtime.getMind();
        }
        if (profile.getMind() != null) {
            return profile.getMind();
        }
        return initMind();
    }

    public void setMind(Mind mind) {
        profile.setMind(mind);
    }

    @JsonIgnore
    public CharacterExecutorConfig getAgentEngineConfig() {
        return executorConfig;
    }

    public void setAgentEngineConfig(CharacterExecutorConfig agentEngineConfig) {
        this.executorConfig = agentEngineConfig;
    }

    // ==================== 执行入口 ====================

    /**
     * 执行主入口
     */
    public String run(String userInput) {
        ReActResult result = runReAct(userInput);
        return result != null ? result.getResponse() : "Execution failed";
    }

    /**
     * 执行主入口（带 Task 透传）
     */
    public String run(String userInput, Task task) {
        ReActResult result = runReAct(userInput, false, task);
        return result != null ? result.getResponse() : "Execution failed";
    }

    /**
     * 使用 ReAct 模式执行
     */
    public ReActResult runReAct(String userInput) {
        return runReAct(userInput, false, null);
    }

    /**
     * 执行 ReAct 循环（带流式、Task 支持）
     */
    public ReActResult runReAct(String userInput, boolean streaming, Task task) {
        String defaultModelId = resolveDefaultModelId();
        int maxIterations = resolveMaxIterations();
        String workspace = resolveWorkspace();

        PromptMaterialContext promptMaterialContext = buildPromptMaterialContext(workspace, task);
        String systemPrompt = resolveSystemPrompt(promptMaterialContext);

        ReActContext context = ReActContext.builder()
                .executionId(java.util.UUID.randomUUID().toString())
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
                .promptMaterialContext(promptMaterialContext)
                .build();

        return executeReActLoop(context);
    }

    /**
     * 执行 ReAct 循环
     * 循环顺序：MemoryInject → SkillInject → BuildPrompt → Think → Act → Observe
     */
    private ReActResult executeReActLoop(ReActContext context) {
        int iteration = 0;
        int maxIterations = context.getMaxIterations();

        MemoryInjectStep memoryInjectStep = createMemoryInjectStep();
        SkillInjectStep skillInjectStep = createSkillInjectStep();
        BuildPromptStep buildPromptStep = createBuildPromptStep();
        ThinkStep thinkStep = createThinkStep();
        ActStep actStep = createActStep();
        EvaluationStep evaluationStep = createEvaluationStep();

        log.info("[Character] Starting ReAct loop for character {} (maxIterations={})",
                profile.getId(), maxIterations);

        while (iteration < maxIterations && !context.isComplete()) {
            iteration++;
            context.setCurrentIteration(iteration);
            log.info("[Character] [{}] Iteration started", iteration);

            try {
                if (memoryInjectStep.isEnabled(context)) {
                    memoryInjectStep.execute(context);
                }

                if (skillInjectStep.isEnabled(context)) {
                    skillInjectStep.execute(context);
                }

                if (buildPromptStep.isEnabled(context)) {
                    buildPromptStep.execute(context);
                }

                if (thinkStep.isEnabled(context)) {
                    thinkStep.execute(context);
                }

                if (actStep.isEnabled(context)) {
                    actStep.execute(context);
                }

                if (evaluationStep.isEnabled(context)) {
                    evaluationStep.execute(context);
                }

            } catch (Exception e) {
                log.error("[Character] [{}] Iteration failed: {}", iteration, e.getMessage(), e);
                context.setErrorMessage(e.getMessage());
                context.complete("Error: " + e.getMessage());
                break;
            }
        }

        if (iteration >= maxIterations && !context.isComplete()) {
            log.warn("[Character] Max iterations {} reached, stopping", maxIterations);
            context.setErrorMessage("Max iterations reached");
            context.complete("Max iterations reached");
        }

        return buildResult(context, iteration);
    }

    // ==================== Step 创建方法 ====================

    private MemoryInjectStep createMemoryInjectStep() {
        return new MemoryInjectStep();
    }

    private SkillInjectStep createSkillInjectStep() {
        if (runtime != null && runtime.getSkillRegistry() != null) {
            return new SkillInjectStep(runtime.getSkillRegistry());
        }
        return new SkillInjectStep();
    }

    private BuildPromptStep createBuildPromptStep() {
        ThoughtPromptAssembler assembler = SpringUtils.getBean(ThoughtPromptAssembler.class);
        ConfigApplication configApp = runtime != null ? runtime.getConfigApplication() : null;
        return new BuildPromptStep(configApp, null, null, assembler);
    }

    private ThinkStep createThinkStep() {
        return new ThinkStep(
                runtime != null ? runtime.getCallerSelector() : null,
                runtime != null ? runtime.getModelRegistry() : null,
                runtime != null ? runtime.getToolRegistry() : null
        );
    }

    private ActStep createActStep() {
        return new ActStep();
    }

    private EvaluationStep createEvaluationStep() {
        return new EvaluationStep();
    }

    // ==================== 工具方法 ====================

    private ReActResult buildResult(ReActContext context, int iterations) {
        List<String> obs = context.getObservations();
        String latestObs = (obs != null && !obs.isEmpty()) ? obs.get(obs.size() - 1) : context.getErrorMessage();
        return ReActResult.builder()
                .executionId(context.getExecutionId())
                .success(context.isComplete() && context.getErrorMessage() == null)
                .response(context.isComplete() ? latestObs : context.getErrorMessage())
                .iterations(iterations)
                .thoughts(new ArrayList<>(context.getThoughts()))
                .actions(new ArrayList<>(context.getActions()))
                .observations(new ArrayList<>(context.getObservations()))
                .errorMessage(context.getErrorMessage())
                .build();
    }

    private String resolveDefaultModelId() {
        String modelId = executorConfig != null && executorConfig.getDefaultModelId() != null
                ? executorConfig.getDefaultModelId()
                : null;
        if (modelId == null && runtime != null && runtime.getModelRegistry() != null) {
            modelId = runtime.getModelRegistry().getDefault()
                    .map(m -> m.getId())
                    .orElse(null);
        }
        return modelId;
    }

    private int resolveMaxIterations() {
        if (executorConfig != null && executorConfig.getReActConfig() != null) {
            return executorConfig.getReActConfig().getMaxIterations();
        }
        return 10;
    }

    private PromptMaterialContext buildPromptMaterialContext(String workspace, Task task) {
        if (getPromptMaterialContextBuilder() == null) {
            return null;
        }
        PromptMaterialConfig materialConfig = resolvePromptMaterialConfig();
        return getPromptMaterialContextBuilder().buildForReAct(
                workspace,
                profile.getId(),
                task,
                getMind(),
                resolveActiveSkills(workspace),
                executorConfig != null ? executorConfig.getReActConfig() : null,
                materialConfig
        );
    }

    private PromptMaterialContextBuilder getPromptMaterialContextBuilder() {
        if (promptMaterialContextBuilder == null) {
            promptMaterialContextBuilder = SpringUtils.getBean(PromptMaterialContextBuilder.class);
        }
        return promptMaterialContextBuilder;
    }

    private PromptMaterialConfig resolvePromptMaterialConfig() {
        if (promptMaterialConfigProperties != null) {
            return promptMaterialConfigProperties.toConfig();
        }
        return PromptMaterialConfig.defaultReAct();
    }

    private String resolveSystemPrompt(PromptMaterialContext promptMaterialContext) {
        String workspace = resolveWorkspace();

        String prompt = "";
        if (runtime != null && runtime.getConfigApplication() != null) {
            prompt = runtime.getConfigApplication().getPrompt(workspace, profile.getId(), PromptKeys.CHARACTER_SYSTEM);
        }

        prompt = appendCharacterMind(prompt);

        if (runtime != null && runtime.getSkillRegistry() != null) {
            prompt += SkillDirectoryBuilder.buildDirectoryPrompt(runtime.getSkillRegistry().getSkills(profile.getId(), workspace));
        }

        if (promptMaterialContext != null && promptMaterialContext.getWorkspacePersonality() != null) {
            prompt = appendWorkspacePersonalityPrompt(prompt, promptMaterialContext.getWorkspacePersonality());
        }

        prompt = appendMbtiPersonalityDescription(prompt);

        return prompt != null ? prompt : "";
    }

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

    private String appendCharacterMind(String prompt) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(prompt)) {
            sb.append(prompt).append("\n\n");
        }
        Mind currentMind = getMind();
        if (currentMind != null && currentMind.getPersonality() != null) {
            prompt = currentMind.getPersonality().toPrompt();
            sb.append(prompt);
        }
        return sb.toString();
    }

    private String appendMbtiPersonalityDescription(String currentPrompt) {
        String mbti = profile.getMbti();
        if (mbti == null || mbti.isBlank()) {
            return currentPrompt;
        }

        if (runtime == null || runtime.getConfigApplication() == null) {
            return currentPrompt;
        }

        String mbtiPrompt = runtime.getConfigApplication().getGlobalPrompt(
                PromptKeys.MBTI_PREFIX + mbti.toUpperCase(), null);
        if (mbtiPrompt == null || mbtiPrompt.isBlank()) {
            return currentPrompt;
        }

        StringBuilder sb = new StringBuilder();
        if (currentPrompt != null && !currentPrompt.isEmpty()) {
            sb.append(currentPrompt).append("\n\n");
        }
        sb.append("## MBTI 人格特征\n");
        sb.append(mbtiPrompt);

        return sb.toString();
    }

    private String resolveWorkspace() {
        if (runtime != null && runtime.getWorkspaceId() != null) {
            return String.valueOf(runtime.getWorkspaceId());
        }
        return null;
    }

    private List<SkillDefinition> resolveActiveSkills(String workspaceId) {
        if (workspaceId == null || runtime == null || runtime.getSkillRegistry() == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(runtime.getSkillRegistry().getSkills(profile.getId(), workspaceId));
    }

    private Mind initMind() {
        DefaultMind defaultMind = new DefaultMind(
                profile.getId(), null, runtime != null ? runtime.getTraitResolutionService() : null);

        if (profile.getMindConfig() != null) {
            String personalityPath = profile.getMindConfig().getPersonalityDescriptorPath();
            if (personalityPath != null && !personalityPath.isEmpty()) {
                defaultMind.loadPersonality(personalityPath);
            }
        }

        if (runtime != null && runtime.getAssetAssociationService() != null) {
            List<String> traitIds = runtime.getAssetAssociationService().getTraitsForCharacter(profile.getId());
            if (traitIds != null && !traitIds.isEmpty()) {
                defaultMind.loadPersonalityFromTraitIds(traitIds);
            }
        }

        defaultMind.getPersonality().setName(profile.getName());
        defaultMind.getPersonality().setDescription(profile.getDescription());

        return defaultMind;
    }

    // ==================== 兼容旧接口 ====================

    @JsonIgnore
    public ConfigApplication getConfigApplication() {
        return runtime != null ? runtime.getConfigApplication() : null;
    }

    @JsonIgnore
    public ModelRegistry getModelRegistry() {
        return runtime != null ? runtime.getModelRegistry() : null;
    }
}