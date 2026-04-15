package org.dragon.agent.react.context;

import lombok.Builder;
import lombok.Data;

/**
 * Prompt 物料上下文配置 - 控制各部分是否包含在 PromptMaterialContext 中
 */
@Data
@Builder
public class PromptMaterialConfig {

    // ========== Workspace 级别开关 ==========

    @Builder.Default
    private boolean includeWorkspacePersonality = true;

    @Builder.Default
    private boolean includeWorkspaceMembers = true;

    @Builder.Default
    private boolean includeWorkspaceBasicInfo = true;

    // ========== Character 级别开关 ==========

    @Builder.Default
    private boolean includeCharacterPersonality = true;

    @Builder.Default
    private boolean includeAvailableSkills = true;

    // ========== Task 级别开关 ==========

    @Builder.Default
    private boolean includeTaskContext = true;

    // ========== Evaluation/Observability 开关 ==========

    /**
     * 默认关闭，避免过大
     */
    @Builder.Default
    private boolean includeRecentEvaluation = false;

    @Builder.Default
    private boolean includeRecentFindings = false;

    // ========== Memory 开关 ==========

    @Builder.Default
    private boolean includeRecentMemories = false;

    // ========== Collaboration 开关 ==========

    @Builder.Default
    private boolean includeCollaborationContext = true;

    // ========== 预设配置 ==========

    /**
     * ReAct 执行默认配置
     */
    public static PromptMaterialConfig defaultReAct() {
        return PromptMaterialConfig.builder().build();
    }

    /**
     * 成员选择配置
     */
    public static PromptMaterialConfig memberSelection() {
        return PromptMaterialConfig.builder()
                .includeWorkspacePersonality(true)
                .includeWorkspaceMembers(true)
                .includeTaskContext(true)
                .includeCharacterPersonality(false)
                .includeAvailableSkills(false)
                .includeRecentEvaluation(false)
                .includeRecentMemories(false)
                .build();
    }

    /**
     * 任务分解配置
     */
    public static PromptMaterialConfig taskDecomposition() {
        return PromptMaterialConfig.builder()
                .includeWorkspacePersonality(true)
                .includeWorkspaceMembers(true)
                .includeTaskContext(true)
                .includeCharacterPersonality(false)
                .includeAvailableSkills(false)
                .includeCollaborationContext(false)
                .build();
    }

    /**
     * Observer 建议配置
     */
    public static PromptMaterialConfig observerSuggestion() {
        return PromptMaterialConfig.builder()
                .includeRecentEvaluation(true)
                .includeRecentFindings(true)
                .includeWorkspacePersonality(true)
                .includeWorkspaceBasicInfo(true)
                .includeCharacterPersonality(false)
                .includeAvailableSkills(false)
                .includeTaskContext(false)
                .build();
    }
}
