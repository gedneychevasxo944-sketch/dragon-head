package org.dragon.schedule.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务执行上下文
 * 对应架构文档中的 JobContext 领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobContext {

    // Cron 相关信息
    private String cronId;
    private String cronName;
    private String cronExpression;

    // 执行标识
    private String executionId;

    // 时间信息
    private Long fireTime;
    private Long scheduledFireTime;
    private Long prevFireTime;
    private Long nextFireTime;

    // 执行控制
    private Integer refireCount;
    private Boolean recovering;
    private Boolean refireImmediately;

    // 任务数据
    private Map<String, Object> jobData;

    // 临时数据 (执行过程中使用)
    @Builder.Default
    private Map<String, Object> transientData = new ConcurrentHashMap<>();

    // 执行结果
    private Object result;

    /**
     * 从 CronDefinition 创建执行上下文
     */
    public static JobContext from(CronDefinition definition, String executionId, Long fireTime) {
        return JobContext.builder()
                .cronId(definition.getId())
                .cronName(definition.getName())
                .cronExpression(definition.getCronExpression())
                .executionId(executionId)
                .fireTime(fireTime)
                .scheduledFireTime(fireTime)
                .jobData(definition.getJobData() != null ? 
                        new ConcurrentHashMap<>(definition.getJobData()) : 
                        new ConcurrentHashMap<>())
                .transientData(new ConcurrentHashMap<>())
                .refireCount(0)
                .recovering(false)
                .refireImmediately(false)
                .build();
    }

    /**
     * 设置执行结果
     */
    public void setResult(Object result) {
        this.result = result;
    }

    /**
     * 获取执行结果
     */
    public Object getResult() {
        return result;
    }

    /**
     * 是否恢复执行
     */
    public boolean isRecovering() {
        return recovering != null && recovering;
    }

    /**
     * 是否立即重试
     */
    public boolean isRefireImmediately() {
        return refireImmediately != null && refireImmediately;
    }

    /**
     * 增加重试计数
     */
    public void incrementRefireCount() {
        if (refireCount == null) {
            refireCount = 0;
        }
        refireCount++;
    }

    /**
     * 设置临时数据
     */
    public void setTransient(String key, Object value) {
        if (transientData == null) {
            transientData = new ConcurrentHashMap<>();
        }
        transientData.put(key, value);
    }

    /**
     * 获取临时数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getTransient(String key) {
        if (transientData == null) {
            return null;
        }
        return (T) transientData.get(key);
    }

    /**
     * 获取任务数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getJobData(String key) {
        if (jobData == null) {
            return null;
        }
        return (T) jobData.get(key);
    }

    /**
     * 设置任务数据
     */
    public void setJobData(String key, Object value) {
        if (jobData == null) {
            jobData = new ConcurrentHashMap<>();
        }
        jobData.put(key, value);
    }
}
