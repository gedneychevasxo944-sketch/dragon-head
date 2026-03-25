package org.dragon.memory.builtin;

import org.dragon.memory.config.ResolvedMemorySearchConfig;
import org.dragon.memory.models.MemorySyncProgressUpdate;
import org.dragon.memory.models.SyncRequest;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MemorySyncCoordinator {

    private final ResolvedMemorySearchConfig searchConfig;
    private final MemoryFileScanner fileScanner;
    private final MemoryEmbeddingIndexer embeddingIndexer;
    private final MemoryIndexRepository indexRepository;
    private final AtomicBoolean syncing;
    private final Semaphore singleFlightSemaphore;
    private final ConcurrentLinkedDeque<SyncRequest> syncQueue;

    public MemorySyncCoordinator(ResolvedMemorySearchConfig searchConfig) {
        this.searchConfig = searchConfig;
        this.fileScanner = new MemoryFileScanner(searchConfig);
        this.embeddingIndexer = new MemoryEmbeddingIndexer(searchConfig);
        this.indexRepository = new MemoryIndexRepository(searchConfig);
        this.syncing = new AtomicBoolean(false);
        this.singleFlightSemaphore = new Semaphore(1);
        this.syncQueue = new ConcurrentLinkedDeque<>();
    }

    /**
     * 同步索引（single-flight 保证）
     */
    public MemorySyncProgressUpdate sync(SyncRequest request) {
        MemorySyncProgressUpdate progress = new MemorySyncProgressUpdate();
        progress.setStatus("syncing");

        try {
            // 尝试获取信号量，最多等待 1 秒
            if (!singleFlightSemaphore.tryAcquire(1, TimeUnit.SECONDS)) {
                // 信号量获取失败，说明已有同步任务在运行
                if (request.isTargetedSessionSync()) {
                    syncQueue.add(request);
                    progress.setStatus("queued");
                    progress.setQueuedFiles(syncQueue.size());
                } else {
                    progress.setStatus("already_syncing");
                }
                return progress;
            }

            try {
                syncing.set(true);
                return doSync(request);
            } finally {
                syncing.set(false);
                singleFlightSemaphore.release();

                // 处理队列中的任务
                if (!syncQueue.isEmpty()) {
                    SyncRequest queuedRequest = syncQueue.poll();
                    CompletableFuture.runAsync(() -> sync(queuedRequest));
                }
            }
        } catch (InterruptedException e) {
            progress.setStatus("interrupted");
            progress.setError(e.getMessage());
            Thread.currentThread().interrupt();
        }

        return progress;
    }

    /**
     * 真正执行同步的逻辑
     */
    private MemorySyncProgressUpdate doSync(SyncRequest request) {
        MemorySyncProgressUpdate progress = new MemorySyncProgressUpdate();
        progress.setStatus("syncing");

        try {
            // 扫描需要同步的文件
            List<String> filesToSync = fileScanner.scanMemoryFiles();

            if (request.isForce()) {
                // 强制重建所有索引
                return runSafeReindex(progress, filesToSync);
            }

            return syncIncrementally(progress, filesToSync);
        } catch (Exception e) {
            progress.setStatus("failed");
            progress.setError(e.getMessage());
            System.err.println("Sync failed: " + e.getMessage());
        }

        return progress;
    }

    /**
     * 增量同步
     */
    private MemorySyncProgressUpdate syncIncrementally(MemorySyncProgressUpdate progress, List<String> filesToSync) {
        int processed = 0;
        int total = filesToSync.size();

        // 逐个文件进行索引
        for (String file : filesToSync) {
            try {
                // 检查文件是否需要更新
                if (needsIndexing(file)) {
                    embeddingIndexer.indexFile(file);
                }
                processed++;
                progress.setProcessedFiles(processed);
                progress.setTotalFiles(total);
            } catch (Exception e) {
                System.err.println("Error indexing file " + file + ": " + e.getMessage());
            }
        }

        progress.setStatus("complete");
        return progress;
    }

    /**
     * 安全全量重建（temp DB + swap）
     */
    private MemorySyncProgressUpdate runSafeReindex(MemorySyncProgressUpdate progress, List<String> filesToSync) {
        // TODO: 实现安全全量重建逻辑
        System.out.println("Running safe reindex...");
        return syncIncrementally(progress, filesToSync);
    }

    /**
     * 会话预热
     */
    public void warmSession(String sessionKey) {
        if (sessionKey == null || sessionKey.isEmpty()) {
            return;
        }
        // 实现会话预热逻辑
        System.out.println("Warming session: " + sessionKey);
    }

    /**
     * 检查文件是否需要重新索引
     */
    private boolean needsIndexing(String filePath) {
        // 实现文件变更检测逻辑
        // 检查文件的修改时间、内容哈希是否与索引中的记录匹配
        return true; // 临时返回 true，每次都重新索引
    }

    /**
     * 检查是否有脏文件
     */
    public boolean hasDirtyFiles() {
        return false; // TODO: 实现脏文件检测逻辑
    }

    /**
     * 检查是否有脏会话
     */
    public boolean hasDirtySessions() {
        return false; // TODO: 实现脏会话检测逻辑
    }
}
