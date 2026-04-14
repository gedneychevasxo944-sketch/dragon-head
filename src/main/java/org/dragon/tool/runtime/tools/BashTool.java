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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Bash 工具实现。
 *
 * <p>对应 TypeScript 版本的 {@code src/tools/BashTool/BashTool.tsx}。
 * 执行 shell 命令并返回输出。
 *
 * <h3>功能：</h3>
 * <ul>
 *   <li>执行任意 shell 命令</li>
 *   <li>支持超时设置</li>
 *   <li>支持后台运行</li>
 *   <li>输出截断和持久化</li>
 * </ul>
 *
 * <h3>输入 Schema：</h3>
 * <pre>
 * {
 *   "command": "ls -la",       // 要执行的命令
 *   "timeout": 120000,         // 超时时间（毫秒）
 *   "description": "List files", // 命令描述
 *   "run_in_background": false  // 是否后台运行
 * }
 * </pre>
 */
@Slf4j
@Component
public class BashTool extends AbstractTool<BashTool.Input, BashTool.Output> {

    /** 默认超时时间（毫秒） */
    private static final long DEFAULT_TIMEOUT_MS = 120_000;

    /** 最大输出长度 */
    private static final long MAX_OUTPUT_CHARS = 100_000;

    /** 后台任务 ID 前缀 */
    private static final String BACKGROUND_TASK_PREFIX = "bg_";

    public BashTool() {
        super("Bash", "Execute a bash command in the terminal", Input.class);
    }

    // ── 核心方法 ─────────────────────────────────────────────────────────

    @Override
    protected CompletableFuture<ToolResult<Output>> doCall(Input input,
                                                           ToolUseContext context,
                                                           Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            String command = input.getCommand();

            log.info("[BashTool] 执行命令: {}", command);

            try {
                // 后台运行模式
                if (input.isRunInBackground()) {
                    return executeBackground(input, context);
                }

                // 同步执行
                ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);
                pb.redirectErrorStream(false);

                Process process = pb.start();

                // 读取输出
                StringBuilder stdout = new StringBuilder();
                StringBuilder stderr = new StringBuilder();

                Thread stdoutThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stdout.append(line).append("\n");
                            // 报告进度
                            if (progress != null) {
                                progress.accept(ToolProgress.bash(
                                        context.getToolUseId(),
                                        command,
                                        stdout.toString(),
                                        System.currentTimeMillis() - startTime
                                ));
                            }
                        }
                    } catch (IOException e) {
                        log.error("[BashTool] 读取 stdout 失败", e);
                    }
                });

                Thread stderrThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getErrorStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stderr.append(line).append("\n");
                        }
                    } catch (IOException e) {
                        log.error("[BashTool] 读取 stderr 失败", e);
                    }
                });

                stdoutThread.start();
                stderrThread.start();

                // 等待完成或超时
                long timeout = input.getTimeout() != null ? input.getTimeout() : DEFAULT_TIMEOUT_MS;
                boolean finished = process.waitFor(timeout, TimeUnit.MILLISECONDS);

                if (!finished) {
                    process.destroyForcibly();
                    return ToolResult.fail("Command timed out after " + timeout + "ms");
                }

                stdoutThread.join(1000);
                stderrThread.join(1000);

                long duration = System.currentTimeMillis() - startTime;

                // 检查是否被中断
                boolean interrupted = context.isAborted();

                Output output = Output.builder()
                        .command(command)
                        .stdout(truncateOutput(stdout.toString()))
                        .stderr(stderr.toString().trim())
                        .exitCode(process.exitValue())
                        .durationMs(duration)
                        .interrupted(interrupted)
                        .build();

                log.info("[BashTool] 命令完成: exitCode={}, duration={}ms, stdoutLen={}",
                        output.getExitCode(), duration, output.getStdout().length());

                return ToolResult.ok(output);

            } catch (Exception e) {
                log.error("[BashTool] 执行失败: {}", e.getMessage(), e);
                return ToolResult.fail("Command execution failed: " + e.getMessage());
            }
        });
    }

    /**
     * 后台执行命令。
     */
    private ToolResult<Output> executeBackground(Input input, ToolUseContext context) {
        String backgroundTaskId = BACKGROUND_TASK_PREFIX + System.currentTimeMillis();

        // 创建后台线程执行
        Thread bgThread = new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", input.getCommand());
                Process process = pb.start();
                process.waitFor();
                log.info("[BashTool] 后台任务完成: taskId={}", backgroundTaskId);
            } catch (Exception e) {
                log.error("[BashTool] 后台任务失败: taskId={}, error={}",
                        backgroundTaskId, e.getMessage());
            }
        });

        bgThread.setDaemon(true);
        bgThread.start();

        Output output = Output.builder()
                .command(input.getCommand())
                .stdout("")
                .stderr("")
                .backgroundTaskId(backgroundTaskId)
                .backgroundedByUser(true)
                .build();

        return ToolResult.ok(output);
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(Output output, String toolUseId) {
        // 处理 stdout
        String stdout = output.getStdout();
        if (stdout != null) {
            // 移除开头空白行
            stdout = stdout.replaceFirst("^(\\s*\n)+", "");
            stdout = stdout.stripTrailing();
        }

        // 构建内容
        StringBuilder content = new StringBuilder();
        if (stdout != null && !stdout.isEmpty()) {
            content.append(stdout);
        }

        // 添加错误信息
        if (output.getStderr() != null && !output.getStderr().isEmpty()) {
            if (content.length() > 0) content.append("\n");
            content.append(output.getStderr());
        }

        // 添加中断信息
        if (output.isInterrupted()) {
            if (content.length() > 0) content.append("\n");
            content.append("<error>Command was aborted before completion</error>");
        }

        // 添加后台任务信息
        if (output.getBackgroundTaskId() != null) {
            if (content.length() > 0) content.append("\n");
            content.append(String.format(
                    "Command running in background with ID: %s",
                    output.getBackgroundTaskId()
            ));
        }

        return ToolResultBlockParam.builder()
                .toolUseId(toolUseId)
                .content(content.toString())
                .isError(output.isInterrupted() || (output.getExitCode() != null && output.getExitCode() != 0))
                .build();
    }

    // ── 元信息 ───────────────────────────────────────────────────────────

    @Override
    public boolean isConcurrencySafe(Input input) {
        // Bash 命令通常不是并发安全的
        return false;
    }

    @Override
    public boolean isReadOnly(Input input) {
        // 通过命令内容判断是否只读
        String cmd = input.getCommand().toLowerCase().trim();
        return !cmd.contains("rm ") &&
               !cmd.contains("mv ") &&
               !cmd.contains("cp ") &&
               !cmd.contains(">") &&
               !cmd.contains(">>") &&
               !cmd.startsWith("git push") &&
               !cmd.startsWith("git commit");
    }

    @Override
    public boolean isDestructive(Input input) {
        String cmd = input.getCommand().toLowerCase().trim();
        return cmd.contains("rm ") ||
               cmd.contains("rmdir") ||
               cmd.contains("dd ") ||
               cmd.contains("mkfs") ||
               cmd.contains("format");
    }

    @Override
    public long getMaxResultSizeChars() {
        return MAX_OUTPUT_CHARS;
    }

    @Override
    public String getUserFacingName(Input input) {
        if (input.getDescription() != null) {
            return input.getDescription();
        }
        // 提取命令名
        String cmd = input.getCommand().trim().split("\\s+")[0];
        return "Bash: " + cmd;
    }

    @Override
    public String getActivityDescription(Input input) {
        String cmd = input.getCommand().trim().split("\\s+")[0];
        return "Running " + cmd;
    }

    // ── 辅助方法 ─────────────────────────────────────────────────────────

    /**
     * 截断输出。
     */
    private String truncateOutput(String output) {
        if (output == null) return "";
        if (output.length() <= MAX_OUTPUT_CHARS) {
            return output;
        }
        // 保留开头和结尾
        int halfLen = (int) (MAX_OUTPUT_CHARS / 2);
        return output.substring(0, halfLen) +
               "\n... [output truncated] ...\n" +
               output.substring(output.length() - halfLen);
    }

    // ── 输入输出类型 ─────────────────────────────────────────────────────

    /**
     * 输入参数。
     */
    @Data
    @Builder
    public static class Input {
        private String command;
        private Long timeout;
        private String description;
        private boolean runInBackground;
    }

    /**
     * 输出结果。
     */
    @Data
    @Builder
    public static class Output {
        private String command;
        private String stdout;
        private String stderr;
        private Integer exitCode;
        private long durationMs;
        private boolean interrupted;
        private String backgroundTaskId;
        private boolean backgroundedByUser;
        private boolean assistantAutoBackgrounded;
    }
}
