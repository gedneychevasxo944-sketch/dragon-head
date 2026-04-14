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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 文件内容正则搜索工具实现。
 *
 * <p>对应 TypeScript 版本的 {@code src/tools/GrepTool/GrepTool.ts}。
 * 使用正则表达式搜索文件内容，支持多种输出模式。
 *
 * <h3>功能：</h3>
 * <ul>
 *   <li>支持 content（显示匹配行内容）、files_with_matches（显示匹配文件）、count（显示匹配数量）三种模式</li>
 *   <li>支持大小写不敏感搜索</li>
 *   <li>支持 glob 过滤文件类型</li>
 *   <li>支持前后上下文行（context, before, after）</li>
 *   <li>支持 head_limit 和 offset 分页</li>
 *   <li>优先使用系统 ripgrep（rg）加速搜索</li>
 * </ul>
 *
 * <h3>输入 Schema：</h3>
 * <pre>
 * {
 *   "pattern": "regex",                    // 正则模式
 *   "path": "/optional/search/path",        // 搜索路径（可选）
 *   "glob": "*.java",                       // 文件类型过滤（可选）
 *   "output_mode": "files_with_matches",    // 输出模式
 *   "case_insensitive": false,              // 大小写不敏感（可选）
 *   "context_lines": 0,                     // 前后上下文行数（可选）
 *   "head_limit": 250                        // 结果限制数（可选）
 * }
 * </pre>
 */
@Slf4j
@Component
public class GrepTool extends AbstractTool<GrepTool.Input, GrepTool.Output> {

    private static final int DEFAULT_HEAD_LIMIT = 250;
    private static final long MAX_RESULT_SIZE = 20_000;

    /** 默认跳过的目录 */
    private static final List<String> SKIP_DIRS = List.of(
            ".git", ".svn", ".hg", ".bzr", ".jj", ".sl"
    );

    public GrepTool() {
        super("Search", "Search file contents with regular expressions. Returns matching files or content.", Input.class);
    }

    // ── 核心方法 ─────────────────────────────────────────────────────────

    @Override
    protected CompletableFuture<ToolResult<Output>> doCall(Input input,
                                                           ToolUseContext context,
                                                           Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            String pattern = input.getPattern();
            String searchPath = input.getPath();
            String outputMode = input.getOutputMode() != null ? input.getOutputMode() : "files_with_matches";
            boolean caseInsensitive = Boolean.TRUE.equals(input.getCaseInsensitive());
            int headLimit = input.getHeadLimit() != null ? input.getHeadLimit() : DEFAULT_HEAD_LIMIT;
            int offset = input.getOffset() != null ? input.getOffset() : 0;
            int contextLines = input.getContextLines() != null ? input.getContextLines() : 0;

            log.info("[GrepTool] 搜索: pattern={}, path={}, mode={}", pattern, searchPath, outputMode);

            try {
                // 优先尝试调用系统 rg
                if (isRipgrepAvailable()) {
                    return executeWithRipgrep(input, headLimit, offset, context);
                }
                // 降级为 Java 实现
                return executeWithJava(pattern, searchPath, outputMode,
                        caseInsensitive, headLimit, offset, contextLines, input.getGlob(), context);

            } catch (Exception e) {
                log.error("[GrepTool] 搜索失败: error={}", e.getMessage(), e);
                return ToolResult.fail("Search failed: " + e.getMessage());
            }
        });
    }

    /**
     * 调用系统 ripgrep 执行搜索。
     */
    private ToolResult<Output> executeWithRipgrep(Input input, int headLimit, int offset,
                                                   ToolUseContext context) throws Exception {
        List<String> args = new ArrayList<>();
        args.add("rg");
        args.add("--hidden");

        // 排除 VCS 目录
        for (String dir : SKIP_DIRS) {
            args.add("--glob");
            args.add("!" + dir);
        }

        args.add("--max-columns");
        args.add("500");

        String outputMode = input.getOutputMode() != null ? input.getOutputMode() : "files_with_matches";

        if (Boolean.TRUE.equals(input.getCaseInsensitive())) {
            args.add("-i");
        }

        if ("files_with_matches".equals(outputMode)) {
            args.add("-l");
        } else if ("count".equals(outputMode)) {
            args.add("-c");
        } else {
            // content mode
            args.add("-n");
        }

        if (input.getContextLines() != null && input.getContextLines() > 0) {
            args.add("-C");
            args.add(String.valueOf(input.getContextLines()));
        }

        if (input.getGlob() != null && !input.getGlob().isBlank()) {
            args.add("--glob");
            args.add(input.getGlob());
        }

        // 添加 pattern
        args.add(input.getPattern());

        // 添加搜索路径
        String searchPath = input.getPath() != null ? input.getPath() : System.getProperty("user.dir");
        args.add(searchPath);

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String output = new String(process.getInputStream().readAllBytes());
        process.waitFor();

        String[] lines = output.split("\n");
        List<String> results = new ArrayList<>();
        for (String line : lines) {
            if (!line.isBlank()) results.add(line.trim());
        }

        return buildOutput(results, outputMode, headLimit, offset);
    }

    /**
     * 纯 Java 实现搜索（ripgrep 不可用时降级）。
     */
    private ToolResult<Output> executeWithJava(String pattern, String searchPath, String outputMode,
                                                boolean caseInsensitive, int headLimit, int offset,
                                                int contextLines, String globFilter,
                                                ToolUseContext context) throws IOException {
        int flags = caseInsensitive ? Pattern.CASE_INSENSITIVE : 0;

        Pattern regex;
        try {
            regex = Pattern.compile(pattern, flags);
        } catch (PatternSyntaxException e) {
            return ToolResult.fail("Invalid regex pattern: " + e.getMessage());
        }

        Path rootDir = searchPath != null ? Paths.get(searchPath) : Paths.get(System.getProperty("user.dir"));
        PathMatcher globMatcher = globFilter != null
                ? FileSystems.getDefault().getPathMatcher("glob:" + globFilter)
                : null;

        List<String> matched = new ArrayList<>();

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
                if (context.isAborted()) return FileVisitResult.TERMINATE;

                // glob 过滤
                if (globMatcher != null && !globMatcher.matches(file.getFileName())) {
                    return FileVisitResult.CONTINUE;
                }

                try {
                    List<String> fileLines = Files.readAllLines(file);
                    boolean hasMatch = false;
                    for (int i = 0; i < fileLines.size(); i++) {
                        String line = fileLines.get(i);
                        Matcher m = regex.matcher(line);
                        if (m.find()) {
                            hasMatch = true;
                            if ("content".equals(outputMode)) {
                                String relPath = rootDir.relativize(file).toString();
                                matched.add(relPath + ":" + (i + 1) + ":" + line);
                            } else if ("count".equals(outputMode)) {
                                // 统计模式放到文件级别处理
                                break;
                            }
                        }
                    }

                    if ("files_with_matches".equals(outputMode) && hasMatch) {
                        matched.add(rootDir.relativize(file).toString());
                    } else if ("count".equals(outputMode) && hasMatch) {
                        // 统计整个文件的匹配数
                        long count = fileLines.stream()
                                .mapToLong(l -> {
                                    Matcher mc = regex.matcher(l);
                                    long c = 0;
                                    while (mc.find()) c++;
                                    return c;
                                }).sum();
                        matched.add(rootDir.relativize(file) + ":" + count);
                    }

                } catch (IOException e) {
                    log.debug("[GrepTool] 跳过无法读取的文件: {}", file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });

        return buildOutput(matched, outputMode, headLimit, offset);
    }

    /**
     * 构建输出结果，应用 head_limit / offset。
     */
    private ToolResult<Output> buildOutput(List<String> results, String outputMode,
                                           int headLimit, int offset) {
        // 应用 offset
        List<String> paginated = results.subList(Math.min(offset, results.size()), results.size());

        // 应用 head_limit (0 = 不限制)
        boolean truncated = false;
        Integer appliedLimit = null;
        if (headLimit > 0 && paginated.size() > headLimit) {
            paginated = paginated.subList(0, headLimit);
            truncated = true;
            appliedLimit = headLimit;
        }

        if ("content".equals(outputMode)) {
            return ToolResult.ok(Output.builder()
                    .mode("content")
                    .numFiles(0)
                    .filenames(List.of())
                    .content(String.join("\n", paginated))
                    .numLines(paginated.size())
                    .appliedLimit(appliedLimit)
                    .appliedOffset(offset > 0 ? offset : null)
                    .build());
        }

        if ("count".equals(outputMode)) {
            int totalMatches = paginated.stream().mapToInt(l -> {
                int idx = l.lastIndexOf(':');
                if (idx >= 0) {
                    try {
                        return Integer.parseInt(l.substring(idx + 1));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
                return 0;
            }).sum();

            return ToolResult.ok(Output.builder()
                    .mode("count")
                    .numFiles(paginated.size())
                    .filenames(List.of())
                    .content(String.join("\n", paginated))
                    .numMatches(totalMatches)
                    .appliedLimit(appliedLimit)
                    .appliedOffset(offset > 0 ? offset : null)
                    .build());
        }

        // files_with_matches
        return ToolResult.ok(Output.builder()
                .mode("files_with_matches")
                .filenames(paginated)
                .numFiles(paginated.size())
                .appliedLimit(appliedLimit)
                .appliedOffset(offset > 0 ? offset : null)
                .build());
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(Output output, String toolUseId) {
        String mode = output.getMode() != null ? output.getMode() : "files_with_matches";

        if ("content".equals(mode)) {
            String content = output.getContent() != null ? output.getContent() : "No matches found";
            if (output.getAppliedLimit() != null) {
                content += "\n\n[Results limited to " + output.getAppliedLimit() + " lines]";
            }
            return ToolResultBlockParam.ofText(toolUseId, content);
        }

        if ("count".equals(mode)) {
            String content = output.getContent() != null ? output.getContent() : "No matches found";
            int matches = output.getNumMatches() != null ? output.getNumMatches() : 0;
            int files = output.getNumFiles();
            content += String.format("\n\nFound %d total occurrences across %d files.", matches, files);
            return ToolResultBlockParam.ofText(toolUseId, content);
        }

        // files_with_matches
        if (output.getFilenames().isEmpty()) {
            return ToolResultBlockParam.ofText(toolUseId, "No files found");
        }
        String result = String.format("Found %d files\n%s",
                output.getNumFiles(),
                String.join("\n", output.getFilenames()));
        return ToolResultBlockParam.ofText(toolUseId, result);
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
        return "search file contents with regex (ripgrep)";
    }

    @Override
    public String getUserFacingName(Input input) {
        return "Search";
    }

    @Override
    public String getActivityDescription(Input input) {
        return "Searching for " + input.getPattern();
    }

    // ── 辅助方法 ─────────────────────────────────────────────────────────

    private boolean isRipgrepAvailable() {
        try {
            Process p = new ProcessBuilder("rg", "--version").start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ── 输入输出类型 ─────────────────────────────────────────────────────

    @Data
    @Builder
    public static class Input {
        private String pattern;
        private String path;
        private String glob;
        private String outputMode;
        private Boolean caseInsensitive;
        private Integer contextLines;
        private Integer headLimit;
        private Integer offset;
    }

    @Data
    @Builder
    public static class Output {
        private String mode;
        private int numFiles;
        private List<String> filenames;
        private String content;
        private Integer numLines;
        private Integer numMatches;
        private Integer appliedLimit;
        private Integer appliedOffset;
    }
}
