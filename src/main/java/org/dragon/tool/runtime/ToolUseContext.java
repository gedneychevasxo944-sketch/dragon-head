package org.dragon.tool.runtime;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 工具执行上下文。
 *
 * <p>对应 TypeScript 版本的 {@code ToolUseContext} 类型定义。
 *
 * <p>在每次工具调用时由框架创建，包含执行所需的所有上下文信息。
 */
@Data
@Builder
public class ToolUseContext {

    // ── 核心配置 ─────────────────────────────────────────────────────────

    /**
     * 工具执行选项。
     */
    private final Options options;

    /**
     * 中断控制器。
     *
     * <p>对应 TS: {@code abortController}
     * 用于取消正在执行的工具。
     */
    private final AbortController abortController;

    /**
     * 当前对话消息列表。
     *
     * <p>对应 TS: {@code messages}
     * 工具可以读取历史消息来理解上下文。
     */
    private final List<Map<String, Object>> messages;

    // ── 工具调用信息 ─────────────────────────────────────────────────────

    /**
     * 当前工具调用 ID。
     *
     * <p>对应 TS: {@code toolUseId}
     */
    private final String toolUseId;

    /**
     * 当前 Agent ID（子 Agent 时有值）。
     *
     * <p>对应 TS: {@code agentId}
     */
    private final String agentId;

    // ── 多租户隔离 ─────────────────────────────────────────────────────────

    /**
     * 租户 ID。
     *
     * <p>用于多租户场景下的数据隔离。
     */
    private final String tenantId;

    /**
     * 当前执行的 Character ID。
     *
     * <p>供需要感知 Character 维度的工具（如 SkillTool）使用，
     * 用于从 SkillRegistry、ToolRegistry 等加载与当前 Character 关联的资源。
     */
    private final String characterId;

    /**
     * 当前所在的 Workspace ID。
     *
     * <p>供需要感知 Workspace 维度的工具（如 SkillTool）使用，
     * 用于从 SkillRegistry、ToolRegistry 等加载与当前 Workspace 关联的资源。
     * 为 null 表示 Character 在独立（无 Workspace）模式下执行。
     */
    private final String workspaceId;

    /**
     * 会话 ID。
     *
     * <p>用于同一租户下的会话隔离。
     */
    private final String sessionId;

    /**
     * Agent 类型。
     *
     * <p>对应 TS: {@code agentType}
     */
    private final String agentType;

    // ── 状态管理 ─────────────────────────────────────────────────────────

    /**
     * 用户是否修改过输入。
     *
     * <p>对应 TS: {@code userModified}
     * 权限检查时用户可能修改工具输入。
     */
    @Builder.Default
    private boolean userModified = false;

    /**
     * 是否保留 toolUseResult（子 Agent 时）。
     *
     * <p>对应 TS: {@code preserveToolUseResults}
     */
    @Builder.Default
    private boolean preserveToolUseResults = false;

    // ── 内容替换状态 ─────────────────────────────────────────────────────

    /**
     * 内容替换状态（用于工具结果预算管理）。
     *
     * <p>对应 TS: {@code contentReplacementState}
     */
    private final ContentReplacementState contentReplacementState;

    // ── 文件状态缓存 ─────────────────────────────────────────────────────

    /**
     * 文件读取状态缓存。
     *
     * <p>对应 TS: {@code readFileState}
     */
    private final FileStateCache readFileState;

    // ── 限制配置 ─────────────────────────────────────────────────────────

    /**
     * 文件读取限制。
     */
    private final FileReadingLimits fileReadingLimits;

    /**
     * Glob 搜索限制。
     */
    private final GlobLimits globLimits;

    // ── 权限追踪 ─────────────────────────────────────────────────────────

    /**
     * 工具决策记录。
     *
     * <p>对应 TS: {@code toolDecisions}
     * 记录每个工具调用的权限决策。
     */
    private final Map<String, ToolDecision> toolDecisions;

    // ── 查询追踪 ─────────────────────────────────────────────────────────

    /**
     * 查询链追踪信息。
     */
    private final QueryChainTracking queryTracking;

    // ── 回调函数 ─────────────────────────────────────────────────────────

    /**
     * 添加通知的回调。
     */
    private final Consumer<Object> addNotification;

    /**
     * 添加系统消息的回调。
     */
    private final Consumer<Object> appendSystemMessage;

    /**
     * 设置工具 JSX UI 的回调。
     */
    private final Function<SetToolJSXParams, Void> setToolJSX;

    // ── 便捷方法 ─────────────────────────────────────────────────────────

    /**
     * 检查是否已被取消。
     */
    public boolean isAborted() {
        return abortController != null && abortController.isAborted();
    }

    /**
     * 获取消息数量。
     */
    public int getMessageCount() {
        return messages != null ? messages.size() : 0;
    }

    /**
     * 创建子 Agent 上下文。
     *
     * <p>对应 TS: {@code createSubagentContext}
     */
    public ToolUseContext createSubagentContext(String subagentId) {
        return ToolUseContext.builder()
                .options(this.options)
                .abortController(new AbortController())
                .messages(this.messages)
                .toolUseId(null)  // 子 agent 新的工具调用会有新的 ID
                .agentId(subagentId)
                .agentType(this.agentType)
                .tenantId(this.tenantId)
                .characterId(this.characterId)
                .workspaceId(this.workspaceId)
                .sessionId(this.sessionId)
                .contentReplacementState(
                        this.contentReplacementState != null
                                ? this.contentReplacementState.clone()
                                : null
                )
                .readFileState(this.readFileState)
                .fileReadingLimits(this.fileReadingLimits)
                .globLimits(this.globLimits)
                .build();
    }

    // ── 内部类型 ─────────────────────────────────────────────────────────

    /**
     * 工具执行选项。
     */
    @Data
    @Builder
    public static class Options {
        private final List<Object> commands;
        private final List<Tool<?, ?>> tools;
        private final String mainLoopModel;
        private final boolean debug;
        private final boolean verbose;
        private final boolean isNonInteractiveSession;
        private final ThinkingConfig thinkingConfig;
        private final List<Object> agentDefinitions;
        private final Double maxBudgetUsd;
        private final String customSystemPrompt;
        private final String appendSystemPrompt;
        private final String querySource;
    }

    /**
     * 中断控制器。
     */
    @Data
    public static class AbortController {
        private volatile boolean aborted = false;
        private final Object lock = new Object();

        public void abort() {
            synchronized (lock) {
                aborted = true;
                lock.notifyAll();
            }
        }

        public boolean isAborted() {
            return aborted;
        }
    }

    /**
     * 思考配置。
     */
    @Data
    @Builder
    public static class ThinkingConfig {
        private final int thinkingBudget;
        private final boolean interleavedThinking;
    }

    /**
     * 内容替换状态。
     */
    @Data
    public static class ContentReplacementState {
        private final Set<String> seenIds = ConcurrentHashMap.newKeySet();
        private final Map<String, String> replacements = new ConcurrentHashMap<>();

        public ContentReplacementState clone() {
            ContentReplacementState cloned = new ContentReplacementState();
            cloned.seenIds.addAll(this.seenIds);
            cloned.replacements.putAll(this.replacements);
            return cloned;
        }
    }

    /**
     * 文件状态缓存。
     */
    @Data
    @Builder
    public static class FileStateCache {
        private final Map<String, FileState> cache = new ConcurrentHashMap<>();

        @Data
        @Builder
        public static class FileState {
            private final long lastModified;
            private final long size;
            private final String hash;
        }
    }

    /**
     * 文件读取限制。
     */
    @Data
    @Builder
    public static class FileReadingLimits {
        private final Integer maxTokens;
        private final Long maxSizeBytes;
    }

    /**
     * Glob 搜索限制。
     */
    @Data
    @Builder
    public static class GlobLimits {
        private final Integer maxResults;
    }

    /**
     * 工具决策记录。
     */
    @Data
    @Builder
    public static class ToolDecision {
        private final String source;
        private final String decision;  // "accept" | "reject"
        private final long timestamp;
    }

    /**
     * 查询链追踪。
     */
    @Data
    @Builder
    public static class QueryChainTracking {
        private final String chainId;
        private final int depth;
    }

    /**
     * 设置工具 JSX 参数。
     */
    @Data
    @Builder
    public static class SetToolJSXParams {
        private final Object jsx;
        private final boolean shouldHidePromptInput;
        private final boolean shouldContinueAnimation;
        private final boolean showSpinner;
        private final boolean isLocalJSXCommand;
        private final boolean isImmediate;
        private final boolean clearLocalJSX;
    }
}
