package org.dragon.character;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dragon.agent.react.ReActResult;
import org.dragon.agent.workflow.WorkflowResult;
import org.dragon.character.config.CharacterExecutorConfig;
import org.dragon.character.profile.CharacterProfile;
import org.dragon.character.runtime.CharacterExecutor;
import org.dragon.character.runtime.CharacterRuntime;
import org.dragon.task.Task;

import lombok.Data;

/**
 * Character 聚合根
 * AI 数字员工实体，作为对外Facade接口
 *
 * @author wyj
 * @version 1.0
 */
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
     * 执行器（惰性初始化）
     */
    private transient CharacterExecutor executor;

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

    public List<String> getWorkspaceIds() {
        return profile.getWorkspaceIds();
    }

    public void setWorkspaceIds(List<String> workspaceIds) {
        profile.setWorkspaceIds(workspaceIds);
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

    public Integer getVersion() {
        return profile.getVersion();
    }

    public void setVersion(Integer version) {
        profile.setVersion(version);
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

    public List<String> getTraits() {
        return profile.getTraits();
    }

    public void setTraits(List<String> traits) {
        profile.setTraits(traits);
    }

    public Map<String, Object> getTraitConfigs() {
        return profile.getTraitConfigs();
    }

    public void setTraitConfigs(Map<String, Object> traitConfigs) {
        profile.setTraitConfigs(traitConfigs);
    }

    public List<Map<String, Object>> getSkills() {
        return profile.getSkills();
    }

    public void setSkills(List<Map<String, Object>> skills) {
        profile.setSkills(skills);
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

    public org.dragon.character.mind.Mind getMind() {
        return profile.getMind();
    }

    public void setMind(org.dragon.character.mind.Mind mind) {
        profile.setMind(mind);
    }

    public CharacterExecutorConfig getAgentEngineConfig() {
        return executorConfig;
    }

    public void setAgentEngineConfig(CharacterExecutorConfig agentEngineConfig) {
        this.executorConfig = agentEngineConfig;
    }

    // ==================== 执行代理 ====================

    public String run(String userInput) {
        return getExecutor().run(userInput);
    }

    public String run(String userInput, Task task) {
        return getExecutor().run(userInput, task);
    }

    public ReActResult runReAct(String userInput) {
        return getExecutor().runReAct(userInput);
    }

    public ReActResult runReAct(String userInput, boolean streaming, Task task,
                                org.dragon.workspace.service.task.execution.TaskBridgeContext bridgeContext) {
        return getExecutor().runReAct(userInput, streaming, task, bridgeContext);
    }

    public ReActResult runReAct(String userInput, boolean streaming, Task task) {
        return getExecutor().runReAct(userInput, streaming, task, null);
    }

    public WorkflowResult runWorkflow(String workflowId) {
        return getExecutor().runWorkflow(workflowId);
    }

    // ==================== 惰性初始化 ====================

    private CharacterExecutor getExecutor() {
        if (executor == null) {
            executor = CharacterExecutor.builder()
                    .profile(profile)
                    .runtime(runtime)
                    .config(executorConfig)
                    .build();
        }
        return executor;
    }

    // ==================== 兼容旧接口 ====================

    public org.dragon.agent.react.ReActExecutor getReActExecutor() {
        return runtime != null ? runtime.getReActExecutor() : null;
    }

    public org.dragon.config.PromptManager getPromptManager() {
        return runtime != null ? runtime.getPromptManager() : null;
    }

    public org.dragon.agent.workflow.WorkflowExecutor getWorkflowExecutor() {
        return runtime != null ? runtime.getWorkflowExecutor() : null;
    }

    public org.dragon.agent.workflow.WorkflowStore getWorkflowStore() {
        return runtime != null ? runtime.getWorkflowStore() : null;
    }

    public org.dragon.agent.model.ModelRegistry getModelRegistry() {
        return runtime != null ? runtime.getModelRegistry() : null;
    }

    public org.dragon.agent.orchestration.OrchestrationService getOrchestrationService() {
        return runtime != null ? runtime.getOrchestrationService() : null;
    }
}
