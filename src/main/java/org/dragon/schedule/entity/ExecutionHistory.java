package org.dragon.schedule.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 任务执行历史记录
 * 对应架构文档中的 ExecutionHistory 领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionHistory {

    private Long id;

    // 执行标识
    private String executionId;
    private String cronId;
    private String cronName;

    // 时间信息
    private Long triggerTime;
    private Long actualFireTime;
    private Long completeTime;
    private Integer durationMs;

    // 执行信息
    private ExecutionStatus status;
    private String executeNode;
    private String executeThread;

    // 结果信息
    private String resultData;
    private String errorMessage;
    private String stackTrace;

    // 重试信息
    private Integer retryCount;
    private String parentExecutionId;

    // 扩展字段
    private String ext1;
    private String ext2;

    /**
     * 计算执行耗时
     */
    public void calculateDuration() {
        if (actualFireTime != null && completeTime != null) {
            this.durationMs = (int) (completeTime - actualFireTime);
        }
    }

    /**
     * 标记执行开始
     */
    public void markStarted(String nodeId, String threadName) {
        this.actualFireTime = System.currentTimeMillis();
        this.executeNode = nodeId;
        this.executeThread = threadName;
        this.status = ExecutionStatus.RUNNING;
    }

    /**
     * 标记执行成功
     */
    public void markSuccess(String result) {
        this.completeTime = System.currentTimeMillis();
        this.status = ExecutionStatus.SUCCESS;
        this.resultData = result;
        calculateDuration();
    }

    /**
     * 标记执行失败
     */
    public void markFailed(String errorMsg, String stackTraceStr) {
        this.completeTime = System.currentTimeMillis();
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = errorMsg;
        this.stackTrace = stackTraceStr;
        calculateDuration();
    }

    /**
     * 标记执行超时
     */
    public void markTimeout() {
        this.completeTime = System.currentTimeMillis();
        this.status = ExecutionStatus.TIMEOUT;
        this.errorMessage = "Execution timeout";
        calculateDuration();
    }

    /**
     * 标记执行取消
     */
    public void markCancelled() {
        this.completeTime = System.currentTimeMillis();
        this.status = ExecutionStatus.CANCELLED;
        this.errorMessage = "Execution cancelled";
        calculateDuration();
    }
}
