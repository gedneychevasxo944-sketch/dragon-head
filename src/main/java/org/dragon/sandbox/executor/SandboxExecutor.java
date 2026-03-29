package org.dragon.sandbox.executor;

import org.dragon.sandbox.domain.*;
import org.dragon.sandbox.manager.SandboxManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Sandbox 命令执行器。
 *
 * 职责：
 * 在指定 workspace 的 sandbox 环境中执行命令，
 * 捕获输出并返回结构化结果。
 *
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SandboxExecutor {

    private final SandboxManager sandboxManager;

    /**
     * 在指定 workspace 的 sandbox 中执行命令。
     *
     * @param request 执行请求
     * @return 执行结果
     */
    public ExecutionResult execute(ExecutionRequest request) {
        long startTime = System.currentTimeMillis();
        String executionId = request.getExecutionId();

        log.info("开始执行命令: executionId={}, workspaceId={}, command={}",
                executionId, request.getWorkspaceId(), request.getCommand());

        // 1. 获取 sandbox
        Sandbox sandbox = sandboxManager.getOrCreate(request.getWorkspaceId());

        // 2. 创建本次执行的临时工作目录
        Path execWorkDir = sandbox.getTmpDir().resolve("exec-" + executionId);
        try {
            Files.createDirectories(execWorkDir);
        } catch (IOException e) {
            return buildFailureResult(executionId, "创建执行目录失败: " + e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }

        try {
            // 3. 解析命令（替换路径占位符）
            String resolvedCommand = resolveCommandPaths(request.getCommand(), sandbox);

            // 4. 合并环境变量
            Map<String, String> mergedEnv = buildMergedEnv(sandbox, request.getExtraEnv());

            // 5. 确定工作目录
            Path workingDir = request.getWorkingDir() != null
                    ? sandbox.getRootDir().resolve(request.getWorkingDir())
                    : execWorkDir;

            // 6. 执行命令
            return executeCommand(
                    executionId,
                    resolvedCommand,
                    workingDir,
                    mergedEnv,
                    request.getTimeoutSeconds(),
                    startTime
            );

        } finally {
            // 7. 清理临时目录（异步，不阻塞返回）
            cleanupAsync(execWorkDir);
        }
    }

    /**
     * 实际执行命令的核心方法。
     */
    private ExecutionResult executeCommand(String executionId,
                                            String command,
                                            Path workingDir,
                                            Map<String, String> env,
                                            int timeoutSeconds,
                                            long startTime) {
        // 使用 bash -c 执行命令，支持管道、重定向等 shell 特性
        List<String> cmdList = List.of("/bin/bash", "-c", command);

        ProcessBuilder pb = new ProcessBuilder(cmdList);
        pb.directory(workingDir.toFile());
        pb.environment().clear();
        pb.environment().putAll(env);
        pb.redirectErrorStream(false);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            return buildFailureResult(executionId, "命令启动失败: " + e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }

        // 异步读取 stdout 和 stderr（防止缓冲区满导致死锁）
        ExecutorService ioExecutor = Executors.newFixedThreadPool(2);
        Future<String> stdoutFuture = ioExecutor.submit(
                () -> readStream(process.getInputStream()));
        Future<String> stderrFuture = ioExecutor.submit(
                () -> readStream(process.getErrorStream()));

        try {
            // 等待进程结束（带超时）
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return buildFailureResult(executionId,
                        "命令执行超时（超过 " + timeoutSeconds + " 秒）",
                        System.currentTimeMillis() - startTime);
            }

            int exitCode = process.exitValue();
            String stdout = getQuietly(stdoutFuture);
            String stderr = getQuietly(stderrFuture);
            long durationMs = System.currentTimeMillis() - startTime;

            log.info("命令执行完成: executionId={}, exitCode={}, durationMs={}",
                    executionId, exitCode, durationMs);

            return ExecutionResult.builder()
                    .executionId(executionId)
                    .success(exitCode == 0)
                    .stdout(stdout)
                    .stderr(stderr)
                    .exitCode(exitCode)
                    .durationMs(durationMs)
                    .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return buildFailureResult(executionId, "执行被中断",
                    System.currentTimeMillis() - startTime);
        } finally {
            ioExecutor.shutdownNow();
        }
    }

    /**
     * 解析命令中的路径占位符。
     */
    private String resolveCommandPaths(String command, Sandbox sandbox) {
        return command
                .replace("{SKILLS_DIR}", sandbox.getSkillsDir().toAbsolutePath().toString())
                .replace("{WORKSPACE_DIR}", sandbox.getWorkspaceDir().toAbsolutePath().toString())
                .replace("{SANDBOX_ROOT}", sandbox.getRootDir().toAbsolutePath().toString());
    }

    /**
     * 合并环境变量。
     */
    private Map<String, String> buildMergedEnv(Sandbox sandbox, Map<String, String> extraEnv) {
        Map<String, String> merged = new LinkedHashMap<>(sandbox.getEnvironmentVariables());
        if (extraEnv != null) {
            merged.putAll(extraEnv);
        }
        return merged;
    }

    private String readStream(InputStream is) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        } catch (IOException e) {
            return "";
        }
    }

    private String getQuietly(Future<String> future) {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "";
        }
    }

    private ExecutionResult buildFailureResult(String executionId, String reason, long durationMs) {
        log.error("命令执行失败: executionId={}, reason={}", executionId, reason);
        return ExecutionResult.builder()
                .executionId(executionId)
                .success(false)
                .exitCode(-1)
                .failureReason(reason)
                .durationMs(durationMs)
                .build();
    }

    private void cleanupAsync(Path dir) {
        CompletableFuture.runAsync(() -> {
            try {
                if (Files.exists(dir)) {
                    Files.walk(dir)
                            .sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try { Files.delete(p); } catch (IOException ignored) {}
                            });
                }
            } catch (IOException e) {
                log.warn("临时目录清理失败: {}", dir);
            }
        });
    }
}