package org.dragon.character.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Character 执行引擎配置
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterExecutorConfig {

    /**
     * 默认模型 ID
     */
    private String defaultModelId;

    /**
     * 工作流配置
     */
    private WorkflowConfig workflowConfig;

    /**
     * ReAct 配置
     */
    private ReActConfig reActConfig;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowConfig {
        private String defaultWorkflowId;
        private int maxSteps;
        private String timeout;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReActConfig {
        private int maxIterations;
        private boolean enableMemorySearch;
        private boolean enableToolUse;
    }
}
