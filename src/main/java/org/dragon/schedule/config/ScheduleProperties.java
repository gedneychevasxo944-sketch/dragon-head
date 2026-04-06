package org.dragon.schedule.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 调度模块配置属性
 */
// TODO [ConfigStore Migration]: 评估是否迁移到 ConfigStore GLOBAL scope（部分配置如 threadPool/corePoolSize 适合迁移）
@Data
@ConfigurationProperties(prefix = "dragon.schedule")
public class ScheduleProperties {

    /**
     * 是否启用调度模块
     */
    private boolean enabled = true;

    /**
     * 调度器名称
     */
    private String schedulerName = "default";

    /**
     * 节点标识（默认为机器名）
     */
    private String nodeId;

    /**
     * 线程池配置
     */
    private ThreadPoolProperties threadPool = new ThreadPoolProperties();

    /**
     * 存储配置
     */
    private StoreProperties store = new StoreProperties();

    /**
     * 分布式锁配置
     */
    private LockProperties lock = new LockProperties();

    /**
     * 执行配置
     */
    private ExecutionProperties execution = new ExecutionProperties();

    /**
     * 线程池配置
     */
    @Data
    public static class ThreadPoolProperties {
        /**
         * 核心线程数
         */
        private int corePoolSize = 10;

        /**
         * 最大线程数
         */
        private int maximumPoolSize = 50;

        /**
         * 线程存活时间（秒）
         */
        private long keepAliveTime = 60;

        /**
         * 队列容量
         */
        private int queueCapacity = 1000;
    }

    /**
     * 存储配置
     */
    @Data
    public static class StoreProperties {
        /**
         * 存储类型: memory, jdbc, redis, file
         */
        private String type = "memory";

        /**
         * 表前缀
         */
        private String tablePrefix = "cron_";

        /**
         * 文件存储路径
         */
        private String filePath = "./cron-data";
    }

    /**
     * 分布式锁配置
     */
    @Data
    public static class LockProperties {
        /**
         * 锁类型: memory, redis, zookeeper, database
         */
        private String type = "memory";

        /**
         * 锁键前缀
         */
        private String keyPrefix = "cron:lock:";

        /**
         * 默认锁过期时间（毫秒）
         */
        private long defaultLockTtl = 30000;

        /**
         * 看门狗续期间隔（毫秒）
         */
        private long watchDogInterval = 10000;
    }

    /**
     * 执行配置
     */
    @Data
    public static class ExecutionProperties {
        /**
         * 默认超时时间（毫秒）
         */
        private long defaultTimeout = 0; // 0 表示无限制

        /**
         * 默认重试次数
         */
        private int defaultRetryCount = 0;

        /**
         * 默认重试间隔（毫秒）
         */
        private long defaultRetryInterval = 0;

        /**
         * 失败重试退避倍数
         */
        private double retryBackoffMultiplier = 2.0;
    }
}
