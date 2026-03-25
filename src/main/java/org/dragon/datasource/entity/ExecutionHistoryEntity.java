package org.dragon.datasource.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.dragon.schedule.entity.ExecutionHistory;
import org.dragon.schedule.entity.ExecutionStatus;

/**
 * ExecutionHistoryEntity 任务执行历史实体
 * 映射数据库 execution_history 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "execution_history")
public class ExecutionHistoryEntity {

    @Id
    private Long id;

    private String executionId;

    private String cronId;

    private String cronName;

    private Long triggerTime;

    private Long actualFireTime;

    private Long completeTime;

    private Integer durationMs;

    private String status;

    private String executeNode;

    private String executeThread;

    private String resultData;

    private String errorMessage;

    private String stackTrace;

    private Integer retryCount;

    private String parentExecutionId;

    private String ext1;

    private String ext2;

    /**
     * 转换为ExecutionHistory
     */
    public ExecutionHistory toExecutionHistory() {
        return ExecutionHistory.builder()
                .id(this.id)
                .executionId(this.executionId)
                .cronId(this.cronId)
                .cronName(this.cronName)
                .triggerTime(this.triggerTime)
                .actualFireTime(this.actualFireTime)
                .completeTime(this.completeTime)
                .durationMs(this.durationMs)
                .status(this.status != null ? ExecutionStatus.valueOf(this.status) : null)
                .executeNode(this.executeNode)
                .executeThread(this.executeThread)
                .resultData(this.resultData)
                .errorMessage(this.errorMessage)
                .stackTrace(this.stackTrace)
                .retryCount(this.retryCount)
                .parentExecutionId(this.parentExecutionId)
                .ext1(this.ext1)
                .ext2(this.ext2)
                .build();
    }

    /**
     * 从ExecutionHistory创建Entity
     */
    public static ExecutionHistoryEntity fromExecutionHistory(ExecutionHistory history) {
        return ExecutionHistoryEntity.builder()
                .id(history.getId())
                .executionId(history.getExecutionId())
                .cronId(history.getCronId())
                .cronName(history.getCronName())
                .triggerTime(history.getTriggerTime())
                .actualFireTime(history.getActualFireTime())
                .completeTime(history.getCompleteTime())
                .durationMs(history.getDurationMs())
                .status(history.getStatus() != null ? history.getStatus().name() : null)
                .executeNode(history.getExecuteNode())
                .executeThread(history.getExecuteThread())
                .resultData(history.getResultData())
                .errorMessage(history.getErrorMessage())
                .stackTrace(history.getStackTrace())
                .retryCount(history.getRetryCount())
                .parentExecutionId(history.getParentExecutionId())
                .ext1(history.getExt1())
                .ext2(history.getExt2())
                .build();
    }
}
