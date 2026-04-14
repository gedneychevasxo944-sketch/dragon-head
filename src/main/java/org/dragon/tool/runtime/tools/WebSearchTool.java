package org.dragon.tool.runtime.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 网络搜索工具实现。
 *
 * <p>对应 TypeScript 版本中通过 MCP 或 API Key 方式调用的 WebSearch 能力。
 * 通过 DuckDuckGo 的 HTML 搜索页面提取结果（无需 API Key 的公开实现）。
 *
 * <h3>功能：</h3>
 * <ul>
 *   <li>执行网络搜索，返回 title + url + snippet 列表</li>
 *   <li>优先使用环境变量 SEARCH_API_URL 指向的自定义搜索 API</li>
 *   <li>降级为 DuckDuckGo HTML 解析</li>
 * </ul>
 *
 * <h3>输入 Schema：</h3>
 * <pre>
 * {
 *   "query": "search terms here",
 *   "num_results": 5
 * }
 * </pre>
 */
@Slf4j
@Component
public class WebSearchTool extends AbstractTool<WebSearchTool.Input, WebSearchTool.Output> {

    private static final long MAX_RESULT_SIZE = 50_000;
    private static final int DEFAULT_NUM_RESULTS = 5;
    private static final int MAX_NUM_RESULTS = 20;
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    private final ObjectMapper jsonMapper = new ObjectMapper();

    public WebSearchTool() {
        super("WebSearch", "Search the web for information. Returns a list of search results with titles, URLs, and snippets.", Input.class);
    }

    // ── 核心方法 ─────────────────────────────────────────────────────────

    @Override
    protected CompletableFuture<ToolResult<Output>> doCall(Input input,
                                                           ToolUseContext context,
                                                           Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            String query = input.getQuery();
            int numResults = input.getNumResults() != null
                    ? Math.min(input.getNumResults(), MAX_NUM_RESULTS)
                    : DEFAULT_NUM_RESULTS;
            long startTime = System.currentTimeMillis();

            log.info("[WebSearchTool] 搜索: query={}, numResults={}", query, numResults);

            // 优先使用自定义搜索 API（如 SerpAPI / Bing / 内部搜索）
            String customApiUrl = System.getenv("SEARCH_API_URL");
            try {
                List<SearchResult> results;
                if (customApiUrl != null && !customApiUrl.isBlank()) {
                    results = searchViaCustomApi(customApiUrl, query, numResults);
                } else {
                    results = searchViaDuckDuckGo(query, numResults);
                }

                long durationMs = System.currentTimeMillis() - startTime;
                log.info("[WebSearchTool] 搜索完成: results={}, duration={}ms",
                        results.size(), durationMs);

                return ToolResult.ok(Output.builder()
                        .query(query)
                        .results(results)
                        .durationMs(durationMs)
                        .build());

            } catch (Exception e) {
                log.error("[WebSearchTool] 搜索失败: query={}, error={}", query, e.getMessage(), e);
                return ToolResult.fail("Search failed: " + e.getMessage());
            }
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(Output output, String toolUseId) {
        if (output.getResults() == null || output.getResults().isEmpty()) {
            return ToolResultBlockParam.ofText(toolUseId, "No search results found for: " + output.getQuery());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Search results for \"").append(output.getQuery()).append("\":\n\n");

        int idx = 1;
        for (SearchResult r : output.getResults()) {
            sb.append(idx++).append(". **").append(r.getTitle()).append("**\n");
            sb.append("   URL: ").append(r.getUrl()).append("\n");
            if (r.getSnippet() != null && !r.getSnippet().isBlank()) {
                sb.append("   ").append(r.getSnippet()).append("\n");
            }
            sb.append("\n");
        }

        return ToolResultBlockParam.ofText(toolUseId, sb.toString().trim());
    }

    @Override
    public ValidationResult validateInput(Input input, ToolUseContext context) {
        if (input.getQuery() == null || input.getQuery().isBlank()) {
            return ValidationResult.fail("Search query is required");
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
        return "search the web for information";
    }

    @Override
    public String getUserFacingName(Input input) {
        return "WebSearch";
    }

    @Override
    public String getActivityDescription(Input input) {
        return "Searching for " + input.getQuery();
    }

    // ── 搜索实现 ─────────────────────────────────────────────────────────

    /**
     * 通过自定义搜索 API（JSON 响应）执行搜索。
     * 期望 API 返回格式：{ "results": [ { "title": "...", "url": "...", "snippet": "..." } ] }
     */
    private List<SearchResult> searchViaCustomApi(String apiUrl, String query, int numResults)
            throws IOException, URISyntaxException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String fullUrl = apiUrl + "?q=" + encodedQuery + "&num=" + numResults;

        URI uri = new URI(fullUrl);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            throw new IOException("Search API returned HTTP " + conn.getResponseCode());
        }

        byte[] responseBytes = conn.getInputStream().readAllBytes();
        JsonNode root = jsonMapper.readTree(responseBytes);

        List<SearchResult> results = new ArrayList<>();
        JsonNode resultsNode = root.path("results");
        if (resultsNode.isArray()) {
            for (JsonNode item : resultsNode) {
                results.add(SearchResult.builder()
                        .title(item.path("title").asText(""))
                        .url(item.path("url").asText(""))
                        .snippet(item.path("snippet").asText(""))
                        .build());
                if (results.size() >= numResults) break;
            }
        }
        return results;
    }

    /**
     * 通过 DuckDuckGo HTML 解析搜索结果（降级方案）。
     * 注意：此方案依赖 DDG 的 HTML 结构，可能因页面更新而失效。
     */
    private List<SearchResult> searchViaDuckDuckGo(String query, int numResults)
            throws IOException, URISyntaxException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        // DuckDuckGo HTML 搜索接口
        String searchUrl = "https://html.duckduckgo.com/html/?q=" + encodedQuery;

        URI uri = new URI(searchUrl);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (compatible; ClawAgent/1.0)");

        InputStream is = conn.getInputStream();
        String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        return parseDdgHtml(html, numResults);
    }

    /**
     * 简单解析 DuckDuckGo HTML 结果。
     */
    private List<SearchResult> parseDdgHtml(String html, int numResults) {
        List<SearchResult> results = new ArrayList<>();

        // 查找结果块（class="result__body"）
        java.util.regex.Pattern titlePattern = java.util.regex.Pattern.compile(
                "<a[^>]+class=\"result__a\"[^>]*href=\"([^\"]+)\"[^>]*>([^<]+)</a>");
        java.util.regex.Pattern snippetPattern = java.util.regex.Pattern.compile(
                "<a[^>]+class=\"result__snippet\"[^>]*>([^<]+(?:<[^>]+>[^<]*</[^>]+>)*[^<]*)</a>");

        java.util.regex.Matcher titleMatcher = titlePattern.matcher(html);
        java.util.regex.Matcher snippetMatcher = snippetPattern.matcher(html);

        List<String> titles = new ArrayList<>();
        List<String> urls = new ArrayList<>();
        while (titleMatcher.find() && titles.size() < numResults) {
            urls.add(titleMatcher.group(1));
            titles.add(titleMatcher.group(2).trim());
        }

        List<String> snippets = new ArrayList<>();
        while (snippetMatcher.find()) {
            String snippet = snippetMatcher.group(1)
                    .replaceAll("<[^>]+>", "")
                    .trim();
            snippets.add(snippet);
        }

        for (int i = 0; i < Math.min(titles.size(), numResults); i++) {
            results.add(SearchResult.builder()
                    .title(titles.get(i))
                    .url(urls.get(i))
                    .snippet(i < snippets.size() ? snippets.get(i) : "")
                    .build());
        }

        return results;
    }

    // ── 输入输出类型 ─────────────────────────────────────────────────────

    @Data
    @Builder
    public static class SearchResult {
        private String title;
        private String url;
        private String snippet;
    }

    @Data
    @Builder
    public static class Input {
        private String query;
        private Integer numResults;
    }

    @Data
    @Builder
    public static class Output {
        private String query;
        private List<SearchResult> results;
        private long durationMs;
    }
}
