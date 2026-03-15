package org.dragon.schedule.executor;

import org.dragon.schedule.entity.CronDefinition;
import org.dragon.schedule.entity.ExecutionHistory;
import org.dragon.schedule.entity.JobContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 任务执行器接口
 * 负责任务的执行管理和线程池管理
 */
public interface JobExecutor {

    /**
     * 同步执行任务
     *
     * @param definition Cron 定义
     * @param context 执行上下文
     * @return 执行历史记录
     */
    ExecutionHistory execute(CronDefinition definition, JobContext context);

    /**
     * 异步执行任务
     *
     * @param definition Cron 定义
     * @param context 执行上下文
     * @return CompletableFuture<ExecutionHistory>
     */
    CompletableFuture<ExecutionHistory> executeAsync(CronDefinition definition, JobContext context);

    /**
     * 使用指定的处理器执行任务
     *
     * @param definition Cron 定义
     * @param context 执行上下文
     * @param handler 任务处理器
     * @return 执行历史记录
     */
    ExecutionHistory executeWithHandler(CronDefinition definition, JobContext context, JobHandler handler);

    /**
     * 取消正在执行的任务
     *
     * @param executionId 执行 ID
     * @return 是否成功取消
     */
    boolean cancel(String executionId);

    /**
     * 检查任务是否正在执行
     *
     * @param executionId 执行 ID
     * @return boolean
     */
    boolean isExecuting(String executionId);

    /**
     * 获取正在执行的任务列表
     *
     * @return List<ExecutionHistory>
     */
    List<ExecutionHistory> getExecutingJobs();

    /**
     * 获取线程池状态
     *
     * @return ThreadPoolStatus
     */
    ThreadPoolStatus getThreadPoolStatus();

    /**
     * 启动执行器
     */
    void start();

    /**
     * 停止执行器
     */
    void stop();

    /**
     * 线程池状态
     */
    class ThreadPoolStatus {
        private int corePoolSize;
        private int maximumPoolSize;
        private int activeCount;
        private long completedTaskCount;
        private long taskCount;
        private int largestPoolSize;
        private int poolSize;
        private int queueSize;
        private int remainingQueueCapacity;

        // Getters and Setters
        public int getCorePoolSize() { return corePoolSize; }
        public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }

        public int getMaximumPoolSize() { return maximumPoolSize; }
        public void setMaximumPoolSize(int maximumPoolSize) { this.maximumPoolSize = maximumPoolSize; }

        public int getActiveCount() { return activeCount; }
        public void setActiveCount(int activeCount) { this.activeCount = activeCount; }

        public long getCompletedTaskCount() { return completedTaskCount; }
        public void setCompletedTaskCount(long completedTaskCount) { this.completedTaskCount = completedTaskCount; }

        public long getTaskCount() { return taskCount; }
        public void setTaskCount(long taskCount) { this.taskCount = taskCount; }

        public int getLargestPoolSize() { return largestPoolSize; }
        public void setLargestPoolSize(int largestPoolSize) { this.largestPoolSize = largestPoolSize; }

        public int getPoolSize() { return poolSize; }
        public void setPoolSize(int poolSize) { this.poolSize = poolSize; }

        public int getQueueSize() { return queueSize; }
        public void setQueueSize(int queueSize) { this.queueSize = queueSize; }

        public int getRemainingQueueCapacity() { return remainingQueueCapacity; }
        public void setRemainingQueueCapacity(int remainingQueueCapacity) { this.remainingQueueCapacity = remainingQueueCapacity; }
    }
}
