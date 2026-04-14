package org.dragon.tool.runtime.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.dragon.tool.runtime.AbstractTool;
import org.dragon.tool.runtime.ToolProgress;
import org.dragon.tool.runtime.ToolResult;
import org.dragon.tool.runtime.ToolResultBlockParam;
import org.dragon.tool.runtime.ToolUseContext;
import org.dragon.tool.runtime.tools.browser.BrowserClient;
import org.dragon.tool.runtime.tools.browser.BrowserTypes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 浏览器控制工具。
 *
 * <p>对应 TypeScript 版本的 {@code agents/tools/browser-tool.ts createBrowserTool()}。
 *
 * <p><b>调用链路</b>：LLM tool_call → {@link #doCall} → {@link BrowserClient} (HTTP)
 * → BrowserControlServer（Netty，端口 18791）→ PlaywrightSession → Chrome（CDP）
 *
 * <p><b>核心操作</b>：
 * <ul>
 *   <li>生命周期：{@code start / stop / status / profiles}</li>
 *   <li>标签页：{@code tabs / open / focus / close}</li>
 *   <li>页面交互：{@code snapshot / screenshot / navigate / act / console / pdf}</li>
 * </ul>
 *
 * <p><b>推荐流程</b>（快照优先）：先 {@code snapshot} 获取 ARIA 树理解页面结构，
 * 再 {@code act} 执行操作，减少截图 token 消耗。
 *
 * <p>{@code screenshot} 结果会以多模态形式返回（图片 + 文本），供视觉模型查看。
 *
 * <p>实例由 {@link org.dragon.tool.runtime.factory.AtomicToolFactory} 预置，
 * 通过 Spring 依赖注入 {@link BrowserClient}。
 */
@Slf4j
public class BrowserTool extends AbstractTool<JsonNode, BrowserTool.Output> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BrowserClient client;

    /**
     * @param client 浏览器控制 HTTP 客户端
     */
    public BrowserTool(BrowserClient client) {
        super("browser",
                "Control the browser (status/start/stop/tabs/open/snapshot/screenshot/act). "
                        + "Use snapshot+act for UI automation. "
                        + "act kinds: click, type, press, hover, scrollIntoView, drag, select, fill, resize, wait, evaluate, goBack, goForward, close. "
                        + "When using refs from snapshot, keep the same tab via targetId. "
                        + "screenshot supports ref/element for element-level capture. "
                        + "wait supports: timeMs, text, textGone, selector, url, loadState, fn. "
                        + "Use the 'profile' param to target a specific browser profile (default: config default).",
                JsonNode.class);
        this.client = client;
    }

    // ── Tool 元信息 ───────────────────────────────────────────────────────

    @Override
    public boolean isReadOnly(JsonNode input) {
        // 浏览器操作通常不是只读的（navigate/act 会修改页面状态）
        String action = optString(input, "action");
        return "status".equals(action) || "profiles".equals(action)
                || "tabs".equals(action) || "snapshot".equals(action)
                || "screenshot".equals(action) || "console".equals(action);
    }

    @Override
    public boolean requiresUserInteraction() {
        // 非无头模式下浏览器窗口可见，视为需要用户感知
        return false;
    }

    @Override
    public long getMaxResultSizeChars() {
        // snapshot 内容可能较大，允许 200K
        return 200_000;
    }

    @Override
    public String getActivityDescription(JsonNode input) {
        String action = optString(input, "action");
        String url = optString(input, "targetUrl");
        if ("navigate".equals(action) && url != null) {
            return "Navigating to " + url;
        }
        if ("snapshot".equals(action)) {
            return "Taking page snapshot";
        }
        if ("screenshot".equals(action)) {
            return "Taking screenshot";
        }
        return action != null ? "Browser: " + action : "Browser action";
    }

    // ── 核心执行 ──────────────────────────────────────────────────────────

    @Override
    protected CompletableFuture<ToolResult<Output>> doCall(JsonNode input,
                                                           ToolUseContext context,
                                                           Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(input);
            } catch (IllegalArgumentException e) {
                return ToolResult.fail(e.getMessage());
            } catch (Exception e) {
                log.error("[BrowserTool] 执行失败: action={}, error={}",
                        optString(input, "action"), e.getMessage(), e);
                return ToolResult.fail("Browser error: " + e.getMessage());
            }
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(Output output, String toolUseId) {
        if (!output.isSuccess()) {
            return ToolResultBlockParam.ofError(toolUseId, output.getError());
        }

        // 截图：多模态结果（图片块 + 文本块）
        if (output.getImageBase64() != null && !output.getImageBase64().isEmpty()) {
            List<ToolResultBlockParam.ContentBlock> blocks = new ArrayList<>();
            blocks.add(ToolResultBlockParam.ImageBlock.builder()
                    .source(ToolResultBlockParam.ImageSource.builder()
                            .type("base64")
                            .mediaType(output.getImageMediaType() != null
                                    ? output.getImageMediaType() : "image/png")
                            .data(output.getImageBase64())
                            .build())
                    .build());
            blocks.add(ToolResultBlockParam.TextBlock.builder()
                    .text(output.getText() != null ? output.getText() : "Screenshot captured")
                    .build());
            return ToolResultBlockParam.ofBlocks(toolUseId, blocks);
        }

        // 普通文本结果
        return ToolResultBlockParam.ofText(toolUseId,
                output.getText() != null ? output.getText() : "");
    }

    // ── action 分发 ───────────────────────────────────────────────────────

    private ToolResult<Output> execute(JsonNode params) throws Exception {
        String action = requireString(params, "action");
        String profile = optString(params, "profile");

        return switch (action) {
            case "status" -> {
                BrowserTypes.BrowserStatus status = client.status(profile);
                yield jsonOutput(status);
            }
            case "start" -> {
                boolean headless = optBoolean(params, "headless", false);
                client.start(profile, headless);
                yield jsonOutput(Map.of("ok", true));
            }
            case "stop" -> {
                client.stop(profile);
                yield jsonOutput(Map.of("ok", true, "stopped", true));
            }
            case "profiles" -> {
                List<BrowserTypes.ProfileStatus> profiles = client.profiles();
                yield jsonOutput(Map.of("profiles", profiles));
            }
            case "tabs" -> {
                List<BrowserTypes.BrowserTab> tabs = client.tabs(profile);
                yield jsonOutput(Map.of("tabs", tabs));
            }
            case "open" -> {
                String targetUrl = requireString(params, "targetUrl");
                BrowserTypes.BrowserTab tab = client.openTab(targetUrl, profile);
                yield jsonOutput(tab);
            }
            case "focus" -> {
                String targetId = requireString(params, "targetId");
                client.focusTab(targetId, profile);
                yield jsonOutput(Map.of("ok", true));
            }
            case "close" -> {
                String targetId = optString(params, "targetId");
                if (targetId != null) {
                    client.closeTab(targetId, profile);
                } else {
                    // 关闭当前活跃标签页（通过 act close）
                    Map<String, Object> req = new LinkedHashMap<>();
                    req.put("kind", "close");
                    client.act(req, profile);
                }
                yield jsonOutput(Map.of("ok", true));
            }
            case "snapshot" -> handleSnapshot(params, profile);
            case "screenshot" -> handleScreenshot(params, profile);
            case "navigate" -> {
                String targetUrl = requireString(params, "targetUrl");
                String targetId = optString(params, "targetId");
                BrowserTypes.NavigateResult nav = client.navigate(targetUrl, targetId, profile);
                yield jsonOutput(nav);
            }
            case "console" -> {
                String targetId = optString(params, "targetId");
                String level = optString(params, "level");
                JsonNode messages = client.consoleMessages(level, targetId, profile);
                yield jsonOutput(messages);
            }
            case "pdf" -> {
                String targetId = optString(params, "targetId");
                BrowserTypes.PdfResult pdf = client.pdf(targetId, profile);
                yield jsonOutput(pdf);
            }
            case "upload" -> ToolResult.fail("File upload not yet implemented");
            case "dialog" -> ToolResult.fail("Dialog handling not yet implemented");
            case "act" -> handleAct(params, profile);
            default -> ToolResult.fail("Unknown browser action: " + action);
        };
    }

    // ── action 处理器 ─────────────────────────────────────────────────────

    private ToolResult<Output> handleSnapshot(JsonNode params, String profile) throws Exception {
        Map<String, Object> opts = new LinkedHashMap<>();
        String targetId = optString(params, "targetId");
        if (targetId != null) opts.put("targetId", targetId);
        if (profile != null) opts.put("profile", profile);

        BrowserTypes.SnapshotResult snap = client.snapshot(opts);

        // snapshot 文本优先作为主要内容返回给 LLM（减少 token 消耗）
        if (snap.getSnapshot() != null) {
            Output output = Output.builder()
                    .success(true)
                    .text(snap.getSnapshot())
                    .rawData(toJson(snap))
                    .build();
            return ToolResult.ok(output);
        }
        return jsonOutput(snap);
    }

    private ToolResult<Output> handleScreenshot(JsonNode params, String profile) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        String targetId = optString(params, "targetId");
        if (targetId != null) body.put("targetId", targetId);
        boolean fullPage = params.has("fullPage") && params.get("fullPage").asBoolean();
        if (fullPage) body.put("fullPage", true);
        String ref = optString(params, "ref");
        String element = optString(params, "element");
        if (ref != null) body.put("ref", ref);
        if (element != null) body.put("element", element);

        if (fullPage && (ref != null || element != null)) {
            return ToolResult.fail("fullPage is not supported for element screenshots");
        }

        BrowserTypes.ScreenshotResult shot = client.screenshot(body, profile);

        // 截图以多模态（base64 图片 + 文字描述）返回，供视觉模型查看
        if (shot.getData() != null && !shot.getData().isEmpty()) {
            String mimeType = shot.getContentType() != null ? shot.getContentType() : "image/png";
            String caption = "Screenshot captured"
                    + (shot.getUrl() != null ? " of " + shot.getUrl() : "");
            Output output = Output.builder()
                    .success(true)
                    .text(caption)
                    .imageBase64(shot.getData())
                    .imageMediaType(mimeType)
                    .rawData(toJson(shot))
                    .build();
            return ToolResult.ok(output);
        }
        return jsonOutput(shot);
    }

    private ToolResult<Output> handleAct(JsonNode params, String profile) throws Exception {
        JsonNode requestNode = params.get("request");
        if (requestNode == null || !requestNode.isObject()) {
            return ToolResult.fail("request is required and must be an object");
        }
        Map<String, Object> request = MAPPER.convertValue(requestNode,
                new TypeReference<LinkedHashMap<String, Object>>() {});
        if (!request.containsKey("kind")) {
            return ToolResult.fail("request.kind is required");
        }
        JsonNode result = client.act(request, profile);
        return jsonOutput(result);
    }

    // ── 输出类型 ──────────────────────────────────────────────────────────

    /**
     * BrowserTool 输出封装。
     *
     * <p>普通结果：{@link #text} 存放 JSON 序列化结果。
     * 截图结果：{@link #imageBase64} + {@link #imageMediaType} 携带图片数据，
     * {@link #text} 存放配字说明（"Screenshot captured of ..."）。
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class Output {
        private boolean success;
        private String error;
        /** 主要文本内容（snapshot 文本或结果 JSON 字符串） */
        private String text;
        /** base64 图片数据（截图时填充） */
        private String imageBase64;
        /** 图片 MIME 类型，如 "image/png"（截图时填充） */
        private String imageMediaType;
        /** 原始响应数据（JSON 字符串，供日志/存储使用） */
        private String rawData;
    }

    // ── 辅助方法 ──────────────────────────────────────────────────────────

    /**
     * 将对象序列化为 JSON 字符串，包装为文本输出。
     */
    private ToolResult<Output> jsonOutput(Object data) {
        String json = toJson(data);
        Output output = Output.builder()
                .success(true)
                .text(json)
                .rawData(json)
                .build();
        return ToolResult.ok(output);
    }

    private String toJson(Object data) {
        try {
            return MAPPER.writeValueAsString(data);
        } catch (Exception e) {
            return data != null ? data.toString() : "";
        }
    }

    private static String requireString(JsonNode params, String field) {
        JsonNode node = params == null ? null : params.get(field);
        if (node == null || node.isNull() || node.asText().isBlank()) {
            throw new IllegalArgumentException("'" + field + "' is required");
        }
        return node.asText().trim();
    }

    private static String optString(JsonNode params, String field) {
        if (params == null) return null;
        JsonNode node = params.get(field);
        if (node == null || node.isNull()) return null;
        String val = node.asText().trim();
        return val.isEmpty() ? null : val;
    }

    private static boolean optBoolean(JsonNode params, String field, boolean defaultValue) {
        if (params == null) return defaultValue;
        JsonNode node = params.get(field);
        if (node == null || node.isNull()) return defaultValue;
        return node.asBoolean(defaultValue);
    }

}
