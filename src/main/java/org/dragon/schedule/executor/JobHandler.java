package org.dragon.schedule.executor;

import org.dragon.schedule.entity.CronDefinition;
import org.dragon.schedule.entity.JobContext;

/**
 * 任务处理器接口
 * 业务代码实现此接口来定义具体的任务逻辑
 */
public interface JobHandler {

    /**
     * 执行任务
     *
     * @param definition Cron 定义
     * @param context 执行上下文
     * @return 执行结果
     * @throws Exception 执行异常
     */
    Object execute(CronDefinition definition, JobContext context) throws Exception;

    /**
     * 获取处理器名称
     *
     * @return 处理器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 是否支持并发执行
     *
     * @return 是否支持
     */
    default boolean supportsConcurrentExecution() {
        return true;
    }

    /**
     * 执行前回调
     *
     * @param definition Cron 定义
     * @param context 执行上下文
     */
    default void beforeExecute(CronDefinition definition, JobContext context) {
        // 子类可覆盖
    }

    /**
     * 执行后回调
     *
     * @param definition Cron 定义
     * @param context 执行上下文
     * @param result 执行结果
     * @param throwable 异常（如果有）
     */
    default void afterExecute(CronDefinition definition, JobContext context, Object result, Throwable throwable) {
        // 子类可覆盖
    }
}
