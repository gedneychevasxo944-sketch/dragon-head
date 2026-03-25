package org.dragon.memory.config;

public class ResolvedMemoryBackendConfig {

    private String backend;
    private ResolvedMemorySearchConfig searchConfig;
    private boolean fallbackEnabled;
    private String fallbackBackend;

    public ResolvedMemoryBackendConfig() {
    }

    public ResolvedMemoryBackendConfig(String backend, ResolvedMemorySearchConfig searchConfig) {
        this.backend = backend;
        this.searchConfig = searchConfig;
    }

    // Getters and Setters
    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    public ResolvedMemorySearchConfig getSearchConfig() {
        return searchConfig;
    }

    public void setSearchConfig(ResolvedMemorySearchConfig searchConfig) {
        this.searchConfig = searchConfig;
    }

    public boolean isFallbackEnabled() {
        return fallbackEnabled;
    }

    public void setFallbackEnabled(boolean fallbackEnabled) {
        this.fallbackEnabled = fallbackEnabled;
    }

    public String getFallbackBackend() {
        return fallbackBackend;
    }

    public void setFallbackBackend(String fallbackBackend) {
        this.fallbackBackend = fallbackBackend;
    }
}