package org.dragon.memory.impl;

import org.dragon.memory.MemorySearchManager;
import org.dragon.memory.builtin.*;
import org.dragon.memory.config.ResolvedMemorySearchConfig;
import org.dragon.memory.models.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MemoryIndexManager implements MemorySearchManager {

    private final ResolvedMemorySearchConfig searchConfig;
    private final MemoryQueryEngine queryEngine;
    private final MemorySyncCoordinator syncCoordinator;
    private final MemoryEmbeddingIndexer embeddingIndexer;
    private final SessionTranscriptAdapter sessionTranscriptAdapter;
    private final MemorySchemaManager schemaManager;
    private final ExecutorService executorService;

    public MemoryIndexManager(ResolvedMemorySearchConfig searchConfig) {
        this.searchConfig = searchConfig;
        this.queryEngine = new MemoryQueryEngine(searchConfig);
        this.syncCoordinator = new MemorySyncCoordinator(searchConfig);
        this.embeddingIndexer = new MemoryEmbeddingIndexer(searchConfig);
        this.sessionTranscriptAdapter = new SessionTranscriptAdapter();
        this.schemaManager = new MemorySchemaManager(searchConfig);
        this.executorService = Executors.newFixedThreadPool(2); // 用于异步预热和同步

        // 初始化数据库 schema
        schemaManager.createTables();
    }

    @Override
    public List<MemorySearchResult> search(String query, SearchOptions opts) {
        String trimmedQuery = query.trim();
        if (trimmedQuery.isEmpty()) {
            return List.of();
        }

        // 若配置开启 onSessionStart，异步触发会话预热
        if (searchConfig.isOnSessionStartEnabled()) {
            executorService.submit(() -> warmSession(opts.getSessionKey()));
        }

        // 若配置 sync.onSearch=true 且有脏标记，异步触发同步
        if (searchConfig.isSyncOnSearchEnabled() && (hasDirtyFiles() || hasDirtySessions())) {
            executorService.submit(() -> sync(new SyncRequest().withReason("search")));
        }

        // 执行搜索（不阻塞）
        return queryEngine.search(trimmedQuery, opts);
    }

    @Override
    public ReadFileResult readFile(ReadFileRequest request) {
        String path = request.getPath();
        StringBuilder content = new StringBuilder();
        int totalLines = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                totalLines++;
                if (request.getStartLine() > 0 && totalLines < request.getStartLine()) {
                    continue;
                }
                if (request.getEndLine() > 0 && totalLines > request.getEndLine()) {
                    break;
                }
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

        return new ReadFileResult(path, content.toString(), totalLines);
    }

    @Override
    public MemoryProviderStatus status() {
        MemoryProviderStatus status = new MemoryProviderStatus();
        status.setBackend("builtin");
        status.setProvider(searchConfig.getProvider());
        status.setModel(searchConfig.getModel());
        status.setRequestedProvider(searchConfig.getFallback());
        status.setDirty(hasDirtyFiles());
        status.setSessionsDirty(hasDirtySessions());
        status.setProviderUnavailableReason(getProviderUnavailableReason());
        status.setFallbackReason(getFallbackReason());
        return status;
    }

    @Override
    public MemorySyncProgressUpdate sync(SyncRequest request) {
        return syncCoordinator.sync(request);
    }

    @Override
    public MemoryEmbeddingProbeResult probeEmbeddingAvailability() {
        try {
            if (searchConfig.getProvider() == null || searchConfig.getProvider().isEmpty()) {
                return new MemoryEmbeddingProbeResult(false, "No embedding provider configured");
            }
            return new MemoryEmbeddingProbeResult(true, null);
        } catch (Exception e) {
            return new MemoryEmbeddingProbeResult(false, e.getMessage());
        }
    }

    @Override
    public boolean probeVectorAvailability() {
        return searchConfig.isStoreVectorEnabled();
    }

    @Override
    public void warmSession(String sessionKey) {
        if (sessionKey == null || sessionKey.isEmpty()) {
            return;
        }
        // 异步触发会话预热
        executorService.submit(() -> syncCoordinator.warmSession(sessionKey));
    }

    @Override
    public void close() {
        try {
            executorService.shutdown();
            // 清理资源
        } catch (Exception e) {
            System.err.println("Error closing MemoryIndexManager: " + e.getMessage());
        }
    }

    // 内部状态检查方法
    private boolean hasDirtyFiles() {
        return syncCoordinator.hasDirtyFiles();
    }

    private boolean hasDirtySessions() {
        return syncCoordinator.hasDirtySessions();
    }

    private String getProviderUnavailableReason() {
        if (searchConfig.getProvider() == null || searchConfig.getProvider().isEmpty()) {
            return "No embedding provider configured";
        }
        return null;
    }

    private String getFallbackReason() {
        return null; // TODO: 实现 fallback 原因判断
    }
}
