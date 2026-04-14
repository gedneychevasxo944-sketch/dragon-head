package org.dragon.tool.runtime;

import lombok.Builder;
import lombok.Data;

/**
 * 工具进度数据。
 *
 * <p>对应 TypeScript 版本的 {@code ToolProgress<P>} 类型：
 * <pre>
 * export type ToolProgress&lt;P extends ToolProgressData&gt; = {
 *   toolUseID: string
 *   data: P
 * }
 * </pre>
 *
 * <p>用于在工具执行过程中报告进度，支持流式更新 UI。
 */
@Data
@Builder
public class ToolProgress {

    /**
     * 对应的工具调用 ID。
     */
    private final String toolUseId;

    /**
     * 进度数据。
     */
    private final ToolProgressData data;

    /**
     * 父工具调用 ID（嵌套工具调用时）。
     */
    private final String parentToolUseId;

    // ── 进度数据子类型 ───────────────────────────────────────────────────

    /**
     * 进度数据基类。
     */
    public interface ToolProgressData {
        String getType();
    }

    /**
     * Bash 工具进度。
     */
    @Data
    @Builder
    public static class BashProgress implements ToolProgressData {
        @Override
        public String getType() { return "bash_progress"; }

        private final String command;
        private final String output;
        private final long elapsedMs;
        private final boolean isBackground;
        private final String backgroundTaskId;
    }

    /**
     * Skill 工具进度。
     */
    @Data
    @Builder
    public static class SkillProgress implements ToolProgressData {
        @Override
        public String getType() { return "skill_progress"; }

        private final String skillName;
        private final String status;
        private final String message;
    }

    /**
     * Agent 工具进度。
     */
    @Data
    @Builder
    public static class AgentProgress implements ToolProgressData {
        @Override
        public String getType() { return "agent_progress"; }

        private final String agentId;
        private final String agentType;
        private final String status;
        private final String description;
    }

    /**
     * Task 输出进度。
     */
    @Data
    @Builder
    public static class TaskOutputProgress implements ToolProgressData {
        @Override
        public String getType() { return "task_output_progress"; }

        private final String taskId;
        private final String status;
        private final String output;
    }

    /**
     * Web 搜索进度。
     */
    @Data
    @Builder
    public static class WebSearchProgress implements ToolProgressData {
        @Override
        public String getType() { return "web_search_progress"; }

        private final String query;
        private final String status;
        private final int resultsFound;
    }

    /**
     * Hook 进度。
     */
    @Data
    @Builder
    public static class HookProgress implements ToolProgressData {
        @Override
        public String getType() { return "hook_progress"; }

        private final String hookName;
        private final String hookType;
        private final String status;
        private final long durationMs;
    }

    // ── 静态工厂方法 ─────────────────────────────────────────────────────

    /**
     * 创建 Bash 进度。
     */
    public static ToolProgress bash(String toolUseId, String command, String output, long elapsedMs) {
        return ToolProgress.builder()
                .toolUseId(toolUseId)
                .data(BashProgress.builder()
                        .command(command)
                        .output(output)
                        .elapsedMs(elapsedMs)
                        .build())
                .build();
    }

    /**
     * 创建 Skill 进度。
     */
    public static ToolProgress skill(String toolUseId, String skillName, String status, String message) {
        return ToolProgress.builder()
                .toolUseId(toolUseId)
                .data(SkillProgress.builder()
                        .skillName(skillName)
                        .status(status)
                        .message(message)
                        .build())
                .build();
    }

    /**
     * 创建 Agent 进度。
     */
    public static ToolProgress agent(String toolUseId, String agentId, String status, String description) {
        return ToolProgress.builder()
                .toolUseId(toolUseId)
                .data(AgentProgress.builder()
                        .agentId(agentId)
                        .status(status)
                        .description(description)
                        .build())
                .build();
    }
}
