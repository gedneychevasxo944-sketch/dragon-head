package org.dragon.schedule.lock;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 本地分布式锁实现（单节点模式）
 * 适用于单节点部署或开发环境
 */
@Slf4j
public class LocalDistributedLock implements DistributedLock {

    private final Map<String, LockHolder> locks = new ConcurrentHashMap<>();
    private final String nodeId;

    public LocalDistributedLock() {
        this.nodeId = "local-" + System.currentTimeMillis();
    }

    public LocalDistributedLock(String nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public boolean lock(String lockKey) {
        return lock(lockKey, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean lock(String lockKey, long timeout, TimeUnit unit) {
        if (lockKey == null) {
            throw new IllegalArgumentException("Lock key cannot be null");
        }

        synchronized (this) {
            LockHolder holder = locks.get(lockKey);
            Thread currentThread = Thread.currentThread();

            if (holder == null) {
                // 创建新锁
                holder = new LockHolder(lockKey, currentThread.getId(), nodeId);
                locks.put(lockKey, holder);
                log.debug("Acquired lock: key={}, thread={}", lockKey, currentThread.getName());
                return true;
            }

            // 检查是否是重入
            if (holder.isHeldBy(currentThread.getId(), nodeId)) {
                holder.incrementReentrant();
                log.debug("Reentrant lock: key={}, thread={}, count={}", 
                        lockKey, currentThread.getName(), holder.getReentrantCount());
                return true;
            }

            // 锁已被其他线程持有，等待
            try {
                long waitTime = unit.toMillis(timeout);
                long deadline = System.currentTimeMillis() + waitTime;

                while (locks.containsKey(lockKey)) {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0) {
                        log.debug("Lock acquisition timeout: key={}, thread={}", 
                                lockKey, currentThread.getName());
                        return false;
                    }
                    this.wait(Math.min(remaining, 100));
                }

                // 获取到锁
                holder = new LockHolder(lockKey, currentThread.getId(), nodeId);
                locks.put(lockKey, holder);
                log.debug("Acquired lock after waiting: key={}, thread={}", 
                        lockKey, currentThread.getName());
                return true;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Lock acquisition interrupted: key={}, thread={}", 
                        lockKey, currentThread.getName());
                return false;
            }
        }
    }

    @Override
    public boolean tryLock(String lockKey) {
        if (lockKey == null) {
            return false;
        }

        synchronized (this) {
            LockHolder holder = locks.get(lockKey);
            Thread currentThread = Thread.currentThread();

            if (holder == null) {
                holder = new LockHolder(lockKey, currentThread.getId(), nodeId);
                locks.put(lockKey, holder);
                log.debug("Try lock acquired: key={}, thread={}", lockKey, currentThread.getName());
                return true;
            }

            if (holder.isHeldBy(currentThread.getId(), nodeId)) {
                holder.incrementReentrant();
                return true;
            }

            return false;
        }
    }

    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        return lock(lockKey, waitTime, unit);
    }

    @Override
    public void unlock(String lockKey) {
        if (lockKey == null) {
            return;
        }

        synchronized (this) {
            LockHolder holder = locks.get(lockKey);

            if (holder == null) {
                log.warn("Unlock called for non-existent lock: key={}", lockKey);
                return;
            }

            Thread currentThread = Thread.currentThread();

            if (!holder.isHeldBy(currentThread.getId(), nodeId)) {
                log.warn("Unlock called by non-owner thread: key={}, thread={}", 
                        lockKey, currentThread.getName());
                return;
            }

            holder.decrementReentrant();

            if (holder.getReentrantCount() == 0) {
                locks.remove(lockKey);
                this.notifyAll();
                log.debug("Lock released: key={}, thread={}", lockKey, currentThread.getName());
            } else {
                log.debug("Lock reentrant count decremented: key={}, thread={}, count={}", 
                        lockKey, currentThread.getName(), holder.getReentrantCount());
            }
        }
    }

    @Override
    public boolean isLocked(String lockKey) {
        if (lockKey == null) {
            return false;
        }
        return locks.containsKey(lockKey);
    }

    @Override
    public boolean isHeldByCurrentThread(String lockKey) {
        if (lockKey == null) {
            return false;
        }
        LockHolder holder = locks.get(lockKey);
        if (holder == null) {
            return false;
        }
        return holder.isHeldBy(Thread.currentThread().getId(), nodeId);
    }

    @Override
    public boolean renewLock(String lockKey, long additionalTime, TimeUnit unit) {
        // 本地锁不需要续期，因为只要线程存活锁就不会过期
        return isHeldByCurrentThread(lockKey);
    }

    @Override
    public void forceUnlock(String lockKey) {
        if (lockKey == null) {
            return;
        }
        synchronized (this) {
            locks.remove(lockKey);
            this.notifyAll();
            log.info("Force unlocked: key={}", lockKey);
        }
    }

    /**
     * 锁持有者
     */
    private static class LockHolder {
        private final String lockKey;
        private final long threadId;
        private final String nodeId;
        private final AtomicInteger reentrantCount;

        LockHolder(String lockKey, long threadId, String nodeId) {
            this.lockKey = lockKey;
            this.threadId = threadId;
            this.nodeId = nodeId;
            this.reentrantCount = new AtomicInteger(1);
        }

        boolean isHeldBy(long threadId, String nodeId) {
            return this.threadId == threadId && this.nodeId.equals(nodeId);
        }

        void incrementReentrant() {
            reentrantCount.incrementAndGet();
        }

        void decrementReentrant() {
            reentrantCount.decrementAndGet();
        }

        int getReentrantCount() {
            return reentrantCount.get();
        }
    }
}
