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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Telegram 专用工具，允许 Agent 直接向当前聊天会话发送图像。
 *
 * <p>接受以下来源的图像：
 * <ul>
 *   <li>{@code filePath} — 图像的本地文件路径</li>
 *   <li>{@code base64} — 原始 base64 编码的图像数据</li>
 *   <li>{@code url} — 公共图像 URL</li>
 * </ul>
 *
 * <p>chatId 在运行时从 {@link ToolUseContext#getSessionId()} 派生，
 * 对于 Telegram 频道，sessionId 格式为 {@code tg:CHAT_ID} 或 {@code tg:CHAT_ID:THREAD_ID}。
 */
@Slf4j
@Component
public class TelegramImageTool extends AbstractTool<TelegramImageTool.Input, TelegramImageTool.Output> {

    /**
     * 用于实际图像发送逻辑的函数式接口，在连接时注入以与频道模块解耦。
     */
    @FunctionalInterface
    public interface ImageSender {
        /**
         * 向 Telegram 聊天发送图像。
         *
         * @param chatId     目标聊天 ID
         * @param imageBytes 图像内容字节
         * @param fileName   建议的文件名（如 "photo.png"）
         * @param caption    可选的标题文本
         */
        void sendImage(String chatId, byte[] imageBytes, String fileName, String caption);
    }

    /** 全局图像发送器 — 在启动时由 TelegramAgentWiring 设置。 */
    private static volatile ImageSender imageSender;

    /**
     * 设置图像发送器实现（在连接时调用一次）。
     *
     * @param sender 图像发送器实现
     */
    public static void setImageSender(ImageSender sender) {
        imageSender = sender;
    }

    public TelegramImageTool() {
        super("send_image",
                "Send an image to the current Telegram chat. " +
                "Provide image via: filePath (local file), base64 (raw image data), or url (public URL). " +
                "Optionally include a caption. " +
                "Use this tool when you need to show images to the user in the chat.",
                Input.class);
    }

    // ── 核心方法 ─────────────────────────────────────────────────────────

    @Override
    protected CompletableFuture<ToolResult<Output>> doCall(Input input,
                                                            ToolUseContext context,
                                                            Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            if (imageSender == null) {
                return ToolResult.fail("Image sending is not configured. " +
                        "This tool is only available when running via Telegram channel.");
            }

            // 从 sessionId 派生 chatId（格式: "tg:CHAT_ID" 或 "tg:CHAT_ID:THREAD_ID"）
            String chatId = context.getSessionId();
            if (chatId == null || chatId.isBlank()) {
                return ToolResult.fail("No target chat ID available.");
            }
            if (chatId.startsWith("tg:")) {
                String[] parts = chatId.split(":");
                chatId = parts[1];
            }

            try {
                String filePath = input.getFilePath();
                String base64Data = input.getBase64();
                String imageUrl = input.getUrl();
                String caption = input.getCaption();
                String format = input.getFormat();
                if (format == null || format.isBlank()) {
                    format = "png";
                }

                byte[] imageBytes;
                String fileName;

                if (filePath != null && !filePath.isBlank()) {
                    // --- 本地文件 ---
                    Path path = Path.of(filePath);
                    if (!Files.exists(path) || !Files.isRegularFile(path)) {
                        return ToolResult.fail("File not found: " + filePath);
                    }
                    long size = Files.size(path);
                    if (size > 10_000_000) {
                        return ToolResult.fail("File too large (max 10MB): " + size + " bytes");
                    }
                    imageBytes = Files.readAllBytes(path);
                    fileName = path.getFileName().toString();

                } else if (base64Data != null && !base64Data.isBlank()) {
                    // --- Base64 数据 ---
                    if (base64Data.contains(",")) {
                        base64Data = base64Data.substring(base64Data.indexOf(',') + 1);
                    }
                    imageBytes = Base64.getDecoder().decode(base64Data.replaceAll("\\s+", ""));
                    fileName = "image." + format;

                } else if (imageUrl != null && !imageUrl.isBlank()) {
                    // --- URL：下载后转发 ---
                    HttpClient httpClient = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(10))
                            .build();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(imageUrl))
                            .timeout(Duration.ofSeconds(30))
                            .GET()
                            .build();
                    HttpResponse<byte[]> response = httpClient.send(request,
                            HttpResponse.BodyHandlers.ofByteArray());

                    if (response.statusCode() != 200) {
                        return ToolResult.fail("Failed to download image: HTTP " + response.statusCode());
                    }
                    imageBytes = response.body();
                    String urlPath = URI.create(imageUrl).getPath();
                    fileName = urlPath.contains("/")
                            ? urlPath.substring(urlPath.lastIndexOf('/') + 1)
                            : "image." + format;
                    if (!fileName.contains(".")) {
                        fileName += "." + format;
                    }

                } else {
                    return ToolResult.fail("Provide one of: filePath, base64, or url");
                }

                log.info("[TelegramImageTool] chatId={} fileName={} size={}",
                        chatId, fileName, imageBytes.length);
                imageSender.sendImage(chatId, imageBytes, fileName, caption);

                Output output = Output.builder()
                        .ok(true)
                        .chatId(chatId)
                        .fileName(fileName)
                        .size(imageBytes.length)
                        .build();

                return ToolResult.ok(output);

            } catch (Exception e) {
                log.error("[TelegramImageTool] 发送图像失败: {}", e.getMessage(), e);
                return ToolResult.fail("Failed to send image: " + e.getMessage());
            }
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(Output output, String toolUseId) {
        String message = output.isOk()
                ? "Image sent successfully to chat " + output.getChatId()
                : "Failed to send image";
        return ToolResultBlockParam.ofText(toolUseId, message, !output.isOk());
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
        return "Sending image to Telegram";
    }

    // ── 输入输出类型 ─────────────────────────────────────────────────────

    /**
     * 输入参数。
     */
    @Data
    @Builder
    public static class Input {
        /** 本地图像文件路径（png/jpg/gif/webp）。 */
        private String filePath;
        /** Base64 编码的图像数据（不含 data URI 前缀）。 */
        private String base64;
        /** 图像格式，使用 base64 时有效（默认 png）。 */
        private String format;
        /** 公共图像 URL。 */
        private String url;
        /** 可选的图像标题文本。 */
        private String caption;
    }

    /**
     * 输出结果。
     */
    @Data
    @Builder
    public static class Output {
        private boolean ok;
        private String chatId;
        private String fileName;
        private long size;
    }
}
