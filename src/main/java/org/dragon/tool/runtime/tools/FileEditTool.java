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
 * 文件编辑工具实现。
 *
 * <p>对应 TypeScript 版本的 {@code src/tools/FileEditTool/FileEditTool.ts}。
 * 通过字符串替换精确编辑文件内容，支持全量替换和单次替换。
 *
 * <h3>功能：</h3>
 * <ul>
 *   <li>通过 old_string/new_string 进行精确字符串替换</li>
 *   <li>replace_all=true 时替换所有匹配</li>
 *   <li>replace_all=false 时仅替换唯一匹配（多个匹配时报错）</li>
 *   <li>old_string 为空时表示创建新文件或向空文件写入内容</li>
 * </ul>
 *
 * <h3>输入 Schema：</h3>
 * <pre>
 * {
 *   "file_path": "/absolute/path/to/file",  // 文件绝对路径
 *   "old_string": "text to replace",         // 要替换的原文本（空串表示新建/覆盖空文件）
 *   "new_string": "replacement text",         // 替换后的文本
 *   "replace_all": false                       // 是否替换全部匹配（默认 false）
 * }
 * </pre>
 */
@Slf4j
@Component
public class FileEditTool extends AbstractTool<FileEditTool.Input, FileEditTool.Output> {

    private static final long MAX_RESULT_SIZE = 100_000;

    public FileEditTool() {
        super("Edit", "A tool for editing files. Replaces a specific string in the file with a new string.", Input.class);
    }

    // ── 核心方法 ─────────────────────────────────────────────────────────

    @Override
    protected CompletableFuture<ToolResult<Output>> doCall(Input input,
                                                           ToolUseContext context,
                                                           Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            String filePath = input.getFilePath();
            String oldString = input.getOldString() != null ? input.getOldString() : "";
            String newString = input.getNewString() != null ? input.getNewString() : "";
            boolean replaceAll = Boolean.TRUE.equals(input.getReplaceAll());

            log.info("[FileEditTool] 编辑文件: path={}, replaceAll={}", filePath, replaceAll);

            try {
                Path path = Paths.get(filePath);

                // old_string 为空且文件不存在 → 新建文件
                if (oldString.isEmpty()) {
                    if (!Files.exists(path)) {
                        Path parentDir = path.getParent();
                        if (parentDir != null && !Files.exists(parentDir)) {
                            Files.createDirectories(parentDir);
                        }
                        Files.writeString(path, newString, StandardCharsets.UTF_8);
                        log.info("[FileEditTool] 新建文件: {}", filePath);
                        return ToolResult.ok(Output.builder()
                                .filePath(filePath)
                                .oldString(oldString)
                                .newString(newString)
                                .originalFile("")
                                .updatedFile(newString)
                                .replaceAll(replaceAll)
                                .build());
                    } else {
                        // 文件存在但 old_string 为空：仅当文件也为空时允许
                        String existing = Files.readString(path, StandardCharsets.UTF_8);
                        if (!existing.trim().isEmpty()) {
                            return ToolResult.fail("Cannot create new file - file already exists.");
                        }
                        Files.writeString(path, newString, StandardCharsets.UTF_8);
                        return ToolResult.ok(Output.builder()
                                .filePath(filePath)
                                .oldString(oldString)
                                .newString(newString)
                                .originalFile(existing)
                                .updatedFile(newString)
                                .replaceAll(replaceAll)
                                .build());
                    }
                }

                // 文件必须存在
                if (!Files.exists(path)) {
                    return ToolResult.fail("File does not exist: " + filePath);
                }

                String originalContent = Files.readString(path, StandardCharsets.UTF_8);

                // 检查 old_string 是否存在
                if (!originalContent.contains(oldString)) {
                    return ToolResult.fail("String to replace not found in file.\nString: " + oldString);
                }

                // 检查多处匹配
                int matchCount = countOccurrences(originalContent, oldString);
                if (matchCount > 1 && !replaceAll) {
                    return ToolResult.fail(String.format(
                            "Found %d matches of the string to replace, but replace_all is false. " +
                            "To replace all occurrences, set replace_all to true. " +
                            "To replace only one occurrence, please provide more context to uniquely identify the instance.\nString: %s",
                            matchCount, oldString));
                }

                // 执行替换
                String updatedContent = replaceAll
                        ? originalContent.replace(oldString, newString)
                        : originalContent.replaceFirst(java.util.regex.Pattern.quote(oldString), 
                                                       java.util.regex.Matcher.quoteReplacement(newString));

                // 确保父目录存在
                Path parentDir = path.getParent();
                if (parentDir != null && !Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                }

                Files.writeString(path, updatedContent, StandardCharsets.UTF_8);

                log.info("[FileEditTool] 编辑完成: path={}, matches={}", filePath, matchCount);

                return ToolResult.ok(Output.builder()
                        .filePath(filePath)
                        .oldString(oldString)
                        .newString(newString)
                        .originalFile(originalContent)
                        .updatedFile(updatedContent)
                        .replaceAll(replaceAll)
                        .build());

            } catch (IOException e) {
                log.error("[FileEditTool] 编辑失败: path={}, error={}", filePath, e.getMessage(), e);
                return ToolResult.fail("Failed to edit file: " + e.getMessage());
            }
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(Output output, String toolUseId) {
        String message = output.isReplaceAll()
                ? "The file " + output.getFilePath() + " has been updated. All occurrences were successfully replaced."
                : "The file " + output.getFilePath() + " has been updated successfully.";
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
    public long getMaxResultSizeChars() {
        return MAX_RESULT_SIZE;
    }

    @Override
    public String getSearchHint() {
        return "modify file contents in place";
    }

    @Override
    public String getUserFacingName(Input input) {
        if (input.getFilePath() != null) {
            return "Edit: " + Paths.get(input.getFilePath()).getFileName();
        }
        return "Edit";
    }

    @Override
    public String getActivityDescription(Input input) {
        if (input.getFilePath() != null) {
            return "Editing " + input.getFilePath();
        }
        return "Editing file";
    }

    // ── 辅助方法 ─────────────────────────────────────────────────────────

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }

    // ── 输入输出类型 ─────────────────────────────────────────────────────

    @Data
    @Builder
    public static class Input {
        private String filePath;
        private String oldString;
        private String newString;
        private Boolean replaceAll;
    }

    @Data
    @Builder
    public static class Output {
        private String filePath;
        private String oldString;
        private String newString;
        private String originalFile;
        private String updatedFile;
        private boolean replaceAll;
    }
}
