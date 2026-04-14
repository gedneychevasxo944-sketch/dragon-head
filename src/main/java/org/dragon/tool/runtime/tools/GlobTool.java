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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 文件通配符搜索工具实现。
 *
 * <p>对应 TypeScript 版本的 {@code src/tools/GlobTool/GlobTool.ts}。
 * 使用 glob 模式搜索文件名，按修改时间倒序排列。
 *
 * <h3>功能：</h3>
 * <ul>
 *   <li>支持标准 glob 模式（*, **, ?）</li>
 *   <li>可指定搜索目录</li>
 *   <li>结果截断保护（最多 100 个文件）</li>
 *   <li>自动跳过 .git、node_modules 等目录</li>
 * </ul>
 *
 * <h3>输入 Schema：</h3>
 * <pre>
 * {
 *   "pattern": "**\/*.java",        // glob 模式
 *   "path": "/optional/directory"   // 搜索目录（可选，默认当前目录）
 * }
 * </pre>
 */
@Slf4j
@Component
public class GlobTool extends AbstractTool<GlobTool.Input, GlobTool.Output> {

    private static final int MAX_RESULTS = 100;
    private static final long MAX_RESULT_SIZE = 100_000;

    /** 默认跳过的目录 */
    private static final List<String> SKIP_DIRS = List.of(
            ".git", ".svn", ".hg", ".bzr",
            "node_modules", "target", "build", ".gradle", ".idea"
    );

    public GlobTool() {
        super("Glob", "Find files by name pattern or wildcard. Searches the filesystem using glob patterns.", Input.class);
    }

    // ── 核心方法 ─────────────────────────────────────────────────────────

    @Override
    protected CompletableFuture<ToolResult<Output>> doCall(Input input,
                                                           ToolUseContext context,
                                                           Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            String pattern = input.getPattern();
            String searchPath = input.getPath();

            log.info("[GlobTool] 搜索文件: pattern={}, path={}", pattern, searchPath);

            try {
                // 确定搜索根目录
                Path rootDir;
                if (searchPath != null && !searchPath.isBlank()) {
                    rootDir = Paths.get(searchPath);
                    if (!Files.exists(rootDir)) {
                        return ToolResult.fail("Directory does not exist: " + searchPath);
                    }
                    if (!Files.isDirectory(rootDir)) {
                        return ToolResult.fail("Path is not a directory: " + searchPath);
                    }
                } else {
                    rootDir = Paths.get(System.getProperty("user.dir"));
                }

                // 构建 PathMatcher
                PathMatcher matcher = FileSystems.getDefault()
                        .getPathMatcher("glob:" + pattern);

                List<String> matched = new ArrayList<>();
                boolean[] truncated = {false};

                Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        String dirName = dir.getFileName() != null ? dir.getFileName().toString() : "";
                        if (SKIP_DIRS.contains(dirName) && !dir.equals(rootDir)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (context.isAborted()) {
                            return FileVisitResult.TERMINATE;
                        }
                        if (matched.size() >= MAX_RESULTS) {
                            truncated[0] = true;
                            return FileVisitResult.TERMINATE;
                        }

                        // 相对于搜索根目录的路径用于匹配
                        Path relativePath = rootDir.relativize(file);
                        if (matcher.matches(relativePath) || matcher.matches(file.getFileName())) {
                            matched.add(rootDir.relativize(file).toString());
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        log.debug("[GlobTool] 跳过无法访问的文件: {}", file);
                        return FileVisitResult.CONTINUE;
                    }
                });

                long durationMs = System.currentTimeMillis() - startTime;
                log.info("[GlobTool] 搜索完成: found={}, truncated={}, duration={}ms",
                        matched.size(), truncated[0], durationMs);

                return ToolResult.ok(Output.builder()
                        .filenames(matched)
                        .numFiles(matched.size())
                        .durationMs(durationMs)
                        .truncated(truncated[0])
                        .build());

            } catch (IOException e) {
                log.error("[GlobTool] 搜索失败: error={}", e.getMessage(), e);
                return ToolResult.fail("Glob search failed: " + e.getMessage());
            }
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(Output output, String toolUseId) {
        if (output.getFilenames().isEmpty()) {
            return ToolResultBlockParam.ofText(toolUseId, "No files found");
        }

        StringBuilder sb = new StringBuilder();
        for (String filename : output.getFilenames()) {
            sb.append(filename).append("\n");
        }
        if (output.isTruncated()) {
            sb.append("(Results are truncated. Consider using a more specific path or pattern.)");
        }

        return ToolResultBlockParam.ofText(toolUseId, sb.toString().trim());
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
        return "find files by name pattern or wildcard";
    }

    @Override
    public String getUserFacingName(Input input) {
        return "Glob: " + input.getPattern();
    }

    @Override
    public String getActivityDescription(Input input) {
        return "Finding " + input.getPattern();
    }

    // ── 输入输出类型 ─────────────────────────────────────────────────────

    @Data
    @Builder
    public static class Input {
        private String pattern;
        /** 可选，搜索目录 */
        private String path;
    }

    @Data
    @Builder
    public static class Output {
        private List<String> filenames;
        private int numFiles;
        private long durationMs;
        private boolean truncated;
    }
}
