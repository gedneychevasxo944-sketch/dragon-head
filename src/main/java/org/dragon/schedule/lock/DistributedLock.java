package org.dragon.schedule.lock;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁抽象接口
 * 对应架构文档中的 DistributedLock
 */
public interface DistributedLock {

    /**
     * 获取锁（阻塞直到获取成功）
     *
     * @param lockKey 锁键
     * @return 是否成功获取
     */
    boolean lock(String lockKey);

    /**
     * 获取锁（带超时时间）
     *
     * @param lockKey 锁键
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 是否成功获取
     */
    boolean lock(String lockKey, long timeout, TimeUnit unit);

    /**
     * 尝试获取锁（非阻塞）
     *
     * @param lockKey 锁键
     * @return 是否成功获取
     */
    boolean tryLock(String lockKey);

    /**
     * 尝试获取锁（带等待时间）
     *
     * @param lockKey 锁键
     * @param waitTime 等待时间
     * @param leaseTime 租约时间
     * @param unit 时间单位
     * @return 是否成功获取
     */
    boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 释放锁
     *
     * @param lockKey 锁键
     */
    void unlock(String lockKey);

    /**
     * 检查是否已锁定
     *
     * @param lockKey 锁键
     * @return 是否已锁定
     */
    boolean isLocked(String lockKey);

    /**
     * 检查当前线程是否持有锁
     *
     * @param lockKey 锁键
     * @return 是否持有锁
     */
    boolean isHeldByCurrentThread(String lockKey);

    /**
     * 续期锁
     *
     * @param lockKey 锁键
     * @param additionalTime 额外时间
     * @param unit 时间单位
     * @return 是否续期成功
     */
    boolean renewLock(String lockKey, long additionalTime, TimeUnit unit);

    /**
     * 强制解锁（管理员使用）
     *
     * @param lockKey 锁键
     */
    void forceUnlock(String lockKey);
}
