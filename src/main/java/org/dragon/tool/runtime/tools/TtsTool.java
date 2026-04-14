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
 * TTS 工具 — 将文本转换为语音并返回媒体路径。
 *
 * <p>对应 TypeScript 的 {@code tools/tts-tool.ts}。
 *
 * <h3>输入 Schema：</h3>
 * <pre>
 * {
 *   "text": "Hello world",   // 要转换的文本
 *   "channel": "telegram"   // 可选，频道 id，用于选择输出格式
 * }
 * </pre>
 */
@Slf4j
@Component
public class TtsTool extends AbstractTool<TtsTool.Input, TtsTool.Output> {

    public TtsTool() {
        super("tts", "Convert text to speech and return a MEDIA: path. " +
                "Use when the user requests audio or TTS is enabled.", Input.class);
    }

    // ── 核心方法 ─────────────────────────────────────────────────────────

    @Override
    protected CompletableFuture<ToolResult<Output>> doCall(Input input,
                                                            ToolUseContext context,
                                                            Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            String text = input.getText();
            if (text == null || text.isBlank()) {
                return ToolResult.fail("'text' is required");
            }

            String channel = input.getChannel();
            log.info("[TtsTool] textLen={} channel={}", text.length(), channel);

            // TODO: 接入 TTS 服务提供商
            Output output = Output.builder()
                    .text(text.length() > 100 ? text.substring(0, 100) + "..." : text)
                    .textLength(text.length())
                    .channel(channel)
                    .status("pending")
                    .note("TTS stub — wire up TTS provider")
                    .build();

            return ToolResult.ok(output);
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(Output output, String toolUseId) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("text", output.getText());
        result.put("textLength", output.getTextLength());
        if (output.getChannel() != null) {
            result.put("channel", output.getChannel());
        }
        result.put("status", output.getStatus());
        result.put("note", output.getNote());
        return ToolResultBlockParam.ofText(toolUseId, result.toString());
    }

    // ── 元信息 ───────────────────────────────────────────────────────────

    @Override
    public boolean isReadOnly(Input input) {
        return false;
    }

    @Override
    public boolean isConcurrencySafe(Input input) {
        return true;
    }

    @Override
    public String getActivityDescription(Input input) {
        return "Converting text to speech";
    }

    // ── 输入输出类型 ─────────────────────────────────────────────────────

    /**
     * 输入参数。
     */
    @Data
    @Builder
    public static class Input {
        /** 要转换为语音的文本。 */
        private String text;
        /** 频道 id，用于选择输出格式（可选）。 */
        private String channel;
    }

    /**
     * 输出结果。
     */
    @Data
    @Builder
    public static class Output {
        private String text;
        private int textLength;
        private String channel;
        private String status;
        private String note;
        /** TTS 生成的媒体路径（MEDIA: 前缀），TODO 接入后填充。 */
        private String mediaPath;
    }
}
