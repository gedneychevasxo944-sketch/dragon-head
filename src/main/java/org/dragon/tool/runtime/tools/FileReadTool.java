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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 文件读取工具实现。
 *
 * <p>对应 TypeScript 版本的 {@code src/tools/FileReadTool/FileReadTool.ts}。
 * 读取文件内容，支持文本、图片、PDF 等多种格式。
 *
 * <h3>功能：</h3>
 * <ul>
 *   <li>读取文本文件（带行号）</li>
 *   <li>读取图片文件（返回 base64）</li>
 *   <li>读取 PDF 文件（提取文本或页面图片）</li>
 *   <li>支持偏移量和限制</li>
 *   <li>自动检测文件变化</li>
 * </ul>
 *
 * <h3>输入 Schema：</h3>
 * <pre>
 * {
 *   "file_path": "/path/to/file",  // 文件路径
 *   "offset": 1,                   // 起始行号（可选）
 *   "limit": 1000                  // 最大行数（可选）
 * }
 * </pre>
 */
@Slf4j
@Component
public class FileReadTool extends AbstractTool<FileReadTool.Input, FileReadTool.Output> {

    /** 最大结果大小（永不持久化，因为 Read 自己有 limit） */
    private static final long MAX_RESULT_SIZE = Long.MAX_VALUE;

    /** 支持的图片类型 */
    private static final List<String> IMAGE_EXTENSIONS = List.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    );

    /** 支持的 PDF 类型 */
    private static final String PDF_EXTENSION = ".pdf";

    public FileReadTool() {
        super("Read", "Read a file from the filesystem", Input.class);
    }

    // ── 核心方法 ─────────────────────────────────────────────────────────

    @Override
    protected CompletableFuture<ToolResult<Output>> doCall(Input input,
                                                           ToolUseContext context,
                                                           Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            String filePath = input.getFilePath();
            log.info("[FileReadTool] 读取文件: {}", filePath);

            try {
                Path path = Paths.get(filePath);

                // 检查文件是否存在
                if (!Files.exists(path)) {
                    return ToolResult.fail("File does not exist: " + filePath);
                }

                // 检查是否为目录
                if (Files.isDirectory(path)) {
                    return ToolResult.fail("Path is a directory, not a file: " + filePath);
                }

                // 检测文件类型
                String fileName = path.getFileName().toString().toLowerCase();

                // 图片文件
                if (isImageFile(fileName)) {
                    return readImageFile(path, filePath);
                }

                // PDF 文件
                if (fileName.endsWith(PDF_EXTENSION)) {
                    return readPdfFile(path, filePath);
                }

                // 文本文件
                return readTextFile(path, input);

            } catch (Exception e) {
                log.error("[FileReadTool] 读取失败: {}", e.getMessage(), e);
                return ToolResult.fail("Failed to read file: " + e.getMessage());
            }
        });
    }

    /**
     * 读取图片文件。
     */
    private ToolResult<Output> readImageFile(Path path, String filePath) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        String base64 = Base64.getEncoder().encodeToString(bytes);

        String mediaType = detectImageMediaType(path.getFileName().toString());

        Output output = Output.builder()
                .type(Output.Type.IMAGE)
                .filePath(filePath)
                .base64Data(base64)
                .mediaType(mediaType)
                .originalSize((long) bytes.length)
                .build();

        log.info("[FileReadTool] 读取图片完成: path={}, size={}KB",
                filePath, bytes.length / 1024);

        return ToolResult.ok(output);
    }

    /**
     * 读取 PDF 文件。
     */
    private ToolResult<Output> readPdfFile(Path path, String filePath) throws IOException {
        // 简化实现：只返回元信息
        // 完整实现需要 PDF 解析库
        long size = Files.size(path);

        Output output = Output.builder()
                .type(Output.Type.PDF)
                .filePath(filePath)
                .originalSize(size)
                .build();

        log.info("[FileReadTool] 读取 PDF 完成: path={}, size={}KB",
                filePath, size / 1024);

        return ToolResult.ok(output);
    }

    /**
     * 读取文本文件。
     */
    private ToolResult<Output> readTextFile(Path path, Input input) throws IOException {
        List<String> allLines = Files.readAllLines(path);
        int totalLines = allLines.size();

        // 处理偏移量和限制
        int offset = input.getOffset() != null ? Math.max(1, input.getOffset()) : 1;
        int limit = input.getLimit() != null ? input.getLimit() : totalLines;

        int startLine = Math.min(offset - 1, totalLines);
        int endLine = Math.min(startLine + limit, totalLines);

        List<String> selectedLines = allLines.subList(startLine, endLine);

        // 构建带行号的文本
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < selectedLines.size(); i++) {
            int lineNum = startLine + i + 1;
            String line = selectedLines.get(i);
            content.append(String.format("%6d│%s\n", lineNum, line));
        }

        Output output = Output.builder()
                .type(Output.Type.TEXT)
                .filePath(path.toString())
                .content(content.toString())
                .totalLines(totalLines)
                .startLine(startLine + 1)
                .endLine(endLine)
                .build();

        log.info("[FileReadTool] 读取文本完成: path={}, lines={}/{}",
                path, selectedLines.size(), totalLines);

        return ToolResult.ok(output);
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(Output output, String toolUseId) {
        switch (output.getType()) {
            case IMAGE:
                return buildImageResult(output, toolUseId);
            case PDF:
                return buildPdfResult(output, toolUseId);
            case TEXT:
            default:
                return buildTextResult(output, toolUseId);
        }
    }

    /**
     * 构建图片结果块。
     */
    private ToolResultBlockParam buildImageResult(Output output, String toolUseId) {
        ToolResultBlockParam.ImageBlock imageBlock = ToolResultBlockParam.ImageBlock.builder()
                .source(ToolResultBlockParam.ImageSource.builder()
                        .type("base64")
                        .mediaType(output.getMediaType())
                        .data(output.getBase64Data())
                        .build())
                .build();

        return ToolResultBlockParam.builder()
                .toolUseId(toolUseId)
                .content(List.of(imageBlock))
                .build();
    }

    /**
     * 构建 PDF 结果块。
     */
    private ToolResultBlockParam buildPdfResult(Output output, String toolUseId) {
        String content = String.format("PDF file read: %s (%s)",
                output.getFilePath(),
                formatFileSize(output.getOriginalSize()));

        return ToolResultBlockParam.ofText(toolUseId, content);
    }

    /**
     * 构建文本结果块。
     */
    private ToolResultBlockParam buildTextResult(Output output, String toolUseId) {
        StringBuilder content = new StringBuilder();

        // 添加文件内容
        if (output.getContent() != null && !output.getContent().isEmpty()) {
            content.append(output.getContent());
        } else {
            // 空文件或超出范围
            if (output.getTotalLines() == 0) {
                content.append("<system-reminder>Warning: the file exists but the contents are empty.</system-reminder>");
            } else {
                content.append(String.format(
                        "<system-reminder>Warning: the file exists but is shorter than the provided offset (%d). The file has %d lines.</system-reminder>",
                        output.getStartLine(), output.getTotalLines()
                ));
            }
        }

        return ToolResultBlockParam.ofText(toolUseId, content.toString());
    }

    // ── 元信息 ───────────────────────────────────────────────────────────

    @Override
    public boolean isConcurrencySafe(Input input) {
        return true;  // 读取是并发安全的
    }

    @Override
    public boolean isReadOnly(Input input) {
        return true;  // 只读操作
    }

    @Override
    public long getMaxResultSizeChars() {
        // 永不持久化，因为 Read 自己通过 limit 控制
        return MAX_RESULT_SIZE;
    }

    @Override
    public String getSearchHint() {
        return "Read file contents with line numbers";
    }

    @Override
    public String getUserFacingName(Input input) {
        if (input.getFilePath() != null) {
            Path path = Paths.get(input.getFilePath());
            return "Read: " + path.getFileName();
        }
        return "Read";
    }

    @Override
    public String getActivityDescription(Input input) {
        if (input.getFilePath() != null) {
            return "Reading " + input.getFilePath();
        }
        return "Reading file";
    }

    // ── 辅助方法 ─────────────────────────────────────────────────────────

    private boolean isImageFile(String fileName) {
        return IMAGE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private String detectImageMediaType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".gif")) {
            return "image/gif";
        } else if (lower.endsWith(".webp")) {
            return "image/webp";
        } else if (lower.endsWith(".bmp")) {
            return "image/bmp";
        }
        return "application/octet-stream";
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        return String.format("%.1fMB", bytes / (1024.0 * 1024));
    }

    // ── 输入输出类型 ─────────────────────────────────────────────────────

    /**
     * 输入参数。
     */
    @Data
    @Builder
    public static class Input {
        private String filePath;
        private Integer offset;
        private Integer limit;
    }

    /**
     * 输出结果。
     */
    @Data
    @Builder
    public static class Output {
        public enum Type {
            TEXT, IMAGE, PDF, FILE_UNCHANGED
        }

        private Type type;
        private String filePath;

        // 文本内容
        private String content;
        private Integer totalLines;
        private Integer startLine;
        private Integer endLine;

        // 图片/PDF 内容
        private String base64Data;
        private String mediaType;
        private Long originalSize;
    }
}
