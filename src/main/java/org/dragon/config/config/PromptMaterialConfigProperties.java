package org.dragon.config.config;

import lombok.Data;
import org.dragon.agent.react.context.PromptMaterialConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Prompt 物料上下文配置属性
 * <p>
 * 通过 {@code dragon.prompt-material.*} 配置控制各部分是否包含在 PromptMaterialContext 中
 */
@Data
@ConfigurationProperties(prefix = "dragon.prompt-material")
public class PromptMaterialConfigProperties {

    // ========== Workspace 级别 ==========

    private boolean includeWorkspacePersonality = true;
    private boolean includeWorkspaceMembers = true;
    private boolean includeTeamPositions = true;
    private boolean includeWorkspaceBasicInfo = true;

    // ========== Character 级别 ==========

    private boolean includeCharacterPersonality = true;
    private boolean includeAvailableSkills = true;

    // ========== Task 级别 ==========

    private boolean includeTaskContext = true;

    // ========== Evaluation/Observability ==========

    private boolean includeRecentEvaluation = false;
    private boolean includeRecentFindings = false;

    // ========== Memory ==========

    private boolean includeRecentMemories = false;

    // ========== Collaboration ==========

    private boolean includeCollaborationContext = true;

    /**
     * 从配置构建 PromptMaterialConfig
     */
    public PromptMaterialConfig toConfig() {
        return PromptMaterialConfig.builder()
                .includeWorkspacePersonality(includeWorkspacePersonality)
                .includeWorkspaceMembers(includeWorkspaceMembers)
                .includeTeamPositions(includeTeamPositions)
                .includeWorkspaceBasicInfo(includeWorkspaceBasicInfo)
                .includeCharacterPersonality(includeCharacterPersonality)
                .includeAvailableSkills(includeAvailableSkills)
                .includeTaskContext(includeTaskContext)
                .includeRecentEvaluation(includeRecentEvaluation)
                .includeRecentFindings(includeRecentFindings)
                .includeRecentMemories(includeRecentMemories)
                .includeCollaborationContext(includeCollaborationContext)
                .build();
    }
}
