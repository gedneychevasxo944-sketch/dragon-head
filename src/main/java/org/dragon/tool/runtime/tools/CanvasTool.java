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
 * 画布工具 — 控制节点画布（显示/隐藏/导航/求值/截图/A2UI）。
 *
 * <p>对应 TypeScript 的 {@code tools/canvas-tool.ts}。
 *
 * <h3>输入 Schema：</h3>
 * <pre>
 * {
 *   "action": "present|hide|navigate|eval|snapshot|a2ui_push|a2ui_reset",
 *   "node": "node-id",
 *   "target": "https://...",    // present 时的 URL
 *   "url": "https://...",       // navigate 时的 URL
 *   "javaScript": "...",        // eval 时的 JS 代码
 *   "outputFormat": "png",      // snapshot 格式
 *   "jsonl": "...",             // a2ui_push 的 JSONL payload
 *   "jsonlPath": "/path/to/file", // a2ui_push 的 JSONL 文件路径
 *   "x": 0, "y": 0, "width": 1920, "height": 1080,
 *   "maxWidth": 1280, "quality": 85, "delayMs": 500
 * }
 * </pre>
 */
@Slf4j
@Component
public class CanvasTool extends AbstractTool<CanvasTool.Input, CanvasTool.Output> {

    public CanvasTool() {
        super("canvas", "Control node canvases (present/hide/navigate/eval/snapshot/A2UI).", Input.class);
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

            log.info("[CanvasTool] action={}", action);

            // TODO: 接入网关 node.invoke canvas.* API
            Output output = Output.builder()
                    .action(action)
                    .ok(true)
                    .note("Canvas stub — wire up gateway canvas API")
                    .build();

            return ToolResult.ok(output);
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(Output output, String toolUseId) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("action", output.getAction());
        result.put("ok", output.isOk());
        result.put("note", output.getNote());
        if (output.getSnapshotPath() != null) {
            result.put("snapshotPath", output.getSnapshotPath());
        }
        return ToolResultBlockParam.ofText(toolUseId, result.toString());
    }

    // ── 元信息 ───────────────────────────────────────────────────────────

    @Override
    public boolean isReadOnly(Input input) {
        return "snapshot".equals(input.getAction());
    }

    @Override
    public boolean isConcurrencySafe(Input input) {
        return false;
    }

    @Override
    public String getActivityDescription(Input input) {
        return "Canvas: " + (input.getAction() != null ? input.getAction() : "");
    }

    // ── 输入输出类型 ─────────────────────────────────────────────────────

    /**
     * 输入参数。
     */
    @Data
    @Builder
    public static class Input {
        /** 操作类型：present | hide | navigate | eval | snapshot | a2ui_push | a2ui_reset。 */
        private String action;
        /** 目标节点 ID。 */
        private String node;
        /** 网关 URL 覆盖。 */
        private String gatewayUrl;
        /** 网关鉴权 Token。 */
        private String gatewayToken;
        /** present 时要展示的 URL。 */
        private String target;
        /** navigate 时要导航到的 URL。 */
        private String url;
        /** eval 时要执行的 JavaScript。 */
        private String javaScript;
        /** snapshot 的输出格式：png 或 jpg。 */
        private String outputFormat;
        /** a2ui_push 的 JSONL payload。 */
        private String jsonl;
        /** a2ui_push 的 JSONL 文件路径。 */
        private String jsonlPath;
        /** X 坐标。 */
        private Double x;
        /** Y 坐标。 */
        private Double y;
        /** 画布宽度。 */
        private Double width;
        /** 画布高度。 */
        private Double height;
        /** snapshot 最大宽度。 */
        private Double maxWidth;
        /** JPEG 质量（0-100）。 */
        private Double quality;
        /** 截图前的延迟毫秒数。 */
        private Double delayMs;
    }

    /**
     * 输出结果。
     */
    @Data
    @Builder
    public static class Output {
        private String action;
        private boolean ok;
        private String note;
        /** snapshot 时生成的文件路径（TODO 接入后填充）。 */
        private String snapshotPath;
    }
}
