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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 进入规划模式工具实现。
 *
 * <p>对应 TypeScript 版本的 {@code src/tools/EnterPlanModeTool/EnterPlanModeTool.ts}。
 * 让 Agent 进入规划模式：只能探索代码库和设计方案，不能写文件。
 *
 * <h3>功能：</h3>
 * <ul>
 *   <li>将当前会话标记为 plan 模式</li>
 *   <li>返回规划阶段的操作指导</li>
 *   <li>按 session 记录模式状态</li>
 * </ul>
 *
 * <h3>输入 Schema：</h3>
 * <pre>
 * {}  // 无参数
 * </pre>
 */
@Slf4j
@Component
public class EnterPlanModeTool extends AbstractTool<EnterPlanModeTool.Input, EnterPlanModeTool.Output> {

    /** Session → 当前模式状态的全局存储 */
    public static final Map<String, String> SESSION_MODE_STORE = new ConcurrentHashMap<>();

    public static final String MODE_PLAN = "plan";
    public static final String MODE_NORMAL = "normal";

    private static final long MAX_RESULT_SIZE = 100_000;

    public EnterPlanModeTool() {
        super("EnterPlanMode",
                "Requests permission to enter plan mode for complex tasks requiring exploration and design",
                Input.class);
    }

    // ── 核心方法 ─────────────────────────────────────────────────────────

    @Override
    protected CompletableFuture<ToolResult<Output>> doCall(Input input,
                                                           ToolUseContext context,
                                                           Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            // 不允许在子 Agent 中使用
            if (context.getAgentId() != null) {
                return ToolResult.fail("EnterPlanMode tool cannot be used in agent contexts");
            }

            String sessionKey = context.getSessionId() != null ? context.getSessionId() : "default";
            SESSION_MODE_STORE.put(sessionKey, MODE_PLAN);

            log.info("[EnterPlanModeTool] 进入规划模式: session={}", sessionKey);

            return ToolResult.ok(Output.builder()
                    .message("Entered plan mode. You should now focus on exploring the codebase " +
                             "and designing an implementation approach.")
                    .build());
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(Output output, String toolUseId) {
        String instructions = output.getMessage() + "\n\n" +
                "In plan mode, you should:\n" +
                "1. Thoroughly explore the codebase to understand existing patterns\n" +
                "2. Identify similar features and architectural approaches\n" +
                "3. Consider multiple approaches and their trade-offs\n" +
                "4. Use AskUserQuestion if you need to clarify the approach\n" +
                "5. Design a concrete implementation strategy\n" +
                "6. When ready, use ExitPlanMode to present your plan for approval\n\n" +
                "Remember: DO NOT write or edit any files yet. This is a read-only exploration and planning phase.";

        return ToolResultBlockParam.ofText(toolUseId, instructions);
    }

    // ── 元信息 ───────────────────────────────────────────────────────────

    @Override
    public boolean isReadOnly(Input input) {
        return true;
    }

    @Override
    public boolean isConcurrencySafe(Input input) {
        return true;
    }

    @Override
    public long getMaxResultSizeChars() {
        return MAX_RESULT_SIZE;
    }

    @Override
    public String getSearchHint() {
        return "switch to plan mode to design an approach before coding";
    }

    @Override
    public String getUserFacingName(Input input) {
        return "";
    }

    // ── 输入输出类型 ─────────────────────────────────────────────────────

    @Data
    @Builder
    public static class Input {
        // 无参数
    }

    @Data
    @Builder
    public static class Output {
        private String message;
    }
}
