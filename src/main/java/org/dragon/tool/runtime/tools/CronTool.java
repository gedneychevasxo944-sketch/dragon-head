package org.dragon.tool.runtime.tools;

import com.fasterxml.jackson.databind.node.ObjectNode;
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
 * 定时任务工具 — 管理计划任务（提醒、循环作业）。
 *
 * <p>对应 TypeScript 的 {@code tools/cron-tool.ts}。
 *
 * <h3>输入 Schema：</h3>
 * <pre>
 * {
 *   "action": "list|create|update|delete|pause|resume",
 *   "id": "job-id",           // update/delete/pause/resume 时必填
 *   "label": "提醒标签",
 *   "cron": "0 9 * * *",      // cron 表达式（create 时与 at 二选一）
 *   "at": "2026-01-01T09:00", // 单次触发时间（create 时与 cron 二选一）
 *   "timezone": "Asia/Shanghai",
 *   "text": "提醒内容",
 *   "mode": "message|cron|hook",
 *   "contextMessages": 5,
 *   "includeDisabled": false
 * }
 * </pre>
 */
@Slf4j
@Component
public class CronTool extends AbstractTool<CronTool.Input, CronTool.Output> {

    public CronTool() {
        super("cron", "Manage scheduled tasks and reminders. " +
                "Actions: list, create, update, delete, pause, resume.", Input.class);
    }

    // ── 核心方法 ─────────────────────────────────────────────────────────

    @Override
    protected CompletableFuture<ToolResult<Output>> doCall(Input input,
                                                            ToolUseContext context,
                                                            Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            String action = input.getAction();
            if (action == null || action.isBlank()) {
                return ToolResult.fail("'action' is required");
            }

            log.info("[CronTool] action={}", action);

            switch (action) {
                case "create": {
                    String cron = input.getCron();
                    String at = input.getAt();
                    if ((cron == null || cron.isBlank()) && (at == null || at.isBlank())) {
                        return ToolResult.fail("Either 'cron' or 'at' required for create");
                    }
                    return buildStubResult(action, input);
                }
                case "update":
                case "delete":
                case "pause":
                case "resume": {
                    String id = input.getId();
                    if (id == null || id.isBlank()) {
                        return ToolResult.fail("'id' required for " + action);
                    }
                    return buildStubResult(action, input);
                }
                case "list":
                    return buildStubResult(action, input);
                default:
                    return ToolResult.fail("Unknown cron action: " + action);
            }
        });
    }

    /**
     * 构建 stub 输出（TODO 接入网关 cron API 后替换）。
     */
    private ToolResult<Output> buildStubResult(String action, Input input) {
        Output output = Output.builder()
                .action(action)
                .id(input.getId())
                .label(input.getLabel())
                .status("list".equals(action) ? "ok" : action + "d")
                .note("Cron stub — wire up gateway cron API")
                .build();
        return ToolResult.ok(output);
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(Output output, String toolUseId) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("action", output.getAction());
        if (output.getId() != null) {
            result.put("id", output.getId());
        }
        if (output.getLabel() != null) {
            result.put("label", output.getLabel());
        }
        result.put("status", output.getStatus());
        result.put("note", output.getNote());
        return ToolResultBlockParam.ofText(toolUseId, result.toString());
    }

    // ── 元信息 ───────────────────────────────────────────────────────────

    @Override
    public boolean isReadOnly(Input input) {
        return "list".equals(input.getAction());
    }

    @Override
    public boolean isConcurrencySafe(Input input) {
        return false;
    }

    @Override
    public String getActivityDescription(Input input) {
        return "Cron: " + (input.getAction() != null ? input.getAction() : "");
    }

    // ── 输入输出类型 ─────────────────────────────────────────────────────

    /**
     * 输入参数。
     */
    @Data
    @Builder
    public static class Input {
        /** 操作类型：list | create | update | delete | pause | resume。 */
        private String action;
        /** 任务 ID（update/delete/pause/resume 时必填）。 */
        private String id;
        /** 人类可读标签。 */
        private String label;
        /** Cron 表达式，如 {@code 0 9 * * *}。 */
        private String cron;
        /** 单次触发时间（ISO-8601 或相对时间）。 */
        private String at;
        /** 时区，默认 UTC。 */
        private String timezone;
        /** 提醒内容文本。 */
        private String text;
        /** 唤醒模式：message | cron | hook。 */
        private String mode;
        /** 网关 URL 覆盖。 */
        private String gatewayUrl;
        /** 网关鉴权 Token。 */
        private String gatewayToken;
        /** 上下文消息条数（0-10）。 */
        private Integer contextMessages;
        /** 列表时是否包含已暂停的任务。 */
        private Boolean includeDisabled;
    }

    /**
     * 输出结果。
     */
    @Data
    @Builder
    public static class Output {
        private String action;
        private String id;
        private String label;
        private String status;
        private String note;
    }
}
