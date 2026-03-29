package org.dragon.sandbox.domain;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 工具执行请求。
 * 由 Agent 在处理 LLM 输出时构建，提交给 SandboxExecutor 执行。
 *
 * @since 1.0
 */
@Data
@Builder
public class ExecutionRequest {

    /** 本次执行的唯一 ID */
    private String executionId;

    /** 归属的 workspace ID */
    private Long workspaceId;

    /**
     * 要执行的命令。
     * 例："bash scripts/run.sh --city Beijing"
     * 注意：命令中的 Skill 路径使用相对路径，
     * executor 会自动解析为 sandbox 内的绝对路径。
     */
    private String command;

    /**
     * 本次执行额外附加的环境变量（优先级高于 sandbox 级别变量）。
     * 通常由 Agent 根据当前对话上下文动态注入。
     */
    private Map<String, String> extraEnv;

    /**
     * 执行超时时间（秒），默认 30 秒。
     */
    @Builder.Default
    private Integer timeoutSeconds = 30;

    /**
     * 执行的工作目录（相对于 sandbox 根目录）。
     * 默认为 tmp/{executionId}/
     */
    private String workingDir;
}