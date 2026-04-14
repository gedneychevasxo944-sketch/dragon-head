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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 文件写入工具实现。
 *
 * <p>对应 TypeScript 版本的 {@code src/tools/FileWriteTool/FileWriteTool.ts}。
 * 写入或覆盖文件内容，自动创建父目录。
 *
 * <h3>功能：</h3>
 * <ul>
 *   <li>创建新文件</li>
 *   <li>覆盖已有文件</li>
 *   <li>自动创建父目录</li>
 *   <li>区分 create 和 update 操作</li>
 * </ul>
 *
 * <h3>输入 Schema：</h3>
 * <pre>
 * {
 *   "file_path": "/absolute/path/to/file",  // 文件绝对路径
 *   "content": "file content here"           // 写入内容
 * }
 * </pre>
 */
@Slf4j
@Component
public class FileWriteTool extends AbstractTool<FileWriteTool.Input, FileWriteTool.Output> {

    private static final long MAX_RESULT_SIZE = 100_000;

    public FileWriteTool() {
        super("Write", "Write a file to the local filesystem. Creates a new file or overwrites an existing one.", Input.class);
    }

    // ── 核心方法 ─────────────────────────────────────────────────────────

    @Override
    protected CompletableFuture<ToolResult<Output>> doCall(Input input,
                                                           ToolUseContext context,
                                                           Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            String filePath = input.getFilePath();
            String content = input.getContent();

            log.info("[FileWriteTool] 写入文件: {}", filePath);

            try {
                Path path = Paths.get(filePath);

                // 记录操作类型（create / update）
                boolean fileExists = Files.exists(path);
                String originalContent = null;
                if (fileExists) {
                    originalContent = Files.readString(path, StandardCharsets.UTF_8);
                }

                // 确保父目录存在
                Path parentDir = path.getParent();
                if (parentDir != null && !Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                    log.debug("[FileWriteTool] 创建父目录: {}", parentDir);
                }

                // 写入文件
                Files.writeString(path, content, StandardCharsets.UTF_8);

                String type = fileExists ? "update" : "create";
                log.info("[FileWriteTool] 文件写入完成: path={}, type={}", filePath, type);

                Output output = Output.builder()
                        .type(type)
                        .filePath(filePath)
                        .content(content)
                        .originalFile(originalContent)
                        .build();

                return ToolResult.ok(output);

            } catch (IOException e) {
                log.error("[FileWriteTool] 写入失败: path={}, error={}", filePath, e.getMessage(), e);
                return ToolResult.fail("Failed to write file: " + e.getMessage());
            }
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(Output output, String toolUseId) {
        String message = "update".equals(output.getType())
                ? "The file " + output.getFilePath() + " has been updated successfully."
                : "File created successfully at: " + output.getFilePath();

        return ToolResultBlockParam.ofText(toolUseId, message);
    }

    // ── 元信息 ───────────────────────────────────────────────────────────

    @Override
    public boolean isReadOnly(Input input) {
        return false;
    }

    @Override
    public boolean isConcurrencySafe(Input input) {
        return false;
    }

    @Override
    public boolean isDestructive(Input input) {
        // 覆盖写操作可能是破坏性的（覆盖已有文件）
        return true;
    }

    @Override
    public long getMaxResultSizeChars() {
        return MAX_RESULT_SIZE;
    }

    @Override
    public String getSearchHint() {
        return "create or overwrite files";
    }

    @Override
    public String getUserFacingName(Input input) {
        if (input.getFilePath() != null) {
            return "Write: " + Paths.get(input.getFilePath()).getFileName();
        }
        return "Write";
    }

    @Override
    public String getActivityDescription(Input input) {
        if (input.getFilePath() != null) {
            return "Writing " + input.getFilePath();
        }
        return "Writing file";
    }

    // ── 输入输出类型 ─────────────────────────────────────────────────────

    @Data
    @Builder
    public static class Input {
        private String filePath;
        private String content;
    }

    @Data
    @Builder
    public static class Output {
        /** "create" or "update" */
        private String type;
        private String filePath;
        private String content;
        /** 原文件内容，新建时为 null */
        private String originalFile;
    }
}
