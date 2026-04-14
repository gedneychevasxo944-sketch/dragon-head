package org.dragon.tool.runtime.tools;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dragon.tool.runtime.AbstractTool;
import org.dragon.tool.runtime.ToolProgress;
import org.dragon.tool.runtime.ToolResult;
import org.dragon.tool.runtime.ToolResultBlockParam;
import org.dragon.tool.runtime.ToolUseContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Todo 任务列表管理工具实现。
 *
 * <p>对应 TypeScript 版本的 {@code src/tools/TodoWriteTool/TodoWriteTool.ts}。
 * 维护 Agent 会话级别的任务列表，帮助 LLM 追踪任务进度。
 *
 * <h3>功能：</h3>
 * <ul>
 *   <li>写入/更新任务列表（merge 模式）</li>
 *   <li>支持 pending / in_progress / completed / cancelled 四种状态</li>
 *   <li>当所有任务完成时自动清空列表</li>
 *   <li>按 session/agent 隔离存储</li>
 * </ul>
 *
 * <h3>输入 Schema：</h3>
 * <pre>
 * {
 *   "todos": [
 *     { "id": "1", "content": "Task description", "status": "pending" },
 *     { "id": "2", "content": "Another task", "status": "in_progress" }
 *   ]
 * }
 * </pre>
 */
@Slf4j
@Component
public class TodoWriteTool extends AbstractTool<TodoWriteTool.Input, TodoWriteTool.Output> {

    /** Session → todoList 的内存存储（云端应替换为 Redis/DB） */
    private static final Map<String, List<TodoItem>> TODO_STORE = new ConcurrentHashMap<>();

    private static final long MAX_RESULT_SIZE = 100_000;

    public TodoWriteTool() {
        super("TodoWrite", "Write the todo list to track tasks. Use this to manage and update your task checklist.", Input.class);
    }

    // ── 核心方法 ─────────────────────────────────────────────────────────

    @Override
    protected CompletableFuture<ToolResult<Output>> doCall(Input input,
                                                           ToolUseContext context,
                                                           Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            List<TodoItem> newTodos = input.getTodos() != null ? input.getTodos() : List.of();

            // 按 agentId 或 sessionId 隔离
            String storeKey = context.getAgentId() != null
                    ? context.getAgentId()
                    : (context.getSessionId() != null ? context.getSessionId() : "default");

            List<TodoItem> oldTodos = TODO_STORE.getOrDefault(storeKey, List.of());

            // 当所有任务都已完成时，清空列表
            boolean allDone = newTodos.stream().allMatch(t -> "completed".equals(t.getStatus()));
            List<TodoItem> savedTodos = allDone ? new ArrayList<>() : newTodos;

            TODO_STORE.put(storeKey, savedTodos);

            log.info("[TodoWriteTool] 更新任务列表: key={}, items={}, allDone={}",
                    storeKey, newTodos.size(), allDone);

            return ToolResult.ok(Output.builder()
                    .oldTodos(new ArrayList<>(oldTodos))
                    .newTodos(newTodos)
                    .build());
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(Output output, String toolUseId) {
        return ToolResultBlockParam.ofText(toolUseId,
                "Todos have been modified successfully. " +
                "Ensure that you continue to use the todo list to track your progress. " +
                "Please proceed with the current tasks if applicable.");
    }

    // ── 元信息 ───────────────────────────────────────────────────────────

    @Override
    public boolean isReadOnly(Input input) {
        return false;
    }

    @Override
    public boolean isConcurrencySafe(Input input) {
        return false;
    }

    @Override
    public long getMaxResultSizeChars() {
        return MAX_RESULT_SIZE;
    }

    @Override
    public String getSearchHint() {
        return "manage the session task checklist";
    }

    @Override
    public String getUserFacingName(Input input) {
        return "";
    }

    // ── 输入输出类型 ─────────────────────────────────────────────────────

    @Data
    @Builder
    public static class TodoItem {
        private String id;
        private String content;
        /** pending | in_progress | completed | cancelled */
        private String status;
    }

    @Data
    @Builder
    public static class Input {
        private List<TodoItem> todos;
        /** merge=true 则合并现有列表，false 则覆盖（默认 false = 覆盖） */
        private Boolean merge;
    }

    @Data
    @Builder
    public static class Output {
        private List<TodoItem> oldTodos;
        private List<TodoItem> newTodos;
    }
}
