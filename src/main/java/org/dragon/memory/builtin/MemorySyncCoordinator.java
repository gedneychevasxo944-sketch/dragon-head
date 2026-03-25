package org.dragon.memory.builtin;

import org.dragon.memory.config.ResolvedMemorySearchConfig;
import org.dragon.memory.models.MemorySyncProgressUpdate;
import org.dragon.memory.models.SyncRequest;

public class MemorySyncCoordinator {

    private final ResolvedMemorySearchConfig searchConfig;
    private final MemoryFileScanner fileScanner;
    private final MemoryEmbeddingIndexer embeddingIndexer;

    public MemorySyncCoordinator(ResolvedMemorySearchConfig searchConfig) {
        this.searchConfig = searchConfig;
        this.fileScanner = new MemoryFileScanner(searchConfig);
        this.embeddingIndexer = new MemoryEmbeddingIndexer(searchConfig);
    }

    public MemorySyncProgressUpdate sync(SyncRequest request) {
        MemorySyncProgressUpdate progress = new MemorySyncProgressUpdate();
        progress.setStatus("syncing");

        // 扫描需要同步的文件
        var filesToSync = fileScanner.scanMemoryFiles();

        // 处理同步进度
        int processed = 0;
        int total = filesToSync.size();

        // 逐个文件进行索引
        for (String file : filesToSync) {
            try {
                embeddingIndexer.indexFile(file);
                processed++;
                progress.setProcessedFiles(processed);
                progress.setTotalFiles(total);
            } catch (Exception e) {
                System.err.println("Error indexing file " + file + ": " + e.getMessage());
            }
        }

        if (request.isForce()) {
            // 强制重建索引
            System.out.println("Force reindexing all files...");
        }

        progress.setStatus("complete");
        return progress;
    }

    // 其他同步相关方法...
}
