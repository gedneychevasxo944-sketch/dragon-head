package org.dragon.memory.config;

import java.util.List;
import java.util.Map;

public class ResolvedMemorySearchConfig {

    private List<String> sources;
    private List<String> extraPaths;
    private String provider;
    private String model;
    private String fallback;
    private String storePath;
    private boolean storeVectorEnabled;
    private boolean ftsEnabled; // 添加 FTS 启用标志
    private int chunkingTokens;
    private int chunkingOverlap;
    private boolean syncOnSearch;
    private boolean syncWatch;
    private int syncIntervalMinutes;
    private int queryMaxResults;
    private double queryMinScore;
    private double queryHybridVectorWeight;
    private double queryHybridTextWeight;
    private boolean queryHybridMmr;
    private boolean queryHybridTemporalDecay;
    private boolean cacheEnabled;
    private String qmdSearchMode;
    private List<String> qmdCollections;
    private List<String> qmdSessions;
    private boolean qmdUpdate;
    private boolean qmdMcporter;
    private boolean onSessionStartEnabled; // 添加会话启动时的同步标志

    public ResolvedMemorySearchConfig() {
    }

    // Getters and Setters
    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public List<String> getExtraPaths() {
        return extraPaths;
    }

    public void setExtraPaths(List<String> extraPaths) {
        this.extraPaths = extraPaths;
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

    public String getFallback() {
        return fallback;
    }

    public void setFallback(String fallback) {
        this.fallback = fallback;
    }

    public String getStorePath() {
        return storePath;
    }

    public void setStorePath(String storePath) {
        this.storePath = storePath;
    }

    public boolean isStoreVectorEnabled() {
        return storeVectorEnabled;
    }

    public void setStoreVectorEnabled(boolean storeVectorEnabled) {
        this.storeVectorEnabled = storeVectorEnabled;
    }

    public int getChunkingTokens() {
        return chunkingTokens;
    }

    public void setChunkingTokens(int chunkingTokens) {
        this.chunkingTokens = chunkingTokens;
    }

    public int getChunkingOverlap() {
        return chunkingOverlap;
    }

    public void setChunkingOverlap(int chunkingOverlap) {
        this.chunkingOverlap = chunkingOverlap;
    }

    public boolean isSyncOnSearch() {
        return syncOnSearch;
    }

    public void setSyncOnSearch(boolean syncOnSearch) {
        this.syncOnSearch = syncOnSearch;
    }

    public boolean isSyncWatch() {
        return syncWatch;
    }

    public void setSyncWatch(boolean syncWatch) {
        this.syncWatch = syncWatch;
    }

    public int getSyncIntervalMinutes() {
        return syncIntervalMinutes;
    }

    public void setSyncIntervalMinutes(int syncIntervalMinutes) {
        this.syncIntervalMinutes = syncIntervalMinutes;
    }

    public int getQueryMaxResults() {
        return queryMaxResults;
    }

    public void setQueryMaxResults(int queryMaxResults) {
        this.queryMaxResults = queryMaxResults;
    }

    public double getQueryMinScore() {
        return queryMinScore;
    }

    public void setQueryMinScore(double queryMinScore) {
        this.queryMinScore = queryMinScore;
    }

    public double getQueryHybridVectorWeight() {
        return queryHybridVectorWeight;
    }

    public void setQueryHybridVectorWeight(double queryHybridVectorWeight) {
        this.queryHybridVectorWeight = queryHybridVectorWeight;
    }

    public double getQueryHybridTextWeight() {
        return queryHybridTextWeight;
    }

    public void setQueryHybridTextWeight(double queryHybridTextWeight) {
        this.queryHybridTextWeight = queryHybridTextWeight;
    }

    public boolean isQueryHybridMmr() {
        return queryHybridMmr;
    }

    public void setQueryHybridMmr(boolean queryHybridMmr) {
        this.queryHybridMmr = queryHybridMmr;
    }

    public boolean isQueryHybridTemporalDecay() {
        return queryHybridTemporalDecay;
    }

    public void setQueryHybridTemporalDecay(boolean queryHybridTemporalDecay) {
        this.queryHybridTemporalDecay = queryHybridTemporalDecay;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public String getQmdSearchMode() {
        return qmdSearchMode;
    }

    public void setQmdSearchMode(String qmdSearchMode) {
        this.qmdSearchMode = qmdSearchMode;
    }

    public List<String> getQmdCollections() {
        return qmdCollections;
    }

    public void setQmdCollections(List<String> qmdCollections) {
        this.qmdCollections = qmdCollections;
    }

    public List<String> getQmdSessions() {
        return qmdSessions;
    }

    public void setQmdSessions(List<String> qmdSessions) {
        this.qmdSessions = qmdSessions;
    }

    public boolean isQmdUpdate() {
        return qmdUpdate;
    }

    public void setQmdUpdate(boolean qmdUpdate) {
        this.qmdUpdate = qmdUpdate;
    }

    public boolean isQmdMcporter() {
        return qmdMcporter;
    }

    public void setQmdMcporter(boolean qmdMcporter) {
        this.qmdMcporter = qmdMcporter;
    }

    // 新增属性的 getters 和 setters
    public boolean isFtsEnabled() {
        return ftsEnabled;
    }

    public void setFtsEnabled(boolean ftsEnabled) {
        this.ftsEnabled = ftsEnabled;
    }

    public boolean isOnSessionStartEnabled() {
        return onSessionStartEnabled;
    }

    public void setOnSessionStartEnabled(boolean onSessionStartEnabled) {
        this.onSessionStartEnabled = onSessionStartEnabled;
    }

    // 便捷方法
    public double getMinScore() {
        return queryMinScore;
    }

    public int getMaxResults() {
        return queryMaxResults;
    }

    public boolean isSyncOnSearchEnabled() {
        return syncOnSearch;
    }

    public void setSyncOnSearchEnabled(boolean syncOnSearchEnabled) {
        this.syncOnSearch = syncOnSearchEnabled;
    }
}