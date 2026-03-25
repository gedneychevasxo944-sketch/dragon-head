package org.dragon.memory.impl;

import org.dragon.memory.MemorySearchManager;
import org.dragon.memory.builtin.*;
import org.dragon.memory.config.ResolvedMemorySearchConfig;
import org.dragon.memory.models.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class MemoryIndexManager implements MemorySearchManager {

    private final ResolvedMemorySearchConfig searchConfig;
    private final MemoryQueryEngine queryEngine;
    private final MemorySyncCoordinator syncCoordinator;
    private final MemoryEmbeddingIndexer embeddingIndexer;
    private final SessionTranscriptAdapter sessionTranscriptAdapter;

    public MemoryIndexManager(ResolvedMemorySearchConfig searchConfig) {
        this.searchConfig = searchConfig;
        this.queryEngine = new MemoryQueryEngine(searchConfig);
        this.syncCoordinator = new MemorySyncCoordinator(searchConfig);
        this.embeddingIndexer = new MemoryEmbeddingIndexer(searchConfig);
        this.sessionTranscriptAdapter = new SessionTranscriptAdapter();
    }

    @Override
    public MemorySearchResult search(String query, SearchOptions opts) {
        return queryEngine.search(query, opts);
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
        // TODO: 补充其他状态信息
        return status;
    }

    @Override
    public MemorySyncProgressUpdate sync(SyncRequest request) {
        return syncCoordinator.sync(request);
    }

    @Override
    public MemoryEmbeddingProbeResult probeEmbeddingAvailability() {
        try {
            // 简单实现：检查 embedding provider 是否可用
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
    public void close() {
        // TODO: 关闭资源
    }
}
