package org.dragon.memory.models;

import java.util.Map;

public class MemoryProviderStatus {

    private String backend;
    private String provider;
    private String model;
    private String requestedProvider;

    private int files;
    private int chunks;
    private boolean dirty;
    private boolean sessionsDirty;
    private String workspaceDir;
    private String dbPath;

    private String providerUnavailableReason;
    private String fallbackReason;

    private Map<String, Object> cache;
    private Map<String, Object> fts;
    private Map<String, Object> vector;
    private Map<String, Object> batch;

    private Map<String, Object> fallback;
    private Map<String, Object> custom;

    // Getters and Setters
    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getRequestedProvider() {
        return requestedProvider;
    }

    public void setRequestedProvider(String requestedProvider) {
        this.requestedProvider = requestedProvider;
    }

    public int getFiles() {
        return files;
    }

    public void setFiles(int files) {
        this.files = files;
    }

    public int getChunks() {
        return chunks;
    }

    public void setChunks(int chunks) {
        this.chunks = chunks;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public String getWorkspaceDir() {
        return workspaceDir;
    }

    public void setWorkspaceDir(String workspaceDir) {
        this.workspaceDir = workspaceDir;
    }

    public String getDbPath() {
        return dbPath;
    }

    public void setDbPath(String dbPath) {
        this.dbPath = dbPath;
    }

    public Map<String, Object> getCache() {
        return cache;
    }

    public void setCache(Map<String, Object> cache) {
        this.cache = cache;
    }

    public Map<String, Object> getFts() {
        return fts;
    }

    public void setFts(Map<String, Object> fts) {
        this.fts = fts;
    }

    public Map<String, Object> getVector() {
        return vector;
    }

    public void setVector(Map<String, Object> vector) {
        this.vector = vector;
    }

    public Map<String, Object> getBatch() {
        return batch;
    }

    public void setBatch(Map<String, Object> batch) {
        this.batch = batch;
    }

    public Map<String, Object> getFallback() {
        return fallback;
    }

    public void setFallback(Map<String, Object> fallback) {
        this.fallback = fallback;
    }

    public Map<String, Object> getCustom() {
        return custom;
    }

    public void setCustom(Map<String, Object> custom) {
        this.custom = custom;
    }

    // 新增属性的 getters 和 setters
    public boolean isSessionsDirty() {
        return sessionsDirty;
    }

    public void setSessionsDirty(boolean sessionsDirty) {
        this.sessionsDirty = sessionsDirty;
    }

    public String getProviderUnavailableReason() {
        return providerUnavailableReason;
    }

    public void setProviderUnavailableReason(String providerUnavailableReason) {
        this.providerUnavailableReason = providerUnavailableReason;
    }

    public String getFallbackReason() {
        return fallbackReason;
    }

    public void setFallbackReason(String fallbackReason) {
        this.fallbackReason = fallbackReason;
    }
}