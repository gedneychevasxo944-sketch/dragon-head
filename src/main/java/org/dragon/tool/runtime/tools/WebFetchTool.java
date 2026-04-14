package org.dragon.tool.runtime.tools;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dragon.tool.runtime.AbstractTool;
import org.dragon.tool.runtime.ToolProgress;
import org.dragon.tool.runtime.ToolResult;
import org.dragon.tool.runtime.ToolResultBlockParam;
import org.dragon.tool.runtime.ToolUseContext;
import org.dragon.tool.runtime.ValidationResult;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * 网页内容抓取工具实现。
 *
 * <p>对应 TypeScript 版本的 {@code src/tools/WebFetchTool/WebFetchTool.ts}。
 * 获取 URL 的页面内容并将其转为 Markdown 格式返回给 LLM。
 *
 * <h3>功能：</h3>
 * <ul>
 *   <li>发送 HTTP GET 请求获取 URL 内容</li>
 *   <li>将 HTML 简单转为纯文本（去除标签）</li>
 *   <li>结果截断到 maxResultSizeChars</li>
 *   <li>记录响应码、大小、耗时</li>
 * </ul>
 *
 * <h3>输入 Schema：</h3>
 * <pre>
 * {
 *   "url": "https://example.com",
 *   "prompt": "What is this page about?"
 * }
 * </pre>
 */
@Slf4j
@Component
public class WebFetchTool extends AbstractTool<WebFetchTool.Input, WebFetchTool.Output> {

    private static final long MAX_RESULT_SIZE = 100_000;
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int MAX_CONTENT_BYTES = 2 * 1024 * 1024; // 2MB

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern MULTIPLE_NEWLINES = Pattern.compile("\n{3,}");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("[ \t]+");

    public WebFetchTool() {
        super("WebFetch", "Fetch and extract content from a URL. Returns the page content as text.", Input.class);
    }

    // ── 核心方法 ─────────────────────────────────────────────────────────

    @Override
    protected CompletableFuture<ToolResult<Output>> doCall(Input input,
                                                           ToolUseContext context,
                                                           Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            String url = input.getUrl();
            String prompt = input.getPrompt() != null ? input.getPrompt() : "";
            long startTime = System.currentTimeMillis();

            log.info("[WebFetchTool] 获取 URL: {}", url);

            try {
                URI uri = new URI(url);
                HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (compatible; ClawAgent/1.0; +https://example.com)");
                conn.setInstanceFollowRedirects(true);

                int code = conn.getResponseCode();
                String codeText = conn.getResponseMessage() != null ? conn.getResponseMessage() : "";
                String contentType = conn.getContentType() != null ? conn.getContentType() : "";

                InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
                byte[] bytes = is != null ? readLimited(is, MAX_CONTENT_BYTES) : new byte[0];

                String rawContent = new String(bytes, StandardCharsets.UTF_8);
                String processedContent = processContent(rawContent, contentType);

                // 截断
                if (processedContent.length() > MAX_RESULT_SIZE) {
                    processedContent = processedContent.substring(0, (int) MAX_RESULT_SIZE) +
                            "\n...[Content truncated]";
                }

                long durationMs = System.currentTimeMillis() - startTime;
                log.info("[WebFetchTool] 完成: url={}, code={}, bytes={}, duration={}ms",
                        url, code, bytes.length, durationMs);

                return ToolResult.ok(Output.builder()
                        .url(url)
                        .code(code)
                        .codeText(codeText)
                        .bytes(bytes.length)
                        .result(processedContent)
                        .durationMs(durationMs)
                        .build());

            } catch (URISyntaxException e) {
                return ToolResult.fail("Invalid URL: " + url + " - " + e.getMessage());
            } catch (IOException e) {
                log.error("[WebFetchTool] 请求失败: url={}, error={}", url, e.getMessage());
                return ToolResult.fail("Failed to fetch URL: " + e.getMessage());
            }
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(Output output, String toolUseId) {
        return ToolResultBlockParam.ofText(toolUseId, output.getResult());
    }

    @Override
    public ValidationResult validateInput(Input input, ToolUseContext context) {
        if (input.getUrl() == null || input.getUrl().isBlank()) {
            return ValidationResult.fail("URL is required");
        }
        try {
            new URI(input.getUrl()).toURL();
        } catch (Exception e) {
            return ValidationResult.fail("Invalid URL \"" + input.getUrl() + "\": " + e.getMessage());
        }
        return ValidationResult.ok();
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
        return "fetch and extract content from a URL";
    }

    @Override
    public String getUserFacingName(Input input) {
        return "Fetch";
    }

    @Override
    public String getActivityDescription(Input input) {
        if (input.getUrl() != null) {
            try {
                String host = new URI(input.getUrl()).getHost();
                return "Fetching " + host;
            } catch (Exception e) {
                // ignore
            }
        }
        return "Fetching web page";
    }

    // ── 辅助方法 ─────────────────────────────────────────────────────────

    /**
     * 读取输入流，最多读取 maxBytes 字节。
     */
    private byte[] readLimited(InputStream is, int maxBytes) throws IOException {
        byte[] buffer = new byte[maxBytes];
        int totalRead = 0;
        int read;
        while (totalRead < maxBytes && (read = is.read(buffer, totalRead, maxBytes - totalRead)) != -1) {
            totalRead += read;
        }
        byte[] result = new byte[totalRead];
        System.arraycopy(buffer, 0, result, 0, totalRead);
        return result;
    }

    /**
     * 简单的 HTML → 纯文本转换。
     * 对于真实场景，建议使用 jsoup 等专业库。
     */
    private String processContent(String content, String contentType) {
        if (content == null || content.isBlank()) return "";

        // 如果不是 HTML，直接返回
        if (contentType != null && !contentType.contains("html")) {
            return content;
        }

        // 移除 script/style 标签及其内容
        String text = content
                .replaceAll("(?is)<script[^>]*>.*?</script>", "")
                .replaceAll("(?is)<style[^>]*>.*?</style>", "");

        // 转换常见标签为换行
        text = text
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(p|div|h[1-6]|li|tr)>", "\n")
                .replaceAll("(?i)<(h[1-6])[^>]*>", "\n## ");

        // 移除所有剩余 HTML 标签
        text = HTML_TAG_PATTERN.matcher(text).replaceAll("");

        // HTML 实体解码（基础版）
        text = text
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ");

        // 清理多余空白
        text = MULTIPLE_SPACES.matcher(text).replaceAll(" ");
        text = MULTIPLE_NEWLINES.matcher(text).replaceAll("\n\n");

        return text.trim();
    }

    // ── 输入输出类型 ─────────────────────────────────────────────────────

    @Data
    @Builder
    public static class Input {
        private String url;
        private String prompt;
    }

    @Data
    @Builder
    public static class Output {
        private String url;
        private int code;
        private String codeText;
        private int bytes;
        private String result;
        private long durationMs;
    }
}
