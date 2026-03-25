package org.dragon.sandbox.domain;

import lombok.Builder;
import lombok.Data;

/**
 * 工具执行结果。
 * 返回给 Agent，由 Agent 将结果传递给 LLM。
 *
 * @since 1.0
 */
@Data
@Builder
public class ExecutionResult {

    private String executionId;

    /** 是否执行成功（exit code = 0） */
    private boolean success;

    /** 标准输出内容 */
    private String stdout;

    /** 标准错误输出内容 */
    private String stderr;

    /** 进程退出码 */
    private int exitCode;

    /** 实际执行耗时（毫秒） */
    private long durationMs;

    /**
     * 执行失败原因（超时、命令不存在等系统级错误）。
     * 与 stderr 区分：stderr 是命令本身的错误输出，
     * failureReason 是执行框架层面的错误。
     */
    private String failureReason;
}