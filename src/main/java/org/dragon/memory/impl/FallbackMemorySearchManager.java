package org.dragon.memory.impl;

import org.dragon.memory.MemorySearchManager;
import org.dragon.memory.models.*;

import java.util.Map;

public class FallbackMemorySearchManager implements MemorySearchManager {

    private final MemorySearchManager primary;
    private final MemorySearchManager fallback;
    private volatile boolean primaryFailed;
    private String failureReason;

    public FallbackMemorySearchManager(MemorySearchManager primary, MemorySearchManager fallback) {
        this.primary = primary;
        this.fallback = fallback;
        this.primaryFailed = false;
        this.failureReason = null;
    }

    @Override
    public MemorySearchResult search(String query, SearchOptions opts) {
        if (!primaryFailed) {
            try {
                return primary.search(query, opts);
            } catch (Exception e) {
                primaryFailed = true;
                failureReason = e.getMessage();
                System.err.println("Primary search failed, falling back: " + failureReason);
            }
        }
        return fallback.search(query, opts);
    }

    @Override
    public ReadFileResult readFile(ReadFileRequest request) {
        if (!primaryFailed) {
            try {
                return primary.readFile(request);
            } catch (Exception e) {
                primaryFailed = true;
                failureReason = e.getMessage();
                System.err.println("Primary readFile failed, falling back: " + failureReason);
            }
        }
        return fallback.readFile(request);
    }

    @Override
    public MemoryProviderStatus status() {
        MemoryProviderStatus status;
        if (!primaryFailed) {
            try {
                status = primary.status();
            } catch (Exception e) {
                primaryFailed = true;
                failureReason = e.getMessage();
                System.err.println("Primary status failed, falling back: " + failureReason);
                status = fallback.status();
            }
        } else {
            status = fallback.status();
        }
        // 在状态中添加降级信息
        status.setFallback(Map.of(
                "enabled", true,
                "primaryFailed", primaryFailed,
                "failureReason", failureReason,
                "primaryBackend", primary.getClass().getSimpleName(),
                "fallbackBackend", fallback.getClass().getSimpleName()
        ));
        return status;
    }

    @Override
    public MemorySyncProgressUpdate sync(SyncRequest request) {
        if (!primaryFailed) {
            try {
                return primary.sync(request);
            } catch (Exception e) {
                primaryFailed = true;
                failureReason = e.getMessage();
                System.err.println("Primary sync failed, falling back: " + failureReason);
            }
        }
        return fallback.sync(request);
    }

    @Override
    public MemoryEmbeddingProbeResult probeEmbeddingAvailability() {
        if (!primaryFailed) {
            try {
                return primary.probeEmbeddingAvailability();
            } catch (Exception e) {
                primaryFailed = true;
                failureReason = e.getMessage();
                System.err.println("Primary probeEmbeddingAvailability failed, falling back: " + failureReason);
            }
        }
        return fallback.probeEmbeddingAvailability();
    }

    @Override
    public boolean probeVectorAvailability() {
        if (!primaryFailed) {
            try {
                return primary.probeVectorAvailability();
            } catch (Exception e) {
                primaryFailed = true;
                failureReason = e.getMessage();
                System.err.println("Primary probeVectorAvailability failed, falling back: " + failureReason);
            }
        }
        return fallback.probeVectorAvailability();
    }

    @Override
    public void close() {
        try {
            primary.close();
        } catch (Exception e) {
            System.err.println("Error closing primary manager: " + e.getMessage());
        }
        try {
            fallback.close();
        } catch (Exception e) {
            System.err.println("Error closing fallback manager: " + e.getMessage());
        }
    }

    public boolean isPrimaryFailed() {
        return primaryFailed;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void resetPrimary() {
        primaryFailed = false;
        failureReason = null;
    }
}