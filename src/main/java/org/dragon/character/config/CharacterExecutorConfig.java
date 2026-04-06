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
        @Builder.Default
        private Integer maxSteps = 10;
        private String timeout;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReActConfig {
        @Builder.Default
        private Integer maxIterations = 10;
        @Builder.Default
        private Boolean enableMemorySearch = true;
        @Builder.Default
        private Boolean enableToolUse = true;

        /**
         * 检查是否启用记忆搜索
         */
        public boolean isEnableMemorySearch() {
            return Boolean.TRUE.equals(enableMemorySearch);
        }

        /**
         * 检查是否启用工具使用
         */
        public boolean isEnableToolUse() {
            return Boolean.TRUE.equals(enableToolUse);
        }
    }
}
