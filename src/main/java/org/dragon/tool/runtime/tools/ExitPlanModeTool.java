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

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 退出规划模式工具实现。
 *
 * <p>对应 TypeScript 版本的 {@code src/tools/ExitPlanModeTool/ExitPlanModeV2Tool.ts}。
 * 让 Agent 在规划模式下提交实现计划，等待用户审批后才能开始编写代码。
 *
 * <h3>功能：</h3>
 * <ul>
 *   <li>将当前会话从 plan 模式切回 normal 模式</li>
 *   <li>记录 Agent 提交的实现计划（plan 字段）</li>
 *   <li>收到用户批准后才能继续写代码</li>
 * </ul>
 *
 * <h3>输入 Schema：</h3>
 * <pre>
 * {
 *   "plan": "Detailed implementation plan here..."
 * }
 * </pre>
 */
@Slf4j
@Component
public class ExitPlanModeTool extends AbstractTool<ExitPlanModeTool.Input, ExitPlanModeTool.Output> {

    private static final long MAX_RESULT_SIZE = 100_000;

    public ExitPlanModeTool() {
        super("ExitPlanMode",
                "Exit plan mode and present the implementation plan to the user for approval.",
                Input.class);
    }

    // ── 核心方法 ─────────────────────────────────────────────────────────

    @Override
    protected CompletableFuture<ToolResult<Output>> doCall(Input input,
                                                           ToolUseContext context,
                                                           Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            String plan = input.getPlan() != null ? input.getPlan() : "";
            String sessionKey = context.getSessionId() != null ? context.getSessionId() : "default";

            // 退出规划模式
            EnterPlanModeTool.SESSION_MODE_STORE.remove(sessionKey);

            log.info("[ExitPlanModeTool] 退出规划模式: session={}, planLength={}",
                    sessionKey, plan.length());

            return ToolResult.ok(Output.builder()
                    .plan(plan)
                    .message("Plan mode exited. Your plan has been presented to the user.")
                    .build());
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(Output output, String toolUseId) {
        return ToolResultBlockParam.ofText(toolUseId,
                "The plan has been presented to the user for approval. " +
                "Please wait for the user to review and approve before proceeding with implementation.");
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
        return "exit plan mode and present plan for user approval";
    }

    @Override
    public String getUserFacingName(Input input) {
        return "";
    }

    // ── 输入输出类型 ─────────────────────────────────────────────────────

    @Data
    @Builder
    public static class Input {
        private String plan;
    }

    @Data
    @Builder
    public static class Output {
        private String plan;
        private String message;
    }
}
